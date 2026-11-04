package com.uit.petrescueapi.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP endpoint {@code /ws} (SockJS fallback) with an in-memory simple broker on
 * {@code /topic} and {@code /queue}.
 *
 * <p>No server code publishes to this broker yet (there is no {@code SimpMessagingTemplate}
 * user): like/comment counters are updated by the RabbitMQ listeners only. Realtime chat does
 * not use STOMP; it runs on the raw WebSocket endpoint {@code /chat-ws}
 * ({@link com.uit.petrescueapi.infrastructure.websocket.ChatWebSocketConfig}).</p>
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
