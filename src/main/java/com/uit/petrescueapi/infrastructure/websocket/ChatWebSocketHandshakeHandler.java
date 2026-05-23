package com.uit.petrescueapi.infrastructure.websocket;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.List;

public class ChatWebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
        if (requestedProtocols == null || requestedProtocols.isEmpty()) {
            return null;
        }
        return requestedProtocols.get(0);
    }
}