/**
 * ==============================================================================
 * FinVault Enterprise - WebSocket Alert Client (STOMP over SockJS)
 * Nhận cảnh báo ngân sách thời gian thực & Cập nhật trực tiếp Live UI không cần reload
 * ==============================================================================
 */

(function () {
    'use strict';

    let stompClient = null;
    let reconnectTimeout = null;

    function initWebSocket() {
        const metaEl = document.getElementById('finvault-user-meta');
        if (!metaEl) {
            console.warn('[FinVault WS] Không tìm thấy metadata người dùng, hoãn kết nối WebSocket.');
            return;
        }

        const username = metaEl.getAttribute('data-username');
        const isSuperAdmin = metaEl.getAttribute('data-is-admin') === 'true';
        const isSystemAuditor = metaEl.getAttribute('data-is-auditor') === 'true';

        if (!username || username === 'anonymousUser' || username === 'SYSTEM') {
            return;
        }

        console.log('[FinVault WS] Đang khởi tạo kết nối STOMP WebSocket cho user:', username);

        try {
            const socket = new SockJS('/ws');
            stompClient = Stomp.over(socket);
            stompClient.debug = null; // Tắt verbose debug logs

            stompClient.connect({}, function (frame) {
                console.log('[FinVault WS] Đã kết nối STOMP WebSocket thành công! Frame:', frame);
                updateWsStatusBadge(true);

                // 1. Đăng ký kênh cảnh báo ngân sách cá nhân an toàn (User Destination Queue)
                stompClient.subscribe('/user/queue/budget-alerts', function (message) {
                    try {
                        const alertData = JSON.parse(message.body);
                        console.log('[FinVault WS] Nhận cảnh báo ngân sách qua /user/queue/budget-alerts:', alertData);
                        handleIncomingBudgetAlert(alertData);
                    } catch (err) {
                        console.error('[FinVault WS] Lỗi phân giải nội dung cảnh báo:', err);
                    }
                });

                // Fallback đăng ký topic cũ nếu có
                if (username) {
                    stompClient.subscribe('/topic/budget-alerts/' + username, function (message) {
                        try {
                            const alertData = JSON.parse(message.body);
                            handleIncomingBudgetAlert(alertData);
                        } catch (err) {
                            console.error('[FinVault WS] Lỗi phân giải nội dung cảnh báo:', err);
                        }
                    });
                }

                // 2. Đăng ký kênh thông báo mục tiêu tiết kiệm (Saving Goals Progress Queue)
                stompClient.subscribe('/user/queue/saving-goals', function (message) {
                    try {
                        const goalAlert = JSON.parse(message.body);
                        console.log('[FinVault WS] Nhận thông báo tiến độ tiết kiệm qua /user/queue/saving-goals:', goalAlert);
                        showToastNotification(
                            goalAlert.title || '🎯 Nhắc Nhở Mục Tiêu Tiết Kiệm',
                            goalAlert.message || goalAlert.alertMessage || ('Cập nhật tiến độ quỹ mục tiêu: ' + (goalAlert.goalName || '')),
                            'info',
                            'bi-piggy-bank-fill'
                        );
                    } catch (err) {
                        console.error('[FinVault WS] Lỗi phân giải thông báo tiết kiệm:', err);
                    }
                });

                // 3. Nếu là Super Admin hoặc System Auditor, đăng ký thêm kênh giám sát toàn hệ thống
                if (isSuperAdmin || isSystemAuditor) {
                    stompClient.subscribe('/topic/admin/budget-alerts', function (message) {
                        try {
                            const adminAlert = JSON.parse(message.body);
                            const adminPct = Number(adminAlert.usagePercentage ?? adminAlert.percentage ?? 0);
                            console.log('[FinVault WS] Nhận cảnh báo ngân sách toàn hệ thống (Admin Channel):', adminAlert);
                            showToastNotification(
                                'Cảnh Báo Quản Trị Hệ Thống',
                                adminAlert.alertMessage || ('User [' + (adminAlert.username || '') + '] đã đạt ' + adminPct.toFixed(1) + '% ngân sách ' + (adminAlert.budgetName || '')),
                                'warning'
                            );
                        } catch (err) {
                            console.error('[FinVault WS] Lỗi xử lý admin alert:', err);
                        }
                    });
                }

            }, function (error) {
                console.warn('[FinVault WS] Mất kết nối STOMP Broker:', error);
                updateWsStatusBadge(false);
                // Tự động kết nối lại sau 5 giây
                clearTimeout(reconnectTimeout);
                reconnectTimeout = setTimeout(initWebSocket, 5000);
            });
        } catch (e) {
            console.error('[FinVault WS] Không thể thiết lập SockJS:', e);
            updateWsStatusBadge(false);
        }
    }

    function updateWsStatusBadge(connected) {
        const badge = document.getElementById('ws-connection-badge');
        if (badge) {
            if (connected) {
                badge.className = 'badge bg-success-subtle text-success border border-success-subtle d-inline-flex align-items-center';
                badge.innerHTML = '<span class="live-pulse-dot me-1"></span> Live STOMP: Online';
            } else {
                badge.className = 'badge bg-secondary-subtle text-secondary border border-secondary-subtle d-inline-flex align-items-center';
                badge.innerHTML = '<i class="bi bi-cloud-slash me-1"></i> Live STOMP: Reconnecting...';
            }
        }
    }

    function handleIncomingBudgetAlert(alertData) {
        const pct = Number(alertData.usagePercentage ?? alertData.percentage ?? 0);
        const severity = String(alertData.severity || 'WARNING_80');
        const isCritical = severity.toUpperCase().includes('CRITICAL') || pct >= 100;
        const toastType = isCritical ? 'danger' : 'warning';
        const icon = isCritical ? 'bi-exclamation-octagon-fill' : 'bi-exclamation-triangle-fill';

        const title = (isCritical ? '🚨 VỠ NGÂN SÁCH (100%+) ' : '⚠️ CẢNH BÁO CHI TIÊU (80%) ') + (alertData.budgetName || '');
        const message = alertData.alertMessage || ('Bạn đã chi tiêu ' + pct.toFixed(1) + '% định mức ngân sách.');

        // 1. Hiển thị Toast thông báo nổi trên màn hình
        showToastNotification(title, message, toastType, icon);

        // 2. Cập nhật trực tiếp thanh Progress Bar của ngân sách tương ứng trên giao diện (In-place live UI update)
        if (alertData.budgetId) {
            const progressBar = document.getElementById('budget-progress-' + alertData.budgetId);
            const percentLabel = document.getElementById('budget-percent-' + alertData.budgetId);
            const spentLabel = document.getElementById('budget-spent-' + alertData.budgetId);

            if (progressBar) {
                progressBar.style.width = Math.min(pct, 100) + '%';
                progressBar.className = 'progress-bar ' + (isCritical ? 'bg-danger' : 'bg-warning');
            }
            if (percentLabel) {
                percentLabel.textContent = pct.toFixed(1) + '%';
                percentLabel.className = isCritical ? 'badge bg-danger' : 'badge bg-warning text-dark';
            }
            if (spentLabel && alertData.spentAmount != null) {
                spentLabel.textContent = new Intl.NumberFormat('vi-VN').format(alertData.spentAmount) + ' VND';
            }
        }
    }

    function showToastNotification(title, message, type, iconClass) {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const icon = iconClass || (type === 'danger' ? 'bi-exclamation-octagon-fill' : 'bi-info-circle-fill');
        const toastId = 'toast-' + Date.now();

        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-bg-${type} border-0 shadow-lg mb-2" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <div class="d-flex align-items-center mb-1">
                            <i class="bi ${icon} fs-5 me-2"></i>
                            <strong class="me-auto">${escapeHtml(title)}</strong>
                            <small class="text-white-50 ms-2">Vừa xong</small>
                        </div>
                        <div class="mt-1 small">${escapeHtml(message)}</div>
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;

        container.insertAdjacentHTML('beforeend', toastHtml);
        const toastEl = document.getElementById(toastId);
        if (toastEl && window.bootstrap) {
            const bsToast = new bootstrap.Toast(toastEl, { delay: 8000 });
            bsToast.show();
            toastEl.addEventListener('hidden.bs.toast', function () {
                toastEl.remove();
            });
        }
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, function (m) { return map[m]; });
    }

    // Xuất hàm toàn cục để có thể gọi thủ công nếu cần
    window.FinVaultWS = {
        init: initWebSocket,
        showToast: showToastNotification
    };

    // Tự động kích hoạt khi DOM tải xong
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initWebSocket);
    } else {
        initWebSocket();
    }
})();
