/**
 * ==============================================================================
 * FinVault Enterprise - Main Live Dashboard Client Logic (dashboard.js)
 * Chart.js Visualizations & AJAX In-Place Multi-Role RBAC Operations
 * ==============================================================================
 */

// ==============================================================================
// CSRF TOKEN HELPER CHO MUTATING AJAX REQUESTS (POST, PUT, DELETE, PATCH)
// ==============================================================================
window.getCsrfHeaders = function () {
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    const headers = {};
    if (metaToken && metaHeader && metaToken.content && metaHeader.content) {
        headers[metaHeader.content] = metaToken.content;
    } else {
        const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        if (match) {
            headers['X-XSRF-TOKEN'] = decodeURIComponent(match[1]);
        }
    }
    return headers;
};

// ==============================================================================
// 0. TOP-LEVEL GLOBAL ACTIONS & UNIVERSAL MODAL HANDLERS
// ==============================================================================

/**
 * Mở Modal an toàn - Hoạt động linh hoạt với Bootstrap 5 hoặc CSS Fallback độc lập
 */
window.openModal = function (modalId) {
    if (!modalId) return;
    const id = modalId.replace(/^#/, '');
    const modalEl = document.getElementById(id);
    if (!modalEl) {
        console.warn('[FinVault Modal] Không tìm thấy modal ID:', id);
        return;
    }

    if (id === 'quickTransactionModal' && typeof window.filterTransactionCategories === 'function') {
        window.filterTransactionCategories();
    }

    // Dọn dẹp triệt để backdrop mồ côi nếu hiện tại không có modal nào đang mở
    const currentlyOpen = document.querySelectorAll('.modal.show');
    if (currentlyOpen.length === 0) {
        document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('overflow');
        document.body.style.removeProperty('padding-right');
    }

    if (window.bootstrap && window.bootstrap.Modal) {
        try {
            const inst = bootstrap.Modal.getOrCreateInstance(modalEl);
            inst.show();
            return;
        } catch (e) {
            console.warn('[FinVault Modal] Bootstrap error, fallback to native CSS:', e);
        }
    }

    modalEl.classList.add('show');
    modalEl.style.display = 'block';
    modalEl.removeAttribute('aria-hidden');
    modalEl.setAttribute('aria-modal', 'true');
    document.body.classList.add('modal-open');

    let backdrop = document.getElementById('finvault-backdrop-' + id);
    if (!backdrop) {
        backdrop = document.createElement('div');
        backdrop.id = 'finvault-backdrop-' + id;
        backdrop.className = 'modal-backdrop fade show';
        backdrop.onclick = function () { window.closeModal(id); };
        document.body.appendChild(backdrop);
    }
};

/**
 * Đóng Modal an toàn - Đồng bộ giữa Bootstrap 5 và Native CSS Fallback, triệt tiêu Backdrop Lock
 */
window.closeModal = function (modalId) {
    if (!modalId) return;
    const id = modalId.replace(/^#/, '');
    const modalEl = document.getElementById(id);
    if (!modalEl) return;

    if (window.bootstrap && window.bootstrap.Modal) {
        try {
            const inst = bootstrap.Modal.getInstance(modalEl);
            if (inst) {
                inst.hide();
            }
        } catch (e) {
            console.warn('[FinVault Modal] Bootstrap hide error:', e);
        }
    }

    // Luôn dọn dẹp các thuộc tính CSS / class để dự phòng Bootstrap trễ hoặc lỗi
    modalEl.classList.remove('show');
    modalEl.style.display = 'none';
    modalEl.setAttribute('aria-hidden', 'true');
    modalEl.removeAttribute('aria-modal');

    const backdrop = document.getElementById('finvault-backdrop-' + id);
    if (backdrop) backdrop.remove();

    // Dọn dẹp triệt để backdrop mồ côi và mở khóa thanh cuộn body
    setTimeout(() => {
        const remainingOpen = document.querySelectorAll('.modal.show');
        if (remainingOpen.length === 0) {
            document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
            document.body.classList.remove('modal-open');
            document.body.style.removeProperty('overflow');
            document.body.style.removeProperty('padding-right');
        }
    }, 150);
};

// Global listener: Bất cứ khi nào Bootstrap đóng xong modal (hidden.bs.modal), dọn sạch backdrop lock
document.addEventListener('hidden.bs.modal', function () {
    const remainingOpen = document.querySelectorAll('.modal.show');
    if (remainingOpen.length === 0) {
        document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('overflow');
        document.body.style.removeProperty('padding-right');
    }
});

/**
 * Chuyển đổi linh hoạt giữa Chế độ Tổng Quan (Overview) và các Chế độ Chuyên Sâu theo từng Module
 * Tương thích linh hoạt mọi User Role (user1, admin, auditor, family_head), cập nhật URL hash & đồng bộ giao diện
 */
window.switchDashboardView = function (targetView) {
    // Tự động đóng mọi modal đang mở dở khi người dùng điều hướng chuyển kênh
    document.querySelectorAll('.modal.show').forEach(m => {
        if (window.closeModal) window.closeModal(m.id);
    });
    if (!targetView || targetView === '' || targetView === 'overview' || targetView === 'all') {
        targetView = 'overview';
    }
    // Chuẩn hóa alias
    let view = targetView.replace(/^#/, '').replace(/-section$/, '');
    if (view === 'loans') view = 'loan-calculator';
    if (view === 'dashboard' || view === 'all') view = 'overview';

    // Cập nhật URL Hash an toàn không gây jump trang giật cục
    const targetHash = '#' + (view === 'overview' ? 'overview' : view);
    if (window.location.hash !== targetHash) {
        if (window.history && window.history.pushState) {
            window.history.pushState(null, null, targetHash);
        } else {
            window.location.hash = targetHash;
        }
    }

    // 1. Cập nhật trạng thái Active trên Top Navbar
    const navLinks = document.querySelectorAll('#mainDashboardNav .nav-link');
    navLinks.forEach(link => {
        const linkView = link.getAttribute('data-view');
        if (linkView === view || (view === 'overview' && linkView === 'overview')) {
            link.classList.add('active', 'text-primary', 'fw-bold', 'border-bottom', 'border-primary', 'border-2');
            link.classList.remove('text-secondary');
        } else {
            link.classList.remove('active', 'text-primary', 'fw-bold', 'border-bottom', 'border-primary', 'border-2');
            link.classList.add('text-secondary');
        }
    });

    // 2. Tham chiếu các container chính
    const viewHeader = document.getElementById('dashboard-view-header');
    const viewHeaderIconBox = document.getElementById('view-header-icon-box');
    const viewHeaderIcon = document.getElementById('view-header-icon');
    const viewHeaderBadge = document.getElementById('view-header-badge');
    const viewHeaderTitle = document.getElementById('view-header-title');
    const viewHeaderDesc = document.getElementById('view-header-desc');
    const viewHeaderActions = document.getElementById('view-header-actions');

    const kpiCardsRow = document.getElementById('kpi-cards-row');
    const mainDashboardRow = document.getElementById('main-dashboard-row');
    const colLeft = document.getElementById('dashboard-col-left');
    const colRight = document.getElementById('dashboard-col-right');

    const walletsModule = document.getElementById('wallets-module-container');
    const biAnalyticsModule = document.getElementById('bi-analytics-section');
    const transactionsModule = document.getElementById('transactions-section');
    const healthScoreModule = document.getElementById('cfp-health-score-widget');
    const budgetsModule = document.getElementById('budgets-section');
    const savingGoalsModule = document.getElementById('saving-goals-section');
    const categoryChartModule = document.getElementById('category-breakdown-widget');
    const loanCalculatorModule = document.getElementById('loan-calculator-section');

    const familyApprovalModule = document.getElementById('family-approval-section');
    const auditorMonitorModule = document.getElementById('auditor-monitor-section');
    const adminCmsModule = document.getElementById('admin-live-cms-section');
    const adminRecoveryModule = document.getElementById('admin-recovery-section');

    // Nút mặc định quay về tổng quan
    const overviewButtonHtml = `
        <button type="button" class="btn btn-outline-secondary btn-sm fw-semibold shadow-sm" onclick="switchDashboardView('overview')">
            <i class="bi bi-grid-fill me-1"></i> Xem Toàn Cảnh (Tổng Quan)
        </button>
    `;

    // 3. Xử lý hiển thị theo từng View
    if (view === 'overview') {
        // --- CHẾ ĐỘ TỔNG QUAN (OVERVIEW) ---
        if (viewHeader) viewHeader.style.display = 'none';
        if (kpiCardsRow) kpiCardsRow.style.display = '';
        if (mainDashboardRow) mainDashboardRow.style.display = '';

        if (colLeft) {
            colLeft.className = 'col-12 col-xl-8';
            colLeft.style.display = '';
        }
        if (colRight) {
            colRight.className = 'col-12 col-xl-4';
            colRight.style.display = '';
        }

        // Hiện tất cả modules trong cột
        if (walletsModule) walletsModule.style.display = '';
        if (biAnalyticsModule) biAnalyticsModule.style.display = '';
        if (transactionsModule) transactionsModule.style.display = '';
        if (healthScoreModule) healthScoreModule.style.display = '';
        if (budgetsModule) budgetsModule.style.display = '';
        if (savingGoalsModule) savingGoalsModule.style.display = '';
        if (categoryChartModule) categoryChartModule.style.display = '';
        if (loanCalculatorModule) loanCalculatorModule.style.display = '';

        if (familyApprovalModule) familyApprovalModule.style.display = '';
        if (auditorMonitorModule) auditorMonitorModule.style.display = '';
        if (adminCmsModule) adminCmsModule.style.display = '';
        if (adminRecoveryModule) adminRecoveryModule.style.display = '';

        // Resize biểu đồ Chart.js
        setTimeout(() => {
            if (dailyChartInstance) dailyChartInstance.resize();
            if (monthlyChartInstance) monthlyChartInstance.resize();
            if (categoryChartInstance) categoryChartInstance.resize();
        }, 150);

    } else {
        // --- CHẾ ĐỘ XEM CHUYÊN SÂU TỪNG PHÂN HỆ (FOCUSED VIEW) ---
        // Ẩn KPI Cards khi vào chế độ xem chi tiết từng phân hệ để module nổi bật ngay trên đầu
        if (kpiCardsRow) kpiCardsRow.style.display = 'none';
        if (viewHeader) viewHeader.style.display = 'block';

        // Ẩn role modules đặc thù trừ khi được chọn
        if (familyApprovalModule) familyApprovalModule.style.display = (view === 'family-approval') ? 'block' : 'none';
        if (auditorMonitorModule) auditorMonitorModule.style.display = (view === 'auditor-monitor') ? 'block' : 'none';
        if (adminCmsModule) adminCmsModule.style.display = (view === 'admin-live-cms') ? 'block' : 'none';
        if (adminRecoveryModule) adminRecoveryModule.style.display = (view === 'admin-recovery') ? 'block' : 'none';

        if (view === 'wallets') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-wallet2 text-primary';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-primary-subtle text-primary rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Ví & Quỹ'; viewHeaderBadge.className = 'badge bg-primary'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Quản Lý Ví & Quỹ Tài Chính';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Quản lý số dư, đối soát sổ cái tự động, chuyển tiền liên ví chống deadlock và tạo ví đa tiền tệ.';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-sm btn-outline-success" data-bs-toggle="modal" data-bs-target="#reconciliationModal" onclick="openReconciliationModal()"><i class="bi bi-shield-check me-1"></i>Đối Soát Sổ Cái</button>
                    <button type="button" class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#createWalletModal" onclick="openModal('createWalletModal')"><i class="bi bi-wallet-fill me-1"></i>Tạo Ví Mới</button>
                    <button type="button" class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#quickTransferModal" onclick="openModal('quickTransferModal')"><i class="bi bi-arrow-left-right me-1"></i>Chuyển Tiền Liên Ví</button>
                    ${overviewButtonHtml}
                `;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'flex';
            if (colLeft) { colLeft.className = 'col-12'; colLeft.style.display = 'block'; }
            if (colRight) { colRight.style.display = 'none'; }
            if (walletsModule) walletsModule.style.display = 'block';
            if (biAnalyticsModule) biAnalyticsModule.style.display = 'none';
            if (transactionsModule) transactionsModule.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';

        } else if (view === 'transactions') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-clock-history text-primary';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-primary-subtle text-primary rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Sổ Cái Giao Dịch'; viewHeaderBadge.className = 'badge bg-primary'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Nhật Ký & Lịch Sử Giao Dịch';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Theo dõi toàn bộ biến động dòng tiền, đảm bảo tính toàn vẹn ACID, nhập xuất Excel và Báo cáo PDF.';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#quickTransactionModal" onclick="openModal('quickTransactionModal')"><i class="bi bi-plus-lg me-1"></i>Giao Dịch Mới</button>
                    <button type="button" class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#importExcelModal" onclick="openModal('importExcelModal')"><i class="bi bi-file-earmark-arrow-up me-1"></i>Nhập Excel</button>
                    <a href="/api/reports/export/excel" class="btn btn-sm btn-outline-success"><i class="bi bi-file-earmark-excel me-1"></i>Xuất Excel</a>
                    <a href="/api/reports/export/pdf" class="btn btn-sm btn-outline-danger"><i class="bi bi-file-earmark-pdf me-1"></i>Báo Cáo PDF</a>
                    ${overviewButtonHtml}
                `;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'flex';
            if (colLeft) { colLeft.className = 'col-12'; colLeft.style.display = 'block'; }
            if (colRight) { colRight.style.display = 'none'; }
            if (walletsModule) walletsModule.style.display = 'none';
            if (biAnalyticsModule) biAnalyticsModule.style.display = 'none';
            if (transactionsModule) transactionsModule.style.display = 'block';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';

        } else if (view === 'budgets') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-pie-chart text-warning';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-warning-subtle text-warning rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Ngân Sách 50/30/20'; viewHeaderBadge.className = 'badge bg-warning text-dark'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Hoạch Định & Kiểm Soát Ngân Sách';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Quản lý hạn mức chi tiêu theo danh mục, tự động cảnh báo ngưỡng 80% và 100% qua STOMP WebSocket.';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#createBudgetModal" onclick="openModal('createBudgetModal')"><i class="bi bi-plus-lg me-1"></i>Tạo Ngân Sách</button>
                    <button type="button" class="btn btn-sm btn-outline-warning text-dark" data-bs-toggle="modal" data-bs-target="#create503020Modal" onclick="openModal('create503020Modal')"><i class="bi bi-magic me-1"></i>Kích Hoạt Gói 50/30/20</button>
                    ${overviewButtonHtml}
                `;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'flex';
            if (colLeft) { colLeft.style.display = 'none'; }
            if (colRight) { colRight.className = 'col-12'; colRight.style.display = 'block'; }
            if (budgetsModule) budgetsModule.style.display = 'block';
            if (healthScoreModule) healthScoreModule.style.display = 'none';
            if (savingGoalsModule) savingGoalsModule.style.display = 'none';
            if (categoryChartModule) categoryChartModule.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';

        } else if (view === 'saving-goals') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-piggy-bank text-success';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-success-subtle text-success rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Mục Tiêu Tích Lũy'; viewHeaderBadge.className = 'badge bg-success'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Quỹ Mục Tiêu Tiết Kiệm';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Theo dõi tiến độ tích lũy, nạp tiền vào quỹ dự phòng và xây dựng nền tảng tài chính bền vững.';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-sm btn-success" data-bs-toggle="modal" data-bs-target="#createSavingGoalModal" onclick="openModal('createSavingGoalModal')"><i class="bi bi-plus-circle me-1"></i>Tạo Quỹ Tiết Kiệm Mới</button>
                    ${overviewButtonHtml}
                `;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'flex';
            if (colLeft) { colLeft.style.display = 'none'; }
            if (colRight) { colRight.className = 'col-12'; colRight.style.display = 'block'; }
            if (savingGoalsModule) savingGoalsModule.style.display = 'block';
            if (healthScoreModule) healthScoreModule.style.display = 'none';
            if (budgetsModule) budgetsModule.style.display = 'none';
            if (categoryChartModule) categoryChartModule.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';

        } else if (view === 'loan-calculator') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-calculator text-primary';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-primary-subtle text-primary rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Kế Hoạch Vay Vốn'; viewHeaderBadge.className = 'badge bg-primary'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Tính Toán Khoản Vay & Lịch Trả Nợ Ngân Hàng';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Công cụ lập lịch trình trả góp Niên kim cố định và Dư nợ giảm dần, phân bổ gốc/lãi chi tiết từng kỳ.';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = overviewButtonHtml;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'flex';

        } else if (view === 'bi-analytics') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-bar-chart-line text-info';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-info-subtle text-info rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Báo Cáo BI'; viewHeaderBadge.className = 'badge bg-info text-dark'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Báo Cáo Phân Tích Dòng Tiền & Tỷ Trọng Chi Tiêu';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Trực quan hóa xu hướng thu/chi thời gian thực theo ngày, chu kỳ 12 tháng và cơ cấu danh mục.';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = overviewButtonHtml;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'flex';
            if (colLeft) { colLeft.className = 'col-12 col-xl-8'; colLeft.style.display = 'block'; }
            if (colRight) { colRight.className = 'col-12 col-xl-4'; colRight.style.display = 'block'; }

            if (walletsModule) walletsModule.style.display = 'none';
            if (biAnalyticsModule) biAnalyticsModule.style.display = 'block';
            if (transactionsModule) transactionsModule.style.display = 'none';
            if (healthScoreModule) healthScoreModule.style.display = 'block';
            if (budgetsModule) budgetsModule.style.display = 'none';
            if (savingGoalsModule) savingGoalsModule.style.display = 'none';
            if (categoryChartModule) categoryChartModule.style.display = 'block';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';

            setTimeout(() => {
                if (dailyChartInstance) dailyChartInstance.resize();
                if (monthlyChartInstance) monthlyChartInstance.resize();
                if (categoryChartInstance) categoryChartInstance.resize();
            }, 150);

        } else if (view === 'family-approval') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-people-fill text-success';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-success-subtle text-success rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Quỹ Gia Đình'; viewHeaderBadge.className = 'badge bg-success'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Quản Lý Quỹ Gia Đình & Phê Duyệt Chi Tiêu';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Giám sát số dư các quỹ gia đình, hạn mức chi tiêu thành viên và phê duyệt giải ngân giao dịch vượt ngưỡng (≥ 5.000.000 VND).';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-sm btn-outline-success fw-semibold" data-bs-toggle="modal" data-bs-target="#createWalletModal" onclick="openCreateFamilyWalletModal()"><i class="bi bi-plus-circle me-1"></i>Tạo Quỹ Gia Đình Mới</button>
                    <button type="button" class="btn btn-sm btn-outline-primary fw-semibold" data-bs-toggle="modal" data-bs-target="#quickTransferModal" onclick="openFamilyTransferModal()"><i class="bi bi-arrow-left-right me-1"></i>Chuyển Tiền Vào Quỹ</button>
                    <button type="button" class="btn btn-sm btn-success fw-semibold" data-bs-toggle="modal" data-bs-target="#quickTransactionModal" onclick="openFamilyTransactionModal()"><i class="bi bi-plus-lg me-1"></i>Tạo Giao Dịch Từ Quỹ</button>
                    ${overviewButtonHtml}
                `;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';
            if (familyApprovalModule) familyApprovalModule.style.display = 'block';

        } else if (view === 'auditor-monitor') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-fingerprint text-dark';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-dark-subtle text-dark rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Kiểm Toán Viên'; viewHeaderBadge.className = 'badge bg-dark'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Bảng Giám Sát An Ninh & Kiểm Toán Thời Gian Thực';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Theo dõi nhật ký kiểm toán hệ thống, mở khóa brute-force và áp dụng Data Masking bảo mật.';
            if (viewHeaderActions) {
                viewHeaderActions.innerHTML = overviewButtonHtml;
            }

            if (mainDashboardRow) mainDashboardRow.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';
            if (auditorMonitorModule) auditorMonitorModule.style.display = 'block';

        } else if (view === 'admin-live-cms') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-sliders text-danger';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-danger-subtle text-danger rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Quản Trị Hệ Thống'; viewHeaderBadge.className = 'badge bg-danger'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Cấu Hình Tham Số Live CMS Toàn Hệ Thống';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Cập nhật cấu hình hạn mức, chính sách bảo mật và thông điệp banner thời gian thực.';
            if (viewHeaderActions) viewHeaderActions.innerHTML = overviewButtonHtml;
            if (mainDashboardRow) mainDashboardRow.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';
            if (adminCmsModule) adminCmsModule.style.display = 'block';

        } else if (view === 'admin-recovery') {
            if (viewHeaderIcon) viewHeaderIcon.className = 'bi bi-life-preserver text-warning';
            if (viewHeaderIconBox) viewHeaderIconBox.className = 'p-3 bg-warning-subtle text-warning rounded-3 me-3 fs-3 d-flex align-items-center justify-content-center';
            if (viewHeaderBadge) { viewHeaderBadge.textContent = 'Cứu Hộ Tài Khoản'; viewHeaderBadge.className = 'badge bg-warning text-dark'; }
            if (viewHeaderTitle) viewHeaderTitle.textContent = 'Chuyên Mục: Tiếp Nhận & Xử Lý Yêu Cầu Cứu Hộ Khẩn Cấp';
            if (viewHeaderDesc) viewHeaderDesc.textContent = 'Hỗ trợ mở khóa tài khoản, cấp lại quyền truy cập và giải quyết khiếu nại giao dịch.';
            if (viewHeaderActions) viewHeaderActions.innerHTML = overviewButtonHtml;
            if (mainDashboardRow) mainDashboardRow.style.display = 'none';
            if (loanCalculatorModule) loanCalculatorModule.style.display = 'none';
            if (adminRecoveryModule) adminRecoveryModule.style.display = 'block';
        }
    }

    // Cuộn mượt mà lên đầu khu vực làm việc để người dùng thấy rõ thay đổi
    try {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (e) {
        // Ignored
    }
};

window.addEventListener('hashchange', function () {
    const rawHash = window.location.hash.replace(/^#/, '');
    if (rawHash) {
        window.switchDashboardView(rawHash);
    } else {
        window.switchDashboardView('overview');
    }
});

/**
 * Cuộn trang mượt mà hoặc chuyển View tương ứng
 */
window.smoothScrollTo = function (sectionId) {
    if (!sectionId) return;
    const cleanId = sectionId.replace(/-section$/, '').replace(/^#/, '');
    window.switchDashboardView(cleanId);
};

/**
 * Mở Modal chỉnh sửa tham số cấu hình từ nút bấm có gắn Data Attributes an toàn
 */
window.openEditConfigFromButton = function (buttonEl) {
    if (!buttonEl) return;
    const key = buttonEl.getAttribute('data-key') || '';
    const val = buttonEl.getAttribute('data-val') || '';
    const group = buttonEl.getAttribute('data-group') || '';
    const desc = buttonEl.getAttribute('data-desc') || '';
    window.openEditConfigModal(key, val, group, desc);
};

/**
 * Mở Modal chỉnh sửa tham số cấu hình thời gian thực (ROLE_SUPER_ADMIN)
 */
window.openEditConfigModal = function (configKey, currentValue, group, description) {
    try {
        const keyInput = document.getElementById('liveConfigKey');
        const valInput = document.getElementById('liveConfigValue');
        const grpInput = document.getElementById('liveConfigGroup');
        const descInput = document.getElementById('liveConfigDesc');

        if (keyInput) keyInput.value = configKey || '';
        if (valInput) valInput.value = currentValue || '';
        if (grpInput) grpInput.value = group || '';
        if (descInput) descInput.value = description || '';

        window.openModal('editConfigModal');
    } catch (e) {
        console.error('[FinVault Admin CMS] Lỗi hiển thị modal cấu hình:', e);
    }
};

/**
 * Gia trưởng duyệt hoặc từ chối chi tiêu quỹ gia đình (ROLE_FAMILY_HEAD / SUPER_ADMIN)
 */
window.decideFamilyTransaction = function (transactionId, isApproved) {
    const actionText = isApproved ? 'phê duyệt' : 'từ chối';
    if (!confirm('Bạn có chắc chắn muốn ' + actionText + ' khoản chi gia đình này?')) {
        return;
    }

    const payload = {
        approved: isApproved,
        rejectionReason: isApproved ? null : 'Chủ quỹ gia đình từ chối khoản chi tiêu này.'
    };

    fetch('/api/transactions/' + transactionId + '/approval', {
        method: 'POST',
        headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast(
                        'Quyết Định Duyệt Chi',
                        (isApproved ? 'Đã duyệt thành công và giải ngân giao dịch #' : 'Đã từ chối giao dịch #') + transactionId,
                        isApproved ? 'success' : 'danger'
                    );
                } else {
                    alert(result.message || 'Thao tác thành công!');
                }
                // Xóa hàng trong bảng chờ duyệt
                const row = document.getElementById('pending-tx-row-' + transactionId);
                if (row) {
                    row.remove();
                }
                const countBadge = document.getElementById('pending-approvals-count');
                if (countBadge) {
                    let current = parseInt(countBadge.textContent || '1') - 1;
                    countBadge.textContent = Math.max(0, current);
                    if (current <= 0) {
                        const tableBody = document.getElementById('pending-approvals-tbody');
                        if (tableBody) {
                            tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3"><i class="bi bi-check2-all me-1"></i>Không có giao dịch nào đang chờ duyệt.</td></tr>';
                        }
                    }
                }
                const navBadge = document.getElementById('nav-pending-badge');
                if (navBadge) {
                    let current = parseInt(navBadge.textContent || '1') - 1;
                    if (current <= 0) navBadge.remove();
                    else navBadge.textContent = current;
                }
                // Tự động tải lại trang sau 1s để cập nhật số dư ví gia đình và biểu đồ
                setTimeout(() => window.location.reload(), 1000);
            } else {
                alert('Không thể thực hiện: ' + (result.message || 'Lỗi server'));
            }
        })
        .catch(err => {
            console.error('Lỗi phê duyệt giao dịch:', err);
            alert('Lỗi kết nối khi duyệt giao dịch gia đình.');
        });
};

/**
 * Kiểm toán viên mở khóa tài khoản người dùng bị khóa do brute-force (ROLE_SYSTEM_AUDITOR / ADMIN)
 */
window.unlockUserAccount = function (username) {
    if (!confirm('Xác nhận mở khóa tài khoản người dùng: [' + username + ']?')) {
        return;
    }

    fetch('/api/audit/locked-accounts/' + encodeURIComponent(username) + '/unlock', {
        method: 'POST',
        headers: window.getCsrfHeaders()
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Mở Khóa Thành Công', 'Tài khoản [' + username + '] đã được phục hồi quyền truy cập!', 'success');
                } else {
                    alert('Đã mở khóa tài khoản thành công!');
                }
                const row = document.getElementById('locked-user-row-' + username);
                if (row) row.remove();
            } else {
                alert('Lỗi mở khóa: ' + (result.message || 'Không thành công'));
            }
        })
        .catch(err => {
            console.error('Lỗi mở khóa tài khoản:', err);
            alert('Đã xảy ra lỗi khi mở khóa tài khoản.');
        });
};

/**
 * Super Admin khôi phục đối tượng đã bị xóa mềm (Soft Delete Recovery)
 */
window.restoreSoftDeletedItem = function (type, id) {
    if (!confirm('Bạn có chắc chắn muốn khôi phục ' + type + ' (ID: ' + id + ') về trạng thái hoạt động?')) {
        return;
    }

    let endpoint = '';
    if (type === 'WALLET') endpoint = '/api/admin/recovery/wallets/' + id + '/restore';
    else if (type === 'TRANSACTION') endpoint = '/api/admin/recovery/transactions/' + id + '/restore';
    else if (type === 'BUDGET') endpoint = '/api/admin/recovery/budgets/' + id + '/restore';
    else if (type === 'SAVING_GOAL') endpoint = '/api/admin/recovery/saving-goals/' + id + '/restore';

    if (!endpoint) return;

    fetch(endpoint, {
        method: 'POST',
        headers: window.getCsrfHeaders()
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Phục Hồi Dữ Liệu', 'Đã khôi phục ' + type + ' #' + id + ' thành công!', 'success');
                } else {
                    alert('Đã khôi phục thành công!');
                }
                const row = document.getElementById('deleted-item-row-' + type + '-' + id);
                if (row) row.remove();
                setTimeout(() => window.location.reload(), 1000);
            } else {
                alert('Lỗi khôi phục: ' + result.message);
            }
        })
        .catch(err => {
            console.error('Lỗi khôi phục:', err);
            alert('Đã xảy ra lỗi khi khôi phục đối tượng dữ liệu.');
        });
};

/**
 * Đổi trạng thái hiển thị thông báo hệ thống
 */
window.toggleNotificationStatus = function (notifId, currentStatus) {
    const newStatus = !currentStatus;
    fetch('/api/admin/cms/notifications/' + notifId + '/toggle?active=' + newStatus, {
        method: 'PATCH',
        headers: window.getCsrfHeaders()
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Thông Báo Hệ Thống', 'Đã cập nhật trạng thái thông báo thành công!', 'info');
                }
                setTimeout(() => window.location.reload(), 800);
            } else {
                alert('Lỗi: ' + result.message);
            }
        })
        .catch(err => console.error('Lỗi toggle notification:', err));
};

/**
 * Định dạng số tiền VND với dấu phân cách hàng nghìn
 */
window.formatVnd = function (val) {
    if (val === null || val === undefined) return '0 VND';
    const num = Number(val);
    if (isNaN(num)) return '0 VND';
    return num.toLocaleString('vi-VN') + ' VND';
};

/**
 * Xóa mềm giao dịch và hoàn trả số dư ví tương ứng
 */
window.deleteTransaction = function (transactionId) {
    if (!confirm('Bạn có chắc chắn muốn xóa giao dịch #' + transactionId + '?\nSố dư ví sẽ được tự động hoàn trả tương ứng.')) {
        return;
    }

    fetch('/api/transactions/' + transactionId, {
        method: 'DELETE',
        headers: window.getCsrfHeaders()
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Xóa Giao Dịch', 'Đã xóa giao dịch #' + transactionId + ' và cập nhật số dư ví thành công!', 'success');
                } else {
                    alert('Đã xóa giao dịch thành công!');
                }
                const row = document.getElementById('tx-row-' + transactionId);
                if (row) row.remove();
                setTimeout(() => window.location.reload(), 1000);
            } else {
                alert('Không thể xóa giao dịch: ' + (result.message || 'Lỗi server'));
            }
        })
        .catch(err => {
            console.error('Lỗi xóa giao dịch:', err);
            alert('Lỗi kết nối khi xóa giao dịch.');
        });
};

/**
 * Xóa ngân sách tài chính
 */
window.deleteBudget = function (budgetId) {
    if (!confirm('Bạn có chắc chắn muốn xóa ngân sách này?')) {
        return;
    }

    fetch('/api/budgets/' + budgetId, {
        method: 'DELETE',
        headers: window.getCsrfHeaders()
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Xóa Ngân Sách', 'Đã xóa ngân sách thành công!', 'success');
                } else {
                    alert('Đã xóa ngân sách thành công!');
                }
                const card = document.getElementById('budget-card-' + budgetId);
                if (card) card.remove();
                setTimeout(() => window.location.reload(), 1000);
            } else {
                alert('Lỗi xóa ngân sách: ' + (result.message || 'Lỗi server'));
            }
        })
        .catch(err => {
            console.error('Lỗi xóa ngân sách:', err);
            alert('Lỗi kết nối khi xóa ngân sách.');
        });
};

/**
 * Xóa / Hủy mục tiêu tiết kiệm
 */
window.deleteSavingGoal = function (goalId) {
    if (!confirm('Bạn có chắc chắn muốn hủy mục tiêu tiết kiệm này?')) {
        return;
    }

    fetch('/api/saving-goals/' + goalId, {
        method: 'DELETE',
        headers: window.getCsrfHeaders()
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Hủy Mục Tiêu', 'Đã hủy mục tiêu tiết kiệm thành công!', 'success');
                } else {
                    alert('Đã hủy mục tiêu tiết kiệm thành công!');
                }
                const card = document.getElementById('goal-card-' + goalId);
                if (card) card.remove();
                setTimeout(() => window.location.reload(), 1000);
            } else {
                alert('Lỗi hủy mục tiêu: ' + (result.message || 'Lỗi server'));
            }
        })
        .catch(err => {
            console.error('Lỗi hủy mục tiêu tiết kiệm:', err);
            alert('Lỗi kết nối khi hủy mục tiêu tiết kiệm.');
        });
};

/**
 * Mở modal nạp tiền vào Quỹ mục tiêu tiết kiệm
 */
window.openContributeModal = function (goalId, goalName) {
    const idInput = document.getElementById('depositGoalId');
    const textSpan = document.getElementById('depositGoalNameText');
    const amtInput = document.getElementById('depositAmount');

    if (idInput) idInput.value = goalId;
    if (textSpan) textSpan.textContent = goalName;
    if (amtInput) amtInput.value = '';

    window.openModal('contributeSavingGoalModal');
};

/**
 * Mở modal đối soát sổ cái
 */
window.openReconciliationModal = function () {
    window.openModal('reconciliationModal');
};

/**
 * Mở modal tạo ví với loại mặc định là Quỹ Gia Đình (FAMILY_FUND)
 */
window.openCreateFamilyWalletModal = function () {
    const typeSelect = document.getElementById('newWalletType');
    if (typeSelect) {
        typeSelect.value = 'FAMILY_FUND';
    }
    const nameInput = document.getElementById('newWalletName');
    if (nameInput) {
        nameInput.value = '';
        nameInput.placeholder = 'Ví dụ: Quỹ Tài Chính Chung Gia Đình, Quỹ Sinh Hoạt...';
    }
    window.openModal('createWalletModal');
};

/**
 * Mở modal tạo giao dịch chi tiêu từ Quỹ Gia Đình
 * Tự động chọn ví Quỹ gia đình và gán loại giao dịch là Chi Tiêu (EXPENSE)
 */
window.openFamilyTransactionModal = function (walletId) {
    const walletSelect = document.getElementById('txWalletId');
    if (walletSelect) {
        if (walletId) {
            walletSelect.value = walletId;
        } else {
            for (let i = 0; i < walletSelect.options.length; i++) {
                const opt = walletSelect.options[i];
                if (opt.text.toLowerCase().includes('gia đình') || 
                    opt.text.toUpperCase().includes('FAMILY') || 
                    opt.value === '3') {
                    walletSelect.selectedIndex = i;
                    break;
                }
            }
        }
    }
    const typeSelect = document.getElementById('txType');
    if (typeSelect) {
        typeSelect.value = 'EXPENSE';
        if (typeof window.filterTransactionCategories === 'function') {
            window.filterTransactionCategories();
        }
    }
    window.openModal('quickTransactionModal');
};

/**
 * Mở modal chuyển tiền vào Quỹ Gia Đình
 * Tự động đặt ví Quỹ gia đình làm Ví Đích (Target Wallet)
 */
window.openFamilyTransferModal = function (targetWalletId) {
    const targetSelect = document.getElementById('transferTargetWalletId');
    const sourceSelect = document.getElementById('transferSourceWalletId');
    if (targetSelect) {
        if (targetWalletId) {
            targetSelect.value = targetWalletId;
        } else {
            for (let i = 0; i < targetSelect.options.length; i++) {
                const opt = targetSelect.options[i];
                if (opt.text.toLowerCase().includes('gia đình') || 
                    opt.text.toUpperCase().includes('FAMILY') || 
                    opt.value === '3') {
                    targetSelect.selectedIndex = i;
                    break;
                }
            }
        }
        // Đảm bảo ví nguồn khác ví đích để không bị xung đột cùng một ví
        if (sourceSelect && sourceSelect.value === targetSelect.value) {
            for (let j = 0; j < sourceSelect.options.length; j++) {
                if (sourceSelect.options[j].value !== targetSelect.value) {
                    sourceSelect.selectedIndex = j;
                    break;
                }
            }
        }
    }
    window.openModal('quickTransferModal');
};

/**
 * Mở modal đối soát cho một ví cụ thể và tự động chạy đối soát
 */
window.openReconciliationForWallet = function (walletId, walletName) {
    const selectEl = document.getElementById('reconcileWalletSelect');
    if (selectEl) {
        selectEl.value = walletId;
    }
    window.openReconciliationModal();
    window.runWalletReconciliation();
};

/**
 * Thực hiện đối soát số dư sổ cái cho ví được chọn
 */
window.runWalletReconciliation = function () {
    const selectEl = document.getElementById('reconcileWalletSelect');
    if (!selectEl) return;
    const walletId = selectEl.value;
    if (!walletId) {
        alert('Vui lòng chọn một ví để đối soát.');
        return;
    }

    const container = document.getElementById('reconciliationResultContainer');
    const badge = document.getElementById('reconcileStatusBadge');

    fetch('/api/transactions/reconcile/' + walletId)
        .then(res => res.json())
        .then(result => {
            if (!result.success || !result.data) {
                alert('Không thể thực hiện đối soát: ' + (result.message || 'Lỗi không xác định'));
                return;
            }
            const d = result.data;

            document.getElementById('recInitialBalance').textContent = window.formatVnd(d.initialBalance);
            document.getElementById('recTotalIncome').textContent = '+' + window.formatVnd(d.totalIncome);
            document.getElementById('recTransfersIn').textContent = '+' + window.formatVnd(d.totalTransfersIn);
            document.getElementById('recTotalExpense').textContent = '-' + window.formatVnd(d.totalExpense);
            document.getElementById('recTransfersOut').textContent = '-' + window.formatVnd(d.totalTransfersOut);
            document.getElementById('recNetFlow').textContent = window.formatVnd(d.netFlow);
            document.getElementById('recCalculatedBalance').textContent = window.formatVnd(d.calculatedBalance);
            document.getElementById('recActualBalance').textContent = window.formatVnd(d.actualBalance);
            document.getElementById('recDiscrepancy').textContent = window.formatVnd(d.discrepancy);

            if (d.isDiscrepancyDetected) {
                badge.className = 'alert alert-danger mb-3 py-2 small fw-semibold';
                badge.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i>Phát hiện chênh lệch: Số dư sổ sách khác số dư thực tế trong ví!';
                document.getElementById('recDiscrepancy').className = 'fs-5 text-danger';
            } else {
                badge.className = 'alert alert-success mb-3 py-2 small fw-semibold';
                badge.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>Sổ cái toàn vẹn 100%: Số dư sổ sách khớp hoàn toàn với số dư ví!';
                document.getElementById('recDiscrepancy').className = 'fs-5 text-success';
            }

            if (container) container.style.display = 'block';
        })
        .catch(err => {
            console.error('Lỗi đối soát:', err);
            alert('Lỗi kết nối khi đối soát số dư ví.');
        });
};

/**
 * Mở modal quản lý bảo mật 2 lớp (2FA)
 */
window.openTwoFactorModal = function () {
    const modalEl = document.getElementById('twoFactorModal');
    if (!modalEl || !window.bootstrap) return;

    const statusText = document.getElementById('twoFactorStatusText');
    const statusBadge = document.getElementById('twoFactorStatusBadge');
    const setupContainer = document.getElementById('twoFactorSetupContainer');
    const disableContainer = document.getElementById('twoFactorDisableContainer');
    const initialStep = document.getElementById('twoFactorInitialStep');
    const qrStep = document.getElementById('twoFactorQrStep');

    fetch('/api/2fa/status')
        .then(res => res.json())
        .then(result => {
            const isEnabled = result.success && result.data && result.data.isTwoFactorEnabled;
            if (isEnabled) {
                if (statusText) statusText.textContent = 'Đang bảo vệ tài khoản bằng Google / Microsoft Authenticator.';
                if (statusBadge) {
                    statusBadge.className = 'badge bg-success';
                    statusBadge.textContent = 'Đang Bật';
                }
                if (setupContainer) setupContainer.style.display = 'none';
                if (disableContainer) disableContainer.style.display = 'block';
            } else {
                if (statusText) statusText.textContent = 'Chưa kích hoạt. Bạn nên bật để nâng cao bảo mật tài sản.';
                if (statusBadge) {
                    statusBadge.className = 'badge bg-secondary';
                    statusBadge.textContent = 'Chưa Bật';
                }
                if (setupContainer) setupContainer.style.display = 'block';
                if (disableContainer) disableContainer.style.display = 'none';
                if (initialStep) initialStep.style.display = 'block';
                if (qrStep) qrStep.style.display = 'none';
            }
            window.openModal('twoFactorModal');
        })
        .catch(err => {
            console.error('Lỗi lấy trạng thái 2FA:', err);
            window.openModal('twoFactorModal');
        });
};

/**
 * Khởi tạo mã Secret và hiển thị QR Code để bật 2FA
 */
window.initiateTwoFactorSetup = function () {
    fetch('/api/2fa/setup', {
        method: 'POST',
        headers: window.getCsrfHeaders()
    })
        .then(res => res.json())
        .then(result => {
            if (!result.success || !result.data) {
                alert('Không thể khởi tạo 2FA: ' + (result.message || 'Lỗi server'));
                return;
            }
            const data = result.data;
            const initialStep = document.getElementById('twoFactorInitialStep');
            const qrStep = document.getElementById('twoFactorQrStep');
            const qrImg = document.getElementById('twoFactorQrImage');
            const secretKeyEl = document.getElementById('twoFactorSecretKey');
            const scratchEl = document.getElementById('twoFactorScratchCodes');

            if (secretKeyEl) secretKeyEl.textContent = data.secretKey || '';
            if (qrImg) {
                qrImg.src = 'https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=' + encodeURIComponent(data.qrCodeUrl || '');
            }

            if (scratchEl && data.scratchCodes) {
                scratchEl.innerHTML = data.scratchCodes.map(c => '<span class="badge bg-light text-dark border me-1 mb-1 p-1 font-monospace">' + c + '</span>').join('');
            }

            if (initialStep) initialStep.style.display = 'none';
            if (qrStep) qrStep.style.display = 'block';
        })
        .catch(err => {
            console.error('Lỗi khởi tạo 2FA:', err);
            alert('Lỗi kết nối khi khởi tạo 2FA.');
        });
};

/**
 * Xác nhận mã TOTP để chính thức kích hoạt 2FA
 */
window.confirmEnableTwoFactor = function () {
    const codeInput = document.getElementById('twoFactorConfirmCode');
    const code = codeInput ? codeInput.value.trim() : '';
    if (!code || code.length !== 6) {
        alert('Vui lòng nhập đủ 6 chữ số mã xác thực từ ứng dụng Authenticator.');
        return;
    }

    fetch('/api/2fa/enable', {
        method: 'POST',
        headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
        body: JSON.stringify({ code: code })
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Bảo Mật 2 Lớp', 'Đã kích hoạt bảo mật 2FA thành công!', 'success');
                } else {
                    alert('Kích hoạt bảo mật 2FA thành công!');
                }
                window.closeModal('twoFactorModal');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                alert('Xác nhận thất bại: ' + (result.message || 'Mã xác thực không chính xác'));
            }
        })
        .catch(err => {
            console.error('Lỗi kích hoạt 2FA:', err);
            alert('Lỗi kết nối khi xác thực 2FA.');
        });
};

/**
 * Hủy kích hoạt 2FA
 */
window.disableTwoFactor = function () {
    const codeInput = document.getElementById('twoFactorDisableCode');
    const code = codeInput ? codeInput.value.trim() : '';
    if (!code) {
        alert('Vui lòng nhập mã OTP hoặc Scratch Code để xác nhận tắt 2FA.');
        return;
    }

    if (!confirm('Bạn có chắc chắn muốn tắt bảo mật 2 lớp?')) return;

    fetch('/api/2fa/disable', {
        method: 'POST',
        headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
        body: JSON.stringify({ code: code })
    })
        .then(res => res.json())
        .then(result => {
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Bảo Mật 2 Lớp', 'Đã tắt bảo mật 2FA.', 'info');
                } else {
                    alert('Đã tắt bảo mật 2FA thành công.');
                }
                window.closeModal('twoFactorModal');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                alert('Tắt 2FA thất bại: ' + (result.message || 'Mã không chính xác'));
            }
        })
        .catch(err => {
            console.error('Lỗi tắt 2FA:', err);
            alert('Lỗi kết nối khi tắt 2FA.');
        });
};

// ==============================================================================
// 1. INITIALIZATION ON DOM READY
// ==============================================================================

document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // 1. Khởi tạo an toàn các biểu đồ phân tích BI (Chart.js)
    try { initDailyCashFlowChart(); } catch (e) { console.error('[Chart Error] Daily:', e); }
    try { initMonthlyCashFlowChart(); } catch (e) { console.error('[Chart Error] Monthly:', e); }
    try { initCategoryExpenseChart(); } catch (e) { console.error('[Chart Error] Category:', e); }

    // 2. Lắng nghe các sự kiện Form & Modal trong ứng dụng
    setupQuickTransactionForm();
    setupQuickTransferForm();
    setupCreateWalletForm();
    setupAdminLiveCms();
    setupCreateBudgetForm();
    setup503020BudgetForm();
    setupCreateSavingGoalForm();
    setupContributeSavingGoalForm();
    setupCreateCategoryForm();
    setupImportExcelForm();
    setupLoanCalculatorForm();
    setupEditWalletForm();
    setupAddFamilyMemberForm();
    setupEditTransactionForm();
    setupEditBudgetForm();
    setupEditSavingGoalForm();
    setupWithdrawSavingGoalForm();
    setupChangePasswordForm();
    setupCreateNotificationForm();

    // 3. Khởi tạo Chế độ xem từ URL Hash (nếu có trên đường dẫn)
    const initialHash = window.location.hash.replace(/^#/, '') || window._initialViewHash;
    if (initialHash && initialHash !== 'overview') {
        window.switchDashboardView(initialHash);
    }
});

// ==============================================================================
// 2. CHART.JS VISUALIZATIONS
// ==============================================================================

let dailyChartInstance = null;
let monthlyChartInstance = null;
let categoryChartInstance = null;

function initDailyCashFlowChart() {
    const canvas = document.getElementById('dailyCashFlowChart');
    if (!canvas) return;

    fetch('/api/analytics/cashflow/daily')
        .then(res => res.json())
        .then(response => {
            if (!response.success || !response.data) {
                console.warn('[FinVault Analytics] Không có dữ liệu dòng tiền ngày');
                return;
            }
            const data = response.data;
            const ctx = canvas.getContext('2d');

            if (dailyChartInstance) dailyChartInstance.destroy();

            // Ánh xạ dữ liệu linh hoạt: hỗ trợ dataPoints từ CashFlowTrendDto và mảng trực tiếp fallback
            const points = Array.isArray(data.dataPoints) ? data.dataPoints : null;
            const labels = points
                ? points.map(p => p.label || p.date || '')
                : (data.labels || []);
            const incomes = points
                ? points.map(p => (p.inflow !== undefined ? p.inflow : (p.income !== undefined ? p.income : 0)))
                : (data.incomes || data.inflows || []);
            const expenses = points
                ? points.map(p => (p.outflow !== undefined ? p.outflow : (p.expense !== undefined ? p.expense : 0)))
                : (data.expenses || data.outflows || []);

            dailyChartInstance = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Tổng Thu Nhập (VND)',
                            data: incomes,
                            borderColor: '#10b981',
                            backgroundColor: 'rgba(16, 185, 129, 0.1)',
                            fill: true,
                            tension: 0.35,
                            borderWidth: 2
                        },
                        {
                            label: 'Tổng Chi Tiêu (VND)',
                            data: expenses,
                            borderColor: '#ef4444',
                            backgroundColor: 'rgba(239, 68, 68, 0.08)',
                            fill: true,
                            tension: 0.35,
                            borderWidth: 2
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'top' },
                        tooltip: {
                            callbacks: {
                                label: function (context) {
                                    return context.dataset.label + ': ' + new Intl.NumberFormat('vi-VN').format(context.raw) + ' VND';
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function (value) {
                                    return (value / 1000000).toFixed(1) + 'M';
                                }
                            }
                        }
                    }
                }
            });
            window.dailyChartInstance = dailyChartInstance;
        })
        .catch(err => console.error('[FinVault Analytics] Lỗi tải biểu đồ dòng tiền ngày:', err));
}

function initMonthlyCashFlowChart() {
    const canvas = document.getElementById('monthlyCashFlowChart');
    if (!canvas) return;

    fetch('/api/analytics/cashflow/monthly')
        .then(res => res.json())
        .then(response => {
            if (!response.success || !response.data) return;
            const data = response.data;
            const ctx = canvas.getContext('2d');

            if (monthlyChartInstance) monthlyChartInstance.destroy();

            // Ánh xạ dữ liệu linh hoạt: hỗ trợ dataPoints từ CashFlowTrendDto và mảng trực tiếp fallback
            const points = Array.isArray(data.dataPoints) ? data.dataPoints : null;
            const labels = points
                ? points.map(p => p.label || p.date || '')
                : (data.labels || []);
            const incomes = points
                ? points.map(p => (p.inflow !== undefined ? p.inflow : (p.income !== undefined ? p.income : 0)))
                : (data.incomes || data.inflows || []);
            const expenses = points
                ? points.map(p => (p.outflow !== undefined ? p.outflow : (p.expense !== undefined ? p.expense : 0)))
                : (data.expenses || data.outflows || []);
            const netCashFlows = points
                ? points.map(p => (p.netFlow !== undefined ? p.netFlow : (p.netCashFlow !== undefined ? p.netCashFlow : 0)))
                : (data.netCashFlows || data.netFlows || []);

            monthlyChartInstance = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Thu nhập',
                            data: incomes,
                            backgroundColor: 'rgba(16, 185, 129, 0.85)',
                            borderRadius: 4
                        },
                        {
                            label: 'Chi tiêu',
                            data: expenses,
                            backgroundColor: 'rgba(239, 68, 68, 0.85)',
                            borderRadius: 4
                        },
                        {
                            label: 'Dòng tiền ròng (Net)',
                            data: netCashFlows,
                            backgroundColor: 'rgba(59, 130, 246, 0.85)',
                            borderRadius: 4
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'top' },
                        tooltip: {
                            callbacks: {
                                label: function (context) {
                                    return context.dataset.label + ': ' + new Intl.NumberFormat('vi-VN').format(context.raw) + ' VND';
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function (value) {
                                    return (value / 1000000).toFixed(0) + 'M';
                                }
                            }
                        }
                    }
                }
            });
            window.monthlyChartInstance = monthlyChartInstance;
        })
        .catch(err => console.error('[FinVault Analytics] Lỗi tải biểu đồ 12 tháng:', err));
}

function initCategoryExpenseChart() {
    const canvas = document.getElementById('categoryDoughnutChart');
    if (!canvas) return;

    const renderChart = function (response) {
        if (!response || !response.success || !response.data) return;
        const data = response.data;
        const ctx = canvas.getContext('2d');

        if (categoryChartInstance) categoryChartInstance.destroy();

        // Linh hoạt xử lý cả data.items lẫn trường hợp backend trả về trực tiếp mảng
        const items = Array.isArray(data) ? data : (Array.isArray(data.items) ? data.items : []);
        const labels = items.map(i => i.categoryName || i.name || 'Khác');
        const values = items.map(i => (i.amount !== undefined ? i.amount : (i.totalAmount !== undefined ? i.totalAmount : 0)));
        const colors = items.map(i => i.color || '#3b82f6');

        categoryChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels.length > 0 ? labels : ['Chưa có chi tiêu'],
                datasets: [{
                    data: values.length > 0 ? values : [1],
                    backgroundColor: colors.length > 0 ? colors : ['#e2e8f0'],
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { boxWidth: 12, font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                const val = context.raw;
                                return context.label + ': ' + new Intl.NumberFormat('vi-VN').format(val) + ' VND';
                            }
                        }
                    }
                },
                cutout: '68%'
            }
        });
        window.categoryChartInstance = categoryChartInstance;
    };

    fetch('/api/analytics/category-expenses')
        .then(res => {
            if (!res.ok) {
                return fetch('/api/analytics/expenses/category').then(r => r.json());
            }
            return res.json();
        })
        .then(renderChart)
        .catch(err => {
            fetch('/api/analytics/expenses/category')
                .then(r => r.json())
                .then(renderChart)
                .catch(e => console.error('[FinVault Analytics] Lỗi tải biểu đồ tỷ trọng danh mục:', err, e));
        });
}

// ==============================================================================
// 3. QUICK TRANSACTION & TRANSFER & CREATE WALLET
// ==============================================================================

/**
 * Lọc danh mục chi tiêu / thu nhập tương ứng với loại giao dịch đã chọn
 */
window.filterTransactionCategories = function () {
    const typeSelect = document.getElementById('txType');
    const catSelect = document.getElementById('txCategoryId');
    if (!typeSelect || !catSelect) return;

    const currentType = typeSelect.value;
    let firstMatch = null;
    let hasCurrentSelectionMatch = false;

    for (let i = 0; i < catSelect.options.length; i++) {
        const opt = catSelect.options[i];
        const optType = opt.getAttribute('data-type');
        if (!optType || optType === currentType) {
            opt.style.display = '';
            opt.disabled = false;
            if (!firstMatch) {
                firstMatch = opt;
            }
            if (opt.selected) {
                hasCurrentSelectionMatch = true;
            }
        } else {
            opt.style.display = 'none';
            opt.disabled = true;
            if (opt.selected) {
                opt.selected = false;
            }
        }
    }

    if (!hasCurrentSelectionMatch && firstMatch) {
        firstMatch.selected = true;
        catSelect.value = firstMatch.value;
    }
};

function setupQuickTransactionForm() {
    const form = document.getElementById('quickTransactionForm');
    if (!form) return;

    const txTypeSelect = document.getElementById('txType');
    if (txTypeSelect) {
        txTypeSelect.addEventListener('change', function () {
            if (typeof window.filterTransactionCategories === 'function') {
                window.filterTransactionCategories();
            }
        });
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();

        const walletId = document.getElementById('txWalletId').value;
        const categoryId = document.getElementById('txCategoryId').value;
        const type = document.getElementById('txType').value;
        const amount = document.getElementById('txAmount').value;
        const note = document.getElementById('txNote').value;
        const date = document.getElementById('txDate').value;

        if (!walletId) {
            alert('Vui lòng chọn ví tài chính!');
            return;
        }

        const isRecurringEl = document.getElementById('txIsRecurring');
        const recurringIntervalEl = document.getElementById('txRecurringInterval');
        const isRecurring = isRecurringEl ? isRecurringEl.checked : false;

        const payload = {
            walletId: parseInt(walletId),
            categoryId: parseInt(categoryId),
            transactionType: type,
            amount: parseFloat(amount),
            note: note,
            transactionDate: date ? date + 'T00:00:00' : null,
            isRecurring: isRecurring,
            recurringInterval: (isRecurring && recurringIntervalEl) ? recurringIntervalEl.value : null
        };

        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';

        fetch('/api/transactions', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Lưu Giao Dịch';

                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Giao Dịch Thành Công', result.message || 'Đã ghi nhận giao dịch vào sổ cái an toàn!', 'success');
                    } else {
                        alert('Giao dịch thành công!');
                    }
                    window.closeModal('quickTransactionModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi: ' + (result.message || 'Không thể tạo giao dịch'));
                }
            })
            .catch(err => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Lưu Giao Dịch';
                console.error('Lỗi gửi giao dịch:', err);
                alert('Đã xảy ra lỗi kết nối khi lưu giao dịch.');
            });
    });
}

function setupQuickTransferForm() {
    const form = document.getElementById('quickTransferForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();

        const sourceWalletId = document.getElementById('transferSourceWalletId').value;
        const targetWalletId = document.getElementById('transferTargetWalletId').value;
        const amount = document.getElementById('transferAmount').value;
        const note = document.getElementById('transferNote').value;

        if (sourceWalletId === targetWalletId) {
            alert('Ví nguồn và Ví đích không được trùng nhau!');
            return;
        }

        const payload = {
            sourceWalletId: parseInt(sourceWalletId),
            targetWalletId: parseInt(targetWalletId),
            amount: parseFloat(amount),
            note: note
        };

        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang chuyển tiền...';

        fetch('/api/transactions/transfer', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-arrow-left-right me-1"></i> Xác Nhận Chuyển';

                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Chuyển Tiền Thành Công', 'Đã chuyển tiền liên ví thành công chống Deadlock!', 'success');
                    } else {
                        alert('Chuyển tiền liên ví thành công!');
                    }
                    window.closeModal('quickTransferModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi chuyển tiền: ' + (result.message || 'Thao tác không thành công'));
                }
            })
            .catch(err => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-arrow-left-right me-1"></i> Xác Nhận Chuyển';
                console.error('Lỗi chuyển tiền:', err);
                alert('Lỗi kết nối khi chuyển tiền liên ví.');
            });
    });
}

function setupCreateWalletForm() {
    const form = document.getElementById('createWalletForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();

        const walletName = document.getElementById('newWalletName').value;
        const walletType = document.getElementById('newWalletType').value;
        const initialBalance = document.getElementById('newWalletInitialBalance').value || '0';
        const bankName = document.getElementById('newWalletBankName').value;
        const accountNumber = document.getElementById('newWalletAccountNumber').value;
        const monthlyLimitEl = document.getElementById('newWalletMonthlyLimit');
        const monthlyLimitVal = (monthlyLimitEl && monthlyLimitEl.value && parseFloat(monthlyLimitEl.value) > 0)
            ? parseFloat(monthlyLimitEl.value)
            : null;

        const payload = {
            walletName: walletName.trim(),
            walletType: walletType,
            initialBalance: parseFloat(initialBalance),
            currency: 'VND',
            bankName: bankName ? bankName.trim() : null,
            bankAccountNumber: accountNumber ? accountNumber.trim() : null,
            monthlySpendingLimit: monthlyLimitVal
        };

        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang tạo ví...';

        fetch('/api/wallets', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-wallet2 me-1"></i> Tạo Ví Mới';

                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Tạo Ví Thành Công', 'Ví [' + walletName + '] đã sẵn sàng sử dụng!', 'success');
                    } else {
                        alert('Tạo ví mới thành công!');
                    }
                    window.closeModal('createWalletModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi tạo ví: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="bi bi-wallet2 me-1"></i> Tạo Ví Mới';
                console.error('Lỗi tạo ví:', err);
                alert('Đã xảy ra lỗi khi tạo ví mới.');
            });
    });
}

// ==============================================================================
// 4. SUPER ADMIN LIVE CMS CONFIGURATION FORM
// ==============================================================================

function setupAdminLiveCms() {
    const configForm = document.getElementById('liveConfigForm');
    if (!configForm) return;

    configForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const key = document.getElementById('liveConfigKey').value;
        const value = document.getElementById('liveConfigValue').value;
        const group = document.getElementById('liveConfigGroup').value;
        const desc = document.getElementById('liveConfigDesc').value;

        const payload = {
            configKey: key,
            configValue: value,
            configGroup: group,
            description: desc,
            isActive: true
        };

        fetch('/api/admin/cms/configs/' + encodeURIComponent(key), {
            method: 'PUT',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Cập Nhật Tham Số', 'Đã lưu cấu hình [' + key + '] theo thời gian thực!', 'success');
                    } else {
                        alert('Đã lưu cấu hình thành công!');
                    }
                    window.closeModal('editConfigModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi cập nhật cấu hình: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                console.error('Lỗi lưu config:', err);
                alert('Lỗi kết nối khi cập nhật cấu hình.');
            });
    });
}

// ==============================================================================
// 5. BUDGET MANAGEMENT FORMS (CREATE BUDGET & 50/30/20 PACKAGE)
// ==============================================================================

function setupCreateBudgetForm() {
    const form = document.getElementById('createBudgetForm');
    if (!form) return;

    const typeSelect = document.getElementById('newBudgetType');
    const allocGroup = document.getElementById('budgetAllocationGroup');
    const catGroup = document.getElementById('budgetCategoryGroup');

    function syncBudgetTypeFields() {
        if (!typeSelect) return;
        if (typeSelect.value === 'RULE_50_30_20') {
            if (allocGroup) allocGroup.style.display = 'block';
            if (catGroup) catGroup.style.display = 'none';
        } else {
            if (allocGroup) allocGroup.style.display = 'none';
            if (catGroup) catGroup.style.display = 'block';
        }
    }

    if (typeSelect) {
        typeSelect.addEventListener('change', syncBudgetTypeFields);
        syncBudgetTypeFields();
    }

    const startInput = document.getElementById('newBudgetStartDate');
    const endInput = document.getElementById('newBudgetEndDate');
    if (startInput && endInput && !startInput.value) {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
        const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0];
        startInput.value = firstDay;
        endInput.value = lastDay;
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        const budgetTypeVal = document.getElementById('newBudgetType').value;
        const allocTypeEl = document.getElementById('newBudgetAllocationType');

        const payload = {
            budgetName: document.getElementById('newBudgetName').value.trim(),
            budgetType: budgetTypeVal,
            allocationType: (budgetTypeVal === 'RULE_50_30_20' && allocTypeEl) ? allocTypeEl.value : null,
            categoryId: (budgetTypeVal === 'CUSTOM' && document.getElementById('newBudgetCategoryId').value) ? parseInt(document.getElementById('newBudgetCategoryId').value) : null,
            walletId: document.getElementById('newBudgetWalletId').value ? parseInt(document.getElementById('newBudgetWalletId').value) : null,
            allocatedAmount: parseFloat(document.getElementById('newBudgetAllocatedAmount').value),
            startDate: document.getElementById('newBudgetStartDate').value,
            endDate: document.getElementById('newBudgetEndDate').value
        };

        fetch('/api/budgets', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (submitBtn) submitBtn.disabled = false;
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Tạo Ngân Sách', 'Ngân sách [' + payload.budgetName + '] đã được thiết lập thành công!', 'success');
                    } else {
                        alert('Khởi tạo ngân sách thành công!');
                    }
                    window.closeModal('createBudgetModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi tạo ngân sách: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                if (submitBtn) submitBtn.disabled = false;
                console.error('Lỗi tạo ngân sách:', err);
                alert('Đã xảy ra lỗi khi tạo ngân sách.');
            });
    });
}

function setup503020BudgetForm() {
    const form = document.getElementById('create503020Form');
    if (!form) return;

    const startInput = document.getElementById('pkg503020StartDate');
    const endInput = document.getElementById('pkg503020EndDate');
    if (startInput && endInput && !startInput.value) {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
        const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0];
        startInput.value = firstDay;
        endInput.value = lastDay;
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        const payload = {
            monthlyIncome: parseFloat(document.getElementById('pkg503020Income').value),
            walletId: document.getElementById('pkg503020WalletId').value ? parseInt(document.getElementById('pkg503020WalletId').value) : null,
            startDate: document.getElementById('pkg503020StartDate').value,
            endDate: document.getElementById('pkg503020EndDate').value
        };

        fetch('/api/budgets/50-30-20', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (submitBtn) submitBtn.disabled = false;
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Gói Ngân Sách 50/30/20', 'Đã khởi tạo tự động 3 ngân sách Thiết yếu, Sở thích và Tiết kiệm!', 'success');
                    } else {
                        alert('Khởi tạo gói 50/30/20 thành công!');
                    }
                    window.closeModal('create503020Modal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi tạo gói 50/30/20: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                if (submitBtn) submitBtn.disabled = false;
                console.error('Lỗi tạo gói 50/30/20:', err);
                alert('Đã xảy ra lỗi khi khởi tạo gói ngân sách.');
            });
    });
}

// ==============================================================================
// 6. SAVING GOAL FORMS (CREATE & DEPOSIT)
// ==============================================================================

function setupCreateSavingGoalForm() {
    const form = document.getElementById('createSavingGoalForm');
    if (!form) return;

    const targetDateInput = document.getElementById('newGoalTargetDate');
    if (targetDateInput && !targetDateInput.value) {
        const nextYear = new Date();
        nextYear.setFullYear(nextYear.getFullYear() + 1);
        targetDateInput.value = nextYear.toISOString().split('T')[0];
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        const initialAmount = parseFloat(document.getElementById('newGoalInitialAmount').value || '0') || 0;
        const walletEl = document.getElementById('newGoalWalletId');
        const walletIdVal = (walletEl && walletEl.value) ? parseInt(walletEl.value, 10) : null;

        if (initialAmount > 0 && !walletIdVal) {
            alert('Vui lòng chọn ví nguồn để trích nạp số tiền ban đầu!');
            if (submitBtn) submitBtn.disabled = false;
            return;
        }

        const payload = {
            goalName: document.getElementById('newGoalName').value.trim(),
            walletId: walletIdVal,
            targetAmount: parseFloat(document.getElementById('newGoalTargetAmount').value),
            initialAmount: initialAmount,
            targetDate: document.getElementById('newGoalTargetDate').value,
            notes: document.getElementById('newGoalNotes').value.trim() || null
        };

        fetch('/api/saving-goals', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (submitBtn) submitBtn.disabled = false;
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Mục Tiêu Tiết Kiệm', 'Quỹ mục tiêu [' + payload.goalName + '] đã được tạo thành công!', 'success');
                    } else {
                        alert('Tạo mục tiêu tiết kiệm thành công!');
                    }
                    window.closeModal('createSavingGoalModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi tạo mục tiêu: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                if (submitBtn) submitBtn.disabled = false;
                console.error('Lỗi tạo mục tiêu:', err);
                alert('Đã xảy ra lỗi khi tạo mục tiêu tiết kiệm.');
            });
    });
}

function setupContributeSavingGoalForm() {
    const form = document.getElementById('contributeSavingGoalForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const goalId = document.getElementById('depositGoalId').value;
        const sourceWalletId = document.getElementById('depositSourceWalletId').value;
        const amount = parseFloat(document.getElementById('depositAmount').value);

        if (!goalId || !sourceWalletId || !amount || amount < 10000) {
            alert('Vui lòng nhập số tiền nạp tối thiểu 10,000 VND và chọn ví nguồn.');
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        const payload = {
            amount: amount,
            sourceWalletId: parseInt(sourceWalletId)
        };

        fetch('/api/saving-goals/' + goalId + '/deposit', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (submitBtn) submitBtn.disabled = false;
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Nạp Quỹ Thành Công', 'Đã nạp ' + window.formatVnd(amount) + ' vào quỹ mục tiêu!', 'success');
                    } else {
                        alert('Nạp tiền vào quỹ thành công!');
                    }
                    window.closeModal('contributeSavingGoalModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi nạp tiền: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                if (submitBtn) submitBtn.disabled = false;
                console.error('Lỗi nạp tiền:', err);
                alert('Đã xảy ra lỗi khi nạp tiền vào quỹ tiết kiệm.');
            });
    });
}

// ==============================================================================
// 7. CATEGORY CREATION FORM
// ==============================================================================

function setupCreateCategoryForm() {
    const form = document.getElementById('createCategoryForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        const payload = {
            categoryName: document.getElementById('newCategoryName').value.trim(),
            categoryType: document.getElementById('newCategoryType').value,
            icon: document.getElementById('newCategoryIcon').value.trim() || 'bi-tag',
            color: document.getElementById('newCategoryColor').value || '#0d6efd'
        };

        fetch('/api/categories', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (submitBtn) submitBtn.disabled = false;
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Tạo Danh Mục', 'Đã thêm danh mục [' + payload.categoryName + '] thành công!', 'success');
                    } else {
                        alert('Tạo danh mục mới thành công!');
                    }
                    window.closeModal('createCategoryModal');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    alert('Lỗi tạo danh mục: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                if (submitBtn) submitBtn.disabled = false;
                console.error('Lỗi tạo danh mục:', err);
                alert('Đã xảy ra lỗi khi tạo danh mục.');
            });
    });
}

// ==============================================================================
// 8. EXCEL IMPORT TRANSACTIONS
// ==============================================================================

function setupImportExcelForm() {
    const form = document.getElementById('importExcelForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const fileInput = document.getElementById('importExcelFile');
        const walletSelect = document.getElementById('importWalletId');
        const feedbackDiv = document.getElementById('importResultFeedback');

        if (!fileInput.files || fileInput.files.length === 0) {
            alert('Vui lòng chọn file Excel (.xlsx) để tải lên.');
            return;
        }

        const file = fileInput.files[0];
        const formData = new FormData();
        formData.append('file', file);
        if (walletSelect && walletSelect.value) {
            formData.append('walletId', walletSelect.value);
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';
        }

        fetch('/api/reports/import/excel', {
            method: 'POST',
            headers: window.getCsrfHeaders(),
            body: formData
        })
            .then(res => res.json())
            .then(result => {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="bi bi-upload me-1"></i> Tải Lên & Xử Lý';
                }
                if (result.success && result.data) {
                    const d = result.data;
                    if (feedbackDiv) {
                        feedbackDiv.className = 'alert alert-success small mt-3';
                        feedbackDiv.style.display = 'block';
                        feedbackDiv.innerHTML = `<strong>Kết quả xử lý:</strong><br/>
                            - Tổng số dòng: ${d.totalRowsProcessed}<br/>
                            - Thành công: <span class="text-success fw-bold">${d.successCount}</span><br/>
                            - Bỏ qua trùng lặp: ${d.skippedDuplicatesCount}<br/>
                            - Thất bại: <span class="text-danger fw-bold">${d.failureCount}</span>
                        `;
                    }
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Nhập Excel', `Đã nhập thành công ${d.successCount} giao dịch!`, 'success');
                    } else {
                        alert(`Đã nhập thành công ${d.successCount} giao dịch!`);
                    }
                    window.closeModal('importExcelModal');
                    setTimeout(() => window.location.reload(), 1500);
                } else {
                    if (feedbackDiv) {
                        feedbackDiv.className = 'alert alert-danger small mt-3';
                        feedbackDiv.style.display = 'block';
                        feedbackDiv.textContent = 'Lỗi xử lý file: ' + (result.message || 'Thất bại');
                    }
                }
            })
            .catch(err => {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="bi bi-upload me-1"></i> Tải Lên & Xử Lý';
                }
                console.error('Lỗi import excel:', err);
                alert('Đã xảy ra lỗi khi tải lên file Excel.');
            });
    });
}

// ==============================================================================
// 9. LOAN CALCULATOR LOGIC
// ==============================================================================

function setupLoanCalculatorForm() {
    const form = document.getElementById('loanCalculatorForm');
    if (!form) return;

    const startDateInput = document.getElementById('loanStartDate');
    if (startDateInput && !startDateInput.value) {
        startDateInput.value = new Date().toISOString().split('T')[0];
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang tính toán...';
        }

        const saveHistoryEl = document.getElementById('loanSaveHistory');

        const payload = {
            loanTitle: document.getElementById('loanTitle').value.trim(),
            principalAmount: parseFloat(document.getElementById('loanPrincipal').value),
            annualInterestRate: parseFloat(document.getElementById('loanInterestRate').value),
            loanTermMonths: parseInt(document.getElementById('loanTermMonths').value),
            startDate: document.getElementById('loanStartDate').value,
            calculationMethod: document.getElementById('loanCalculationMethod').value,
            saveHistory: saveHistoryEl ? saveHistoryEl.checked : false
        };

        fetch('/api/loans/calculate', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="bi bi-calculator me-1"></i> Tính Lịch Trả Nợ';
                }
                if (!result.success || !result.data) {
                    alert('Lỗi tính toán: ' + (result.message || 'Dữ liệu không hợp lệ'));
                    return;
                }

                const data = result.data;
                document.getElementById('resLoanPrincipal').textContent = window.formatVnd(data.principalAmount);
                document.getElementById('resLoanMonthly').textContent = window.formatVnd(data.monthlyPayment);
                document.getElementById('resLoanTotalInterest').textContent = window.formatVnd(data.totalInterest);
                document.getElementById('resLoanTotalPayable').textContent = window.formatVnd(data.totalPayable);

                const countBadge = document.getElementById('resLoanScheduleCount');
                if (countBadge) countBadge.textContent = (data.amortizationSchedule ? data.amortizationSchedule.length : 0) + ' kỳ';

                const tbody = document.getElementById('loanScheduleTableBody');
                if (tbody && data.amortizationSchedule) {
                    tbody.innerHTML = data.amortizationSchedule.map((item, idx) => {
                        const period = item.monthNumber || item.periodNumber || (idx + 1);
                        const principalPaid = item.principalPayment || 0;
                        const interestPaid = item.interestPayment || 0;
                        const totalPaid = item.totalMonthlyPayment != null ? item.totalMonthlyPayment : (item.totalPayment != null ? item.totalPayment : (principalPaid + interestPaid));
                        const remaining = item.remainingBalance != null ? item.remainingBalance : (item.remainingPrincipal != null ? item.remainingPrincipal : 0);
                        const starting = item.startingBalance != null ? item.startingBalance : (remaining + principalPaid);

                        return `
                        <tr>
                            <td><span class="badge bg-light text-dark border">Kỳ ${period}</span></td>
                            <td>${item.paymentDate || ''}</td>
                            <td class="text-end">${window.formatVnd(starting)}</td>
                            <td class="text-end text-primary">${window.formatVnd(principalPaid)}</td>
                            <td class="text-end text-danger">${window.formatVnd(interestPaid)}</td>
                            <td class="text-end fw-bold text-dark">${window.formatVnd(totalPaid)}</td>
                            <td class="text-end text-muted">${window.formatVnd(remaining)}</td>
                        </tr>
                    `;
                    }).join('');
                }

                const resultContainer = document.getElementById('loanResultContainer');
                if (resultContainer) {
                    resultContainer.style.display = 'block';
                    resultContainer.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                }

                if (window.FinVaultWS) {
                    const msg = payload.saveHistory
                        ? 'Đã tính toán và lưu lịch sử phương án [' + payload.loanTitle + ']!'
                        : 'Đã tính toán thành công lịch trình trả nợ phương án [' + payload.loanTitle + ']!';
                    window.FinVaultWS.showToast('Tính Khoản Vay', msg, 'info');
                }
            })
            .catch(err => {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="bi bi-calculator me-1"></i> Tính Lịch Trả Nợ';
                }
                console.error('Lỗi tính khoản vay:', err);
                alert('Đã xảy ra lỗi khi tính toán khoản vay.');
            });
    });
}

// ==============================================================================
// 10. FULL CRUD EXTENSIONS: WALLETS, FAMILY MEMBERS, TRANSACTIONS, BUDGETS,
//     SAVING GOALS, CATEGORIES, LOANS, PASSWORD & ADMIN CMS
// ==============================================================================

/**
 * Lọc bảng giao dịch phía Client theo từ khóa và loại giao dịch
 */
window.filterTransactionsTable = function () {
    const searchInput = document.getElementById('txSearchInput');
    const typeFilter = document.getElementById('txTypeFilter');
    const tbody = document.getElementById('transactions-tbody');
    if (!tbody) return;

    const query = (searchInput && searchInput.value ? searchInput.value : '').trim().toLowerCase();
    const selectedType = (typeFilter && typeFilter.value) ? typeFilter.value : 'ALL';

    const rows = tbody.querySelectorAll('tr');
    rows.forEach(row => {
        if (!row.hasAttribute('data-tx-type')) return;
        const rowType = row.getAttribute('data-tx-type') || '';
        const rowSearch = (row.getAttribute('data-tx-search') || row.textContent || '').toLowerCase();

        const matchesType = (selectedType === 'ALL') || (rowType === selectedType);
        const matchesQuery = !query || rowSearch.indexOf(query) !== -1;

        row.style.display = (matchesType && matchesQuery) ? '' : 'none';
    });
};

/**
 * Mở Modal chỉnh sửa Ví Tài Chính / Quỹ Gia Đình
 */
window.openEditWalletModal = function (btn) {
    if (!btn) return;
    document.getElementById('editWalletId').value = btn.getAttribute('data-id') || '';
    document.getElementById('editWalletName').value = btn.getAttribute('data-name') || '';
    const typeEl = document.getElementById('editWalletType');
    if (typeEl && btn.getAttribute('data-type')) {
        typeEl.value = btn.getAttribute('data-type');
    }
    document.getElementById('editWalletBankName').value = btn.getAttribute('data-bank-name') || '';
    document.getElementById('editWalletAccountNumber').value = btn.getAttribute('data-account-number') || '';
    document.getElementById('editWalletMonthlyLimit').value = btn.getAttribute('data-monthly-limit') || '';
    window.openModal('editWalletModal');
};

function setupEditWalletForm() {
    const form = document.getElementById('editWalletForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const id = document.getElementById('editWalletId').value;
        if (!id) return;

        const monthlyLimitRaw = document.getElementById('editWalletMonthlyLimit').value;
        const payload = {
            walletName: document.getElementById('editWalletName').value.trim(),
            walletType: document.getElementById('editWalletType').value,
            currency: 'VND',
            bankName: document.getElementById('editWalletBankName').value.trim() || null,
            bankAccountNumber: document.getElementById('editWalletAccountNumber').value.trim() || null,
            monthlySpendingLimit: (monthlyLimitRaw && parseFloat(monthlyLimitRaw) > 0) ? parseFloat(monthlyLimitRaw) : null
        };

        fetch('/api/wallets/' + id, {
            method: 'PUT',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Cập Nhật Ví', 'Đã lưu thay đổi thông tin ví [' + payload.walletName + ']!', 'success');
                    } else {
                        alert('Cập nhật ví thành công!');
                    }
                    window.closeModal('editWalletModal');
                    setTimeout(() => window.location.reload(), 900);
                } else {
                    alert('Lỗi cập nhật ví: ' + (result.message || 'Thao tác thất bại'));
                }
            })
            .catch(err => {
                console.error('Lỗi cập nhật ví:', err);
                alert('Đã xảy ra lỗi khi cập nhật ví.');
            });
    });
}

/**
 * Xóa mềm Ví Tài Chính (Soft Delete)
 */
window.deleteWallet = async function (btn) {
    if (!btn) return;
    const id = btn.getAttribute('data-id');
    const name = btn.getAttribute('data-name') || ('#' + id);
    if (!id) return;

    if (!confirm('Bạn có chắc chắn muốn xóa mềm ví [' + name + '] không? Dữ liệu có thể được khôi phục bởi Quản trị viên.')) {
        return;
    }

    try {
        const res = await fetch('/api/wallets/' + id, {
            method: 'DELETE',
            headers: window.getCsrfHeaders()
        });
        const result = await res.json();
        if (result.success) {
            if (window.FinVaultWS) {
                window.FinVaultWS.showToast('Đã Xóa Ví', 'Ví [' + name + '] đã được chuyển vào thùng rác an toàn.', 'info');
            } else {
                alert('Đã xóa ví thành công!');
            }
            setTimeout(() => window.location.reload(), 900);
        } else {
            alert('Không thể xóa ví: ' + (result.message || 'Thao tác bị từ chối'));
        }
    } catch (err) {
        console.error('Lỗi xóa ví:', err);
        alert('Đã xảy ra lỗi kết nối khi xóa ví.');
    }
};

/**
 * Mở Modal quản lý thành viên Quỹ Gia Đình
 */
window.openFamilyMembersModal = async function (btn) {
    if (!btn) return;
    const walletId = btn.getAttribute('data-wallet-id');
    const walletName = btn.getAttribute('data-wallet-name') || 'Quỹ Gia Đình';
    if (!walletId) return;

    const idInput = document.getElementById('familyModalWalletId');
    const nameEl = document.getElementById('familyModalWalletName');
    if (idInput) idInput.value = walletId;
    if (nameEl) nameEl.textContent = walletName;

    window.openModal('familyMembersModal');
    await window.loadFamilyWalletMembers(walletId);
};

window.loadFamilyWalletMembers = async function (walletId) {
    const tbody = document.getElementById('familyMembersTableBody');
    if (!tbody || !walletId) return;

    tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3"><span class="spinner-border spinner-border-sm me-1"></span>Đang tải danh sách thành viên...</td></tr>';

    try {
        const res = await fetch('/api/wallets/' + walletId);
        const result = await res.json();
        const walletData = result.data || {};
        const members = Array.isArray(walletData.members) ? walletData.members : [];

        if (members.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Chưa có thành viên phụ nào trong quỹ gia đình này.</td></tr>';
            return;
        }

        tbody.innerHTML = members.map(m => {
            const limitVal = m.spendingLimit != null ? m.spendingLimit : 0;
            const displayName = (m.fullName ? m.fullName + ' (' + (m.username || ('ID:' + m.userId)) + ')' : (m.username || ('User #' + m.userId)));
            return `
                <tr>
                    <td>
                        <div class="fw-semibold text-dark">${displayName}</div>
                        <small class="text-muted font-monospace">User ID: #${m.userId}</small>
                    </td>
                    <td><span class="badge bg-info-subtle text-info border border-info-subtle">${m.memberRole || 'MEMBER'}</span></td>
                    <td>
                        <div class="input-group input-group-sm">
                            <input type="number" class="form-control font-monospace" id="member-limit-input-${walletId}-${m.userId}" value="${limitVal}" min="0" step="100000">
                            <button type="button" class="btn btn-outline-primary" onclick="updateFamilyMemberLimit(${walletId}, ${m.userId})" title="Lưu hạn mức">
                                <i class="bi bi-check2"></i>
                            </button>
                        </div>
                    </td>
                    <td class="text-end">
                        <button type="button" class="btn btn-sm btn-outline-danger py-0 px-2" onclick="removeFamilyMember(${walletId}, ${m.userId})" title="Xóa thành viên khỏi quỹ">
                            <i class="bi bi-person-dash"></i> Xóa
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        console.error('Lỗi tải thành viên quỹ gia đình:', err);
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-3">Không thể tải danh sách thành viên.</td></tr>';
    }
};

