package com.uit.petrescueapi.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time notifications.
 * 
 * Clients connect via STOMP protocol and subscribe to topics:
 * - /topic/post/{postId}/likes       - New like on a post
 * - /topic/post/{postId}/comments    - New comment on a post
 * - /topic/comment/{commentId}/likes - New like on a comment
 * 
 * Example client connection:
 * const socket = new SockJS('http://localhost:8080/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, () => {
 *   stompClient.subscribe('/topic/post/123/likes', (message) => {
 *     console.log('New like:', JSON.parse(message.body));
 *   });
 * });
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory broker for /topic and /queue destinations
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for client messages to server (if needed in future)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint for WebSocket connection
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Configure based on CORS requirements
                .withSockJS();  // SockJS fallback for browsers that don't support WebSocket
    }
}
