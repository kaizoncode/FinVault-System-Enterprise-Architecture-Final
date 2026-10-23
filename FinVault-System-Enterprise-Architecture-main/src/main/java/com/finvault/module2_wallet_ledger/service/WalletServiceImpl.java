package com.finvault.module2_wallet_ledger.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module1_security_rbac.service.UserService;
import com.finvault.module2_wallet_ledger.dto.WalletMemberDto;
import com.finvault.module2_wallet_ledger.dto.WalletRequest;
import com.finvault.module2_wallet_ledger.dto.WalletResponseDto;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.entity.WalletMember;
import com.finvault.module2_wallet_ledger.entity.WalletMemberRole;
import com.finvault.module2_wallet_ledger.entity.WalletType;
import com.finvault.module2_wallet_ledger.factory.WalletFactory;
import com.finvault.module2_wallet_ledger.repository.WalletMemberRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletServiceImpl: Triển khai các nghiệp vụ quản lý Đa Ví và Quỹ tài chính gia đình
 *
 * Tính năng nổi bật:
 * - Sử dụng WalletFactory khởi tạo các cấu hình ví chuyên biệt
 * - Quản lý thành viên gia đình (WalletMember) với phân quyền ADMIN/MEMBER/VIEWER
 * - Khóa bi quan (Pessimistic Lock) khi truy xuất ví qua getWalletEntityForUpdate
 * ==============================================================================
 */
