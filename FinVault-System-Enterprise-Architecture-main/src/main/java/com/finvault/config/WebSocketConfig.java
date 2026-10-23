package com.finvault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

/**
 * ==============================================================================
 * [Owner: Architecture Core / Member 3 (WebSocket Alert Engine)]
 * WebSocketConfig: Cấu hình STOMP WebSocket Broker phục vụ đẩy cảnh báo ngân sách
 * và cập nhật giao diện Live UI theo thời gian thực (Real-time In-place UI Updates)
 *
 * Tính năng bảo mật nâng cao:
 * - Chống tấn công CSWSH (Cross-Site WebSocket Hijacking) bằng kiểm soát Origin chặt chẽ
 * - Kiểm tra Authentication & Authorization trước khi Subscribe qua ChannelInterceptor
 * ==============================================================================
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${finvault.websocket.allowed-origins:http://localhost:*,http://127.0.0.1:*,https://localhost:*,https://127.0.0.1:*}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt simple in-memory broker cho các kênh /topic (Broadcast) và /queue (User-specific)
        config.enableSimpleBroker("/topic", "/queue");
        // Tiền tố cho các message gửi từ Client lên Server
        config.setApplicationDestinationPrefixes("/app");
        // Tiền tố cho các thông báo nhắm cụ thể vào từng User
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký STOMP endpoint cho web socket client kết nối (Bảo vệ CSWSH bằng domain cụ thể)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Principal principal = accessor.getUser();
                    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
                        throw new AccessDeniedException("Yêu cầu đăng nhập trước khi đăng ký nhận dữ liệu qua WebSocket");
                    }
                    String destination = accessor.getDestination();
                    // Ngăn chặn nghe lén thông báo cá nhân của người dùng khác
                    if (destination != null && destination.startsWith("/topic/budget-alerts/")) {
                        String targetUser = destination.substring("/topic/budget-alerts/".length());
                        if (!principal.getName().equalsIgnoreCase(targetUser)) {
                            throw new AccessDeniedException("Bạn không có quyền đăng ký nhận cảnh báo của người dùng khác");
                        }
                    }
                }
                return message;
            }
        });
    }
}