window.updateFamilyMemberLimit = async function (walletId, userId) {
    const inputEl = document.getElementById(`member-limit-input-${walletId}-${userId}`);
    const limit = inputEl && inputEl.value ? parseFloat(inputEl.value) : 0;

    try {
        const res = await fetch(`/api/wallets/${walletId}/members/${userId}/limit?limit=${encodeURIComponent(limit)}`, {
            method: 'PUT',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify({ spendingLimit: limit })
        });
        const result = await res.json();
        if (result.success) {
            if (window.FinVaultWS) {
                window.FinVaultWS.showToast('Cập Nhật Hạn Mức', 'Đã cập nhật hạn mức chi tiêu thành viên!', 'success');
            } else {
                alert('Đã cập nhật hạn mức thành viên!');
            }
            await window.loadFamilyWalletMembers(walletId);
        } else {
            alert('Lỗi cập nhật hạn mức: ' + (result.message || 'Thất bại'));
        }
    } catch (err) {
        console.error('Lỗi cập nhật hạn mức thành viên:', err);
        alert('Lỗi kết nối khi cập nhật hạn mức thành viên.');
    }
};

window.removeFamilyMember = async function (walletId, userId) {
    if (!confirm('Bạn có chắc chắn muốn xóa thành viên #' + userId + ' khỏi quỹ gia đình này?')) return;

    try {
        const res = await fetch(`/api/wallets/${walletId}/members/${userId}`, {
            method: 'DELETE',
            headers: window.getCsrfHeaders()
        });
        const result = await res.json();
        if (result.success) {
            if (window.FinVaultWS) {
                window.FinVaultWS.showToast('Đã Xóa Thành Viên', 'Đã gỡ thành viên khỏi quỹ gia đình.', 'info');
            }
            await window.loadFamilyWalletMembers(walletId);
        } else {
            alert('Lỗi xóa thành viên: ' + (result.message || 'Thất bại'));
        }
    } catch (err) {
        console.error('Lỗi xóa thành viên:', err);
        alert('Lỗi kết nối khi xóa thành viên khỏi quỹ.');
    }
};

