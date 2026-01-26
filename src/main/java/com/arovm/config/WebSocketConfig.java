package com.arovm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // This MUST match the URL in your JS: /terminal
        registry.addHandler(new TerminalHandler(), "/terminal").setAllowedOrigins("*");
    }
}