@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final WalletMemberRepository walletMemberRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final WalletFactory walletFactory;

    public WalletServiceImpl(
            WalletRepository walletRepository,
            WalletMemberRepository walletMemberRepository,
            UserRepository userRepository,
            UserService userService,
            WalletFactory walletFactory) {
        this.walletRepository = walletRepository;
        this.walletMemberRepository = walletMemberRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.walletFactory = walletFactory;
    }

    @Override
    @Transactional
    public WalletResponseDto createWallet(String username, WalletRequest request) {
        User user = userService.getUserEntityByUsername(username);

        // Khởi tạo ví thông qua Factory Pattern
        Wallet wallet = walletFactory.createWallet(user, request);
        Wallet saved = walletRepository.save(wallet);

        log.info("Người dùng [{}] đã tạo ví mới thành công: ID [{}], Tên [{}], Loại [{}]",
                username, saved.getId(), saved.getWalletName(), saved.getWalletType());
        return WalletResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getWalletById(Long walletId, String username) {
        User user = userService.getUserEntityByUsername(username);
        Wallet wallet = getWalletEntityById(walletId);

        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isOwner = wallet.getUser().getId().equals(user.getId());
        boolean isMember = wallet.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()) && Boolean.TRUE.equals(m.getIsActive()));

        if (!isSuperAdmin && !isOwner && !isMember) {
            throw new BusinessException("FORBIDDEN", "Bạn không có quyền truy cập thông tin ví này", HttpStatus.FORBIDDEN);
        }

        return WalletResponseDto.fromEntity(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getWalletEntityById(Long walletId) {
        return walletRepository.findByIdAndIsDeletedFalse(walletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví có ID: " + walletId, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public Wallet getWalletEntityForUpdate(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví có ID: " + walletId, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponseDto> getUserWallets(String username) {
        User user = userService.getUserEntityByUsername(username);
        return walletRepository.findAllAccessibleWalletsByUser(user.getId()).stream()
                .map(WalletResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponseDto> getFamilyWallets(String username) {
        User user = userService.getUserEntityByUsername(username);
        return walletRepository.findFamilyWalletsAccessibleByUser(user.getId()).stream()
                .map(WalletResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WalletResponseDto updateWallet(Long walletId, String username, WalletRequest request) {
        User user = userService.getUserEntityByUsername(username);
        Wallet wallet = getWalletEntityById(walletId);

        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isOwner = wallet.getUser().getId().equals(user.getId());

        if (!isSuperAdmin && !isOwner) {
            throw new BusinessException("FORBIDDEN", "Chỉ chủ sở hữu hoặc Super Admin mới có quyền cập nhật ví", HttpStatus.FORBIDDEN);
        }

        wallet.setWalletName(request.getWalletName().trim());
        if (request.getBankAccountNumber() != null) wallet.setBankAccountNumber(request.getBankAccountNumber().trim());
        if (request.getBankName() != null) wallet.setBankName(request.getBankName().trim());
        if (request.getMonthlySpendingLimit() != null) wallet.setMonthlySpendingLimit(request.getMonthlySpendingLimit());

        Wallet updated = walletRepository.save(wallet);
        log.info("Người dùng [{}] đã cập nhật thông tin ví [{}]: ID [{}]", username, updated.getWalletName(), updated.getId());
        return WalletResponseDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteWallet(Long walletId, String username) {
        User user = userService.getUserEntityByUsername(username);
        Wallet wallet = getWalletEntityById(walletId);

        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isOwner = wallet.getUser().getId().equals(user.getId());

        if (!isSuperAdmin && !isOwner) {
            throw new BusinessException("FORBIDDEN", "Chỉ chủ sở hữu hoặc Super Admin mới có quyền xóa ví", HttpStatus.FORBIDDEN);
        }

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("WALLET_NOT_EMPTY", "Không thể xóa ví khi số dư hoặc dư nợ chưa được tất toán về 0. Vui lòng tất toán số dư trước khi xóa.");
        }

        wallet.setIsDeleted(true);
        wallet.setIsActive(false);
        walletRepository.save(wallet);
        log.info("Người dùng [{}] đã xóa mềm ví [{}]: ID [{}]", username, wallet.getWalletName(), wallet.getId());
    }

    @Override
    @Transactional
    public WalletResponseDto addFamilyMember(Long walletId, String ownerUsername, WalletMemberDto dto) {
        User owner = userService.getUserEntityByUsername(ownerUsername);
        Wallet wallet = getWalletEntityById(walletId);

        if (!Boolean.TRUE.equals(wallet.getIsFamilyWallet())) {
            throw new BusinessException("NOT_FAMILY_WALLET", "Ví này không phải là Quỹ chung gia đình");
        }

        boolean isSuperAdmin = owner.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isOwner = wallet.getUser().getId().equals(owner.getId());
        if (!isSuperAdmin && !isOwner) {
            throw new BusinessException("FORBIDDEN", "Chỉ chủ sở hữu quỹ gia đình mới có quyền thêm thành viên", HttpStatus.FORBIDDEN);
        }

        User memberUser;
        if (dto.getUserId() != null) {
            memberUser = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng có ID: " + dto.getUserId(), HttpStatus.NOT_FOUND));
        } else if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String identifier = dto.getUsername().trim();
            memberUser = userRepository.findByUsernameAndIsDeletedFalse(identifier)
                    .or(() -> userRepository.findByEmailAndIsDeletedFalse(identifier))
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng với tên đăng nhập/email: " + identifier, HttpStatus.NOT_FOUND));
        } else {
            throw new BusinessException("INVALID_MEMBER_REQUEST", "Vui lòng cung cấp ID hoặc Tên đăng nhập/Email của thành viên cần thêm", HttpStatus.BAD_REQUEST);
        }

        Optional<WalletMember> existing = walletMemberRepository.findByWalletIdAndUserId(walletId, memberUser.getId());
        if (existing.isPresent()) {
            WalletMember m = existing.get();
            m.setIsActive(true);
            m.setMemberRole(dto.getMemberRole() != null ? dto.getMemberRole() : WalletMemberRole.MEMBER);
            m.setSpendingLimit(dto.getSpendingLimit() != null ? dto.getSpendingLimit() : BigDecimal.ZERO);
            walletMemberRepository.save(m);
        } else {
            WalletMember member = new WalletMember(wallet, memberUser,
                    dto.getMemberRole() != null ? dto.getMemberRole() : WalletMemberRole.MEMBER,
                    dto.getSpendingLimit() != null ? dto.getSpendingLimit() : BigDecimal.ZERO);
            wallet.addMember(member);
            walletRepository.save(wallet);
        }

        log.info("Chủ ví [{}] đã thêm thành viên [{}] vào Quỹ gia đình [{}]: Quyền [{}], Hạn mức [{}]",
                ownerUsername, memberUser.getUsername(), wallet.getWalletName(), dto.getMemberRole(), dto.getSpendingLimit());
        return WalletResponseDto.fromEntity(wallet);
    }

    @Override
    @Transactional
    public WalletResponseDto removeFamilyMember(Long walletId, String ownerUsername, Long memberUserId) {
        User owner = userService.getUserEntityByUsername(ownerUsername);
        Wallet wallet = getWalletEntityById(walletId);

        boolean isSuperAdmin = owner.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isOwner = wallet.getUser().getId().equals(owner.getId());
        if (!isSuperAdmin && !isOwner) {
            throw new BusinessException("FORBIDDEN", "Chỉ chủ sở hữu quỹ mới có quyền xóa thành viên", HttpStatus.FORBIDDEN);
        }

        if (wallet.getUser().getId().equals(memberUserId)) {
            throw new BusinessException("CANNOT_REMOVE_OWNER", "Không thể xóa chủ sở hữu khỏi quỹ gia đình");
        }

        wallet.getMembers().removeIf(m -> m.getUser().getId().equals(memberUserId));
        walletRepository.save(wallet);
        log.info("Đã xóa thành viên ID [{}] khỏi Quỹ gia đình ID [{}]", memberUserId, walletId);
        return WalletResponseDto.fromEntity(wallet);
    }

    @Override
    @Transactional
    public WalletResponseDto updateMemberSpendingLimit(Long walletId, String ownerUsername, Long memberUserId, BigDecimal newLimit) {
        User owner = userService.getUserEntityByUsername(ownerUsername);
        Wallet wallet = getWalletEntityById(walletId);

        boolean isSuperAdmin = owner.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isOwner = wallet.getUser().getId().equals(owner.getId());
        if (!isSuperAdmin && !isOwner) {
            throw new BusinessException("FORBIDDEN", "Chỉ chủ sở hữu quỹ mới có quyền điều chỉnh hạn mức chi", HttpStatus.FORBIDDEN);
        }

        if (newLimit != null && newLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_LIMIT", "Hạn mức chi tiêu không được là số âm");
        }

        WalletMember member = walletMemberRepository.findByWalletIdAndUserId(walletId, memberUserId)
                .orElseThrow(() -> new BusinessException("MEMBER_NOT_FOUND", "Không tìm thấy thành viên trong quỹ gia đình", HttpStatus.NOT_FOUND));

        member.setSpendingLimit(newLimit != null ? newLimit : BigDecimal.ZERO);
        walletMemberRepository.save(member);
        log.info("Đã cập nhật hạn mức chi cho thành viên [{}] trong Quỹ [{}]: {}", member.getUser().getUsername(), wallet.getWalletName(), newLimit);
        return WalletResponseDto.fromEntity(wallet);
    }
}
