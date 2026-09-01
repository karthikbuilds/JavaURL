package com.karthik.JavaURL.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket setup used to push live click analytics.
 *
 * Clients connect to {@code /ws} (SockJS fallback enabled) and subscribe to
 * {@code /topic/clicks/{shortCode}} to receive a broadcast every time that link is visited.
 *
 * Uses the in-process simple broker by default; set {@code app.broker.type=external}
 * to route messages through a shared STOMP broker (e.g. RabbitMQ/ActiveMQ) so that
 * analytics broadcasts work across multiple backend instances.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT_PATH = "/ws";
    public static final String TOPIC_PREFIX = "/topic/clicks/";

    private final AppProperties properties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT_PATH)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if ("external".equalsIgnoreCase(properties.broker().type())) {
            AppProperties.Broker broker = properties.broker();
            registry.enableStompBrokerRelay("/topic")
                    .setRelayHost(broker.host())
                    .setRelayPort(broker.port());
        } else {
            registry.enableSimpleBroker("/topic");
        }
        registry.setApplicationDestinationPrefixes("/app");
    }
}