function setupAddFamilyMemberForm() {
    const form = document.getElementById('addFamilyMemberForm');
    if (!form) return;

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        const walletId = document.getElementById('familyModalWalletId').value;
        const userIdRaw = document.getElementById('newFamilyMemberUserId').value;
        const usernameRaw = document.getElementById('newFamilyMemberUsername').value.trim();
        const memberRole = document.getElementById('newFamilyMemberRole').value || 'MEMBER';
        const limitRaw = document.getElementById('newFamilyMemberLimit').value;

        if (!walletId) return;
        if (!userIdRaw && !usernameRaw) {
            alert('Vui lòng nhập ID Người Dùng (User ID) hoặc Username cần thêm vào quỹ!');
            return;
        }

        const payload = {
            userId: userIdRaw ? parseInt(userIdRaw, 10) : null,
            username: usernameRaw || null,
            memberRole: memberRole,
            spendingLimit: limitRaw ? parseFloat(limitRaw) : 0
        };

        try {
            const res = await fetch(`/api/wallets/${walletId}/members`, {
                method: 'POST',
                headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            if (result.success) {
                if (window.FinVaultWS) {
                    window.FinVaultWS.showToast('Thêm Thành Viên', 'Đã thêm thành viên vào quỹ gia đình!', 'success');
                } else {
                    alert('Đã thêm thành viên thành công!');
                }
                form.reset();
                await window.loadFamilyWalletMembers(walletId);
            } else {
                alert('Lỗi thêm thành viên: ' + (result.message || 'Không tìm thấy người dùng hoặc thao tác không hợp lệ'));
            }
        } catch (err) {
            console.error('Lỗi thêm thành viên quỹ:', err);
            alert('Lỗi kết nối khi thêm thành viên vào quỹ gia đình.');
        }
    });
}

