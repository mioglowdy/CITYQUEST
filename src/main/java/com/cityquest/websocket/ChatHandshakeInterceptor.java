package com.cityquest.websocket;

import com.cityquest.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private static final String USER_ID_ATTR = "userId";

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            return false;
        }

        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }

        attributes.put(USER_ID_ATTR, Long.parseLong(userId));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        // 1. 查询参数 token
        URI uri = request.getURI();
        if (uri.getQuery() != null) {
            String[] pairs = uri.getQuery().split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2 && "token".equalsIgnoreCase(kv[0])) {
                    return kv[1];
                }
            }
        }

        // 2. Header Authorization: Bearer
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null) {
            for (String header : authHeaders) {
                if (header != null && header.startsWith("Bearer ")) {
                    return header.substring(7);
                }
            }
        }

        // 3. Header token
        List<String> tokenHeaders = request.getHeaders().get("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }
}

