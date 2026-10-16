package com.campuslands.ops.security;

import com.google.gson.JsonObject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/api/") || path.startsWith("/api/auth/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            rechazar(response, "Debes iniciar sesión para usar el sistema operativo.");
            return;
        }

        String token = header.substring(7).trim();
        if (token.isEmpty() || "null".equalsIgnoreCase(token) || "undefined".equalsIgnoreCase(token)) {
            rechazar(response, "Sesión inválida. Inicia sesión de nuevo.");
            return;
        }

        try {
            JsonObject claims = jwtUtil.validateToken(token);
            Long userId = jwtUtil.getUserId(token);
            List<String> roles = jwtUtil.getRoles(token);
            request.setAttribute("jwtUserId", userId);
            request.setAttribute("jwtRoles", roles);
            request.setAttribute("jwtClaims", claims);
            chain.doFilter(request, response);
        } catch (Exception e) {
            rechazar(response, "Sesión expirada o token inválido. Inicia sesión de nuevo.");
        }
    }

    private void rechazar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        String json = "{\"error\":true,\"tipo\":\"NO_AUTORIZADO\",\"mensaje\":\""
                + mensaje.replace("\"", "'") + "\"}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