/**
 * Mở Modal chỉnh sửa Giao dịch (Edit Transaction)
 */
window.openEditTransactionModal = function (btn) {
    if (!btn) return;
    document.getElementById('editTxId').value = btn.getAttribute('data-id') || '';
    const txType = btn.getAttribute('data-type') || 'EXPENSE';
    const typeEl = document.getElementById('editTxType');
    if (typeEl) typeEl.value = txType === 'INCOME' ? 'INCOME' : 'EXPENSE';

    const walletEl = document.getElementById('editTxWalletId');
    if (walletEl && btn.getAttribute('data-wallet-id')) {
        walletEl.value = btn.getAttribute('data-wallet-id');
    }

    const catEl = document.getElementById('editTxCategoryId');
    if (catEl && btn.getAttribute('data-category-id')) {
        catEl.value = btn.getAttribute('data-category-id');
    }

    document.getElementById('editTxAmount').value = btn.getAttribute('data-amount') || '';
    document.getElementById('editTxDate').value = btn.getAttribute('data-date') || new Date().toISOString().split('T')[0];
    document.getElementById('editTxNote').value = btn.getAttribute('data-note') || '';

    const isRecurring = btn.getAttribute('data-recurring') === 'true';
    const recCheck = document.getElementById('editTxIsRecurring');
    const recGroup = document.getElementById('editTxRecurringIntervalGroup');
    const recInterval = document.getElementById('editTxRecurringInterval');
    if (recCheck) recCheck.checked = isRecurring;
    if (recGroup) recGroup.style.display = isRecurring ? 'block' : 'none';
    if (recInterval && btn.getAttribute('data-interval')) {
        recInterval.value = btn.getAttribute('data-interval');
    }

    window.openModal('editTransactionModal');
};

function setupEditTransactionForm() {
    const form = document.getElementById('editTransactionForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const id = document.getElementById('editTxId').value;
        if (!id) return;

        const isRecurringEl = document.getElementById('editTxIsRecurring');
        const intervalEl = document.getElementById('editTxRecurringInterval');
        const isRecurring = isRecurringEl ? isRecurringEl.checked : false;
        const dateVal = document.getElementById('editTxDate').value;

        const payload = {
            walletId: parseInt(document.getElementById('editTxWalletId').value, 10),
            categoryId: parseInt(document.getElementById('editTxCategoryId').value, 10),
            transactionType: document.getElementById('editTxType').value,
            amount: parseFloat(document.getElementById('editTxAmount').value),
            note: document.getElementById('editTxNote').value.trim(),
            transactionDate: dateVal ? dateVal + 'T00:00:00' : null,
            isRecurring: isRecurring,
            recurringInterval: (isRecurring && intervalEl) ? intervalEl.value : null
        };

        fetch('/api/transactions/' + id, {
            method: 'PUT',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Cập Nhật Giao Dịch', 'Đã cập nhật giao dịch và đồng bộ lại số dư sổ cái!', 'success');
                    } else {
                        alert('Cập nhật giao dịch thành công!');
                    }
                    window.closeModal('editTransactionModal');
                    setTimeout(() => window.location.reload(), 900);
                } else {
                    alert('Lỗi cập nhật giao dịch: ' + (result.message || 'Thao tác thất bại'));
                }
            })
            .catch(err => {
                console.error('Lỗi cập nhật giao dịch:', err);
                alert('Đã xảy ra lỗi kết nối khi cập nhật giao dịch.');
            });
    });
}

/**
 * Mở Modal chỉnh sửa Ngân sách (Edit Budget)
 */
window.openEditBudgetModal = function (btn) {
    if (!btn) return;
    document.getElementById('editBudgetId').value = btn.getAttribute('data-id') || '';
    document.getElementById('editBudgetName').value = btn.getAttribute('data-name') || '';
    const bType = btn.getAttribute('data-type') || 'CUSTOM';
    const typeEl = document.getElementById('editBudgetType');
    if (typeEl) typeEl.value = bType;

    const allocEl = document.getElementById('editBudgetAllocationType');
    if (allocEl && btn.getAttribute('data-allocation-type')) {
        allocEl.value = btn.getAttribute('data-allocation-type');
    }

    const catEl = document.getElementById('editBudgetCategoryId');
    if (catEl) catEl.value = btn.getAttribute('data-category-id') || '';

    const walletEl = document.getElementById('editBudgetWalletId');
    if (walletEl) walletEl.value = btn.getAttribute('data-wallet-id') || '';

    document.getElementById('editBudgetAllocatedAmount').value = btn.getAttribute('data-allocated') || '';
    document.getElementById('editBudgetStartDate').value = btn.getAttribute('data-start') || '';
    document.getElementById('editBudgetEndDate').value = btn.getAttribute('data-end') || '';

    window.openModal('editBudgetModal');
};

function setupEditBudgetForm() {
    const form = document.getElementById('editBudgetForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const id = document.getElementById('editBudgetId').value;
        if (!id) return;

        const budgetType = document.getElementById('editBudgetType').value;
        const catIdVal = document.getElementById('editBudgetCategoryId').value;
        const walletIdVal = document.getElementById('editBudgetWalletId').value;

        const payload = {
            budgetName: document.getElementById('editBudgetName').value.trim(),
            budgetType: budgetType,
            allocationType: budgetType === 'RULE_50_30_20' ? document.getElementById('editBudgetAllocationType').value : null,
            categoryId: catIdVal ? parseInt(catIdVal, 10) : null,
            walletId: walletIdVal ? parseInt(walletIdVal, 10) : null,
            allocatedAmount: parseFloat(document.getElementById('editBudgetAllocatedAmount').value),
            startDate: document.getElementById('editBudgetStartDate').value,
            endDate: document.getElementById('editBudgetEndDate').value
        };

        fetch('/api/budgets/' + id, {
            method: 'PUT',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Cập Nhật Ngân Sách', 'Đã lưu thay đổi ngân sách [' + payload.budgetName + ']!', 'success');
                    } else {
                        alert('Cập nhật ngân sách thành công!');
                    }
                    window.closeModal('editBudgetModal');
                    setTimeout(() => window.location.reload(), 900);
                } else {
                    alert('Lỗi cập nhật ngân sách: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                console.error('Lỗi cập nhật ngân sách:', err);
                alert('Đã xảy ra lỗi khi cập nhật ngân sách.');
            });
    });
}

/**
 * Mở Modal chỉnh sửa Mục tiêu tiết kiệm (Edit Saving Goal)
 */
window.openEditSavingGoalModal = function (btn) {
    if (!btn) return;
    document.getElementById('editGoalId').value = btn.getAttribute('data-id') || '';
    document.getElementById('editGoalName').value = btn.getAttribute('data-name') || '';
    document.getElementById('editGoalTargetAmount').value = btn.getAttribute('data-target') || '';
    document.getElementById('editGoalTargetDate').value = btn.getAttribute('data-date') || '';
    document.getElementById('editGoalNotes').value = btn.getAttribute('data-notes') || '';
    window.openModal('editSavingGoalModal');
};

function setupEditSavingGoalForm() {
    const form = document.getElementById('editSavingGoalForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const id = document.getElementById('editGoalId').value;
        if (!id) return;

        const payload = {
            goalName: document.getElementById('editGoalName').value.trim(),
            targetAmount: parseFloat(document.getElementById('editGoalTargetAmount').value),
            targetDate: document.getElementById('editGoalTargetDate').value,
            notes: document.getElementById('editGoalNotes').value.trim() || null
        };

        fetch('/api/saving-goals/' + id, {
            method: 'PUT',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Cập Nhật Mục Tiêu', 'Đã lưu thay đổi mục tiêu [' + payload.goalName + ']!', 'success');
                    } else {
                        alert('Cập nhật mục tiêu tiết kiệm thành công!');
                    }
                    window.closeModal('editSavingGoalModal');
                    setTimeout(() => window.location.reload(), 900);
                } else {
                    alert('Lỗi cập nhật mục tiêu: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                console.error('Lỗi cập nhật mục tiêu tiết kiệm:', err);
                alert('Đã xảy ra lỗi khi cập nhật mục tiêu tiết kiệm.');
            });
    });
}

/**
 * Mở Modal Rút Tiền từ Mục tiêu tiết kiệm về Ví
 */
window.openWithdrawSavingGoalModal = function (btn) {
    if (!btn) return;
    const goalId = btn.getAttribute('data-goal-id') || '';
    const goalName = btn.getAttribute('data-goal-name') || 'Quỹ Mục Tiêu';
    const currentAmount = parseFloat(btn.getAttribute('data-current-amount') || '0') || 0;

    document.getElementById('withdrawGoalId').value = goalId;
    const nameEl = document.getElementById('withdrawGoalNameText');
    if (nameEl) nameEl.textContent = goalName;
    const availEl = document.getElementById('withdrawGoalAvailableText');
    if (availEl) availEl.textContent = window.formatVnd ? window.formatVnd(currentAmount) : (currentAmount + ' VND');

    const amountInput = document.getElementById('withdrawAmount');
    if (amountInput) {
        amountInput.value = '';
        if (currentAmount > 0) amountInput.max = currentAmount;
    }

    window.openModal('withdrawSavingGoalModal');
};

function setupWithdrawSavingGoalForm() {
    const form = document.getElementById('withdrawSavingGoalForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const goalId = document.getElementById('withdrawGoalId').value;
        const targetWalletId = document.getElementById('withdrawTargetWalletId').value;
        const amount = parseFloat(document.getElementById('withdrawAmount').value);

        if (!goalId || !targetWalletId || !amount || amount < 10000) {
            alert('Vui lòng nhập số tiền rút hợp lệ (tối thiểu 10,000 VND) và chọn ví nhận tiền.');
            return;
        }

        const url = `/api/saving-goals/${encodeURIComponent(goalId)}/withdraw?amount=${encodeURIComponent(amount)}&targetWalletId=${encodeURIComponent(targetWalletId)}`;
        fetch(url, {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify({ amount: amount, targetWalletId: parseInt(targetWalletId, 10) })
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Rút Tiền Thành Công', 'Đã hoàn tiền từ mục tiêu tiết kiệm về ví tài chính!', 'success');
                    } else {
                        alert('Rút tiền từ quỹ mục tiêu thành công!');
                    }
                    window.closeModal('withdrawSavingGoalModal');
                    setTimeout(() => window.location.reload(), 900);
                } else {
                    alert('Lỗi rút tiền: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                console.error('Lỗi rút tiền quỹ tiết kiệm:', err);
                alert('Đã xảy ra lỗi khi rút tiền từ quỹ mục tiêu.');
            });
    });
}

/**
 * Xóa Danh mục tùy chỉnh (Delete Custom Category)
 */
window.deleteCategory = async function (btn) {
    if (!btn) return;
    const id = btn.getAttribute('data-id');
    const name = btn.getAttribute('data-name') || ('#' + id);
    if (!id) return;

    if (!confirm('Bạn có chắc chắn muốn xóa danh mục [' + name + '] không?')) return;

    try {
        const res = await fetch('/api/categories/' + id, {
            method: 'DELETE',
            headers: window.getCsrfHeaders()
        });
        const result = await res.json();
        if (result.success) {
            if (window.FinVaultWS) {
                window.FinVaultWS.showToast('Đã Xóa Danh Mục', 'Đã xóa danh mục [' + name + '] thành công.', 'info');
            }
            setTimeout(() => window.location.reload(), 800);
        } else {
            alert('Không thể xóa danh mục: ' + (result.message || 'Danh mục mặc định hoặc đang được sử dụng'));
        }
    } catch (err) {
        console.error('Lỗi xóa danh mục:', err);
        alert('Lỗi kết nối khi xóa danh mục.');
    }
};

/**
 * Xóa bản ghi Lịch sử tính toán khoản vay (Delete Loan Calculation Record)
 */
window.deleteLoanRecord = async function (btn) {
    if (!btn) return;
    const id = btn.getAttribute('data-id');
    if (!id) return;

    if (!confirm('Bạn có chắc chắn muốn xóa bản ghi phương án vay #' + id + ' khỏi lịch sử?')) return;

    try {
        const res = await fetch('/api/loans/' + id, {
            method: 'DELETE',
            headers: window.getCsrfHeaders()
        });
        const result = await res.json();
        if (result.success) {
            const row = document.getElementById('loan-history-row-' + id);
            if (row) row.remove();
            if (window.FinVaultWS) {
                window.FinVaultWS.showToast('Đã Xóa Khoản Vay', 'Đã xóa phương án vay khỏi lịch sử lưu trữ.', 'info');
            }
        } else {
            alert('Lỗi xóa bản ghi khoản vay: ' + (result.message || 'Thất bại'));
        }
    } catch (err) {
        console.error('Lỗi xóa bản ghi khoản vay:', err);
        alert('Lỗi kết nối khi xóa bản ghi khoản vay.');
    }
};

/**
 * Đổi Mật Khẩu Người Dùng (Change Password)
 */
function setupChangePasswordForm() {
    const form = document.getElementById('changePasswordForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const currentPassword = document.getElementById('currentPasswordInput').value;
        const newPassword = document.getElementById('newPasswordInput').value;
        const confirmNewPassword = document.getElementById('confirmNewPasswordInput').value;

        if (newPassword !== confirmNewPassword) {
            alert('Mật khẩu mới và xác nhận mật khẩu mới không khớp!');
            return;
        }

        fetch('/api/users/change-password', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword
            })
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Đổi Mật Khẩu Thành Công', 'Mật khẩu bảo mật của bạn đã được cập nhật!', 'success');
                    } else {
                        alert('Đổi mật khẩu thành công!');
                    }
                    form.reset();
                    window.closeModal('changePasswordModal');
                } else {
                    alert('Đổi mật khẩu thất bại: ' + (result.message || 'Mật khẩu hiện tại không đúng'));
                }
            })
            .catch(err => {
                console.error('Lỗi đổi mật khẩu:', err);
                alert('Đã xảy ra lỗi kết nối khi đổi mật khẩu.');
            });
    });
}

/**
 * Quản trị viên: Khóa / Mở khóa trạng thái hoạt động tài khoản người dùng
 */
window.toggleUserActiveStatus = async function (userId, active) {
    if (!userId) return;
    const actionLabel = active ? 'kích hoạt lại' : 'vô hiệu hóa';
    if (!confirm(`Bạn có chắc chắn muốn ${actionLabel} tài khoản người dùng #${userId}?`)) return;

    try {
        const res = await fetch(`/api/users/${encodeURIComponent(userId)}/status?active=${encodeURIComponent(active)}`, {
            method: 'PUT',
            headers: window.getCsrfHeaders()
        });
        const result = await res.json();
        if (result.success) {
            if (window.FinVaultWS) {
                window.FinVaultWS.showToast('Quản Trị Tài Khoản', result.message || 'Đã cập nhật trạng thái tài khoản!', 'success');
            }
            setTimeout(() => window.location.reload(), 800);
        } else {
            alert('Lỗi cập nhật trạng thái tài khoản: ' + (result.message || 'Thất bại'));
        }
    } catch (err) {
        console.error('Lỗi cập nhật trạng thái user:', err);
        alert('Lỗi kết nối khi cập nhật trạng thái tài khoản.');
    }
};

/**
 * Quản trị viên: Mở khóa tài khoản bị khóa do Brute-Force
 */
window.unlockLockedUser = async function (userId) {
    if (!userId) return;
    try {
        const res = await fetch(`/api/users/${encodeURIComponent(userId)}/unlock`, {
            method: 'PUT',
            headers: window.getCsrfHeaders()
        });
        const result = await res.json();
        if (result.success) {
            if (window.FinVaultWS) {
                window.FinVaultWS.showToast('Mở Khóa Tài Khoản', 'Đã mở khóa Brute-Force cho người dùng #' + userId + '!', 'success');
            }
            setTimeout(() => window.location.reload(), 800);
        } else {
            alert('Lỗi mở khóa tài khoản: ' + (result.message || 'Thất bại'));
        }
    } catch (err) {
        console.error('Lỗi mở khóa user:', err);
        alert('Lỗi kết nối khi mở khóa tài khoản.');
    }
};

/**
 * Quản trị viên: Phát Thông Báo Hệ Thống Toàn Cục (Create System Notification)
 */
function setupCreateNotificationForm() {
    const form = document.getElementById('createNotificationForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const payload = {
            title: document.getElementById('notifTitle').value.trim(),
            content: document.getElementById('notifContent').value.trim(),
            notificationType: document.getElementById('notifType').value || 'INFO',
            broadcastToRoles: document.getElementById('notifBroadcastRoles').value.trim() || 'ALL',
            isActive: document.getElementById('notifIsActive').checked
        };

        fetch('/api/admin/cms/notifications', {
            method: 'POST',
            headers: Object.assign({ 'Content-Type': 'application/json' }, window.getCsrfHeaders()),
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(result => {
                if (result.success) {
                    if (window.FinVaultWS) {
                        window.FinVaultWS.showToast('Thông Báo Hệ Thống', 'Đã phát thông báo [' + payload.title + '] toàn hệ thống!', 'success');
                    } else {
                        alert('Đã tạo thông báo hệ thống thành công!');
                    }
                    window.closeModal('createNotificationModal');
                    setTimeout(() => window.location.reload(), 900);
                } else {
                    alert('Lỗi tạo thông báo: ' + (result.message || 'Thất bại'));
                }
            })
            .catch(err => {
                console.error('Lỗi tạo thông báo hệ thống:', err);
                alert('Đã xảy ra lỗi khi phát thông báo hệ thống.');
            });
    });
}


