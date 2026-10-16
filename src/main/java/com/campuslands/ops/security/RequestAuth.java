package com.campuslands.ops.security;

import com.campuslands.ops.model.RolUsuario;
import com.campuslands.ops.service.AccesoNoAutorizadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestAuth {

    public Long userId(HttpServletRequest request) {
        Object value = request.getAttribute("jwtUserId");
        if (value instanceof Long id) {
            return id;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new AccesoNoAutorizadoException("No hay una sesión válida. Inicia sesión de nuevo.");
    }

    @SuppressWarnings("unchecked")
    public RolUsuario rol(HttpServletRequest request) {
        Object value = request.getAttribute("jwtRoles");
        if (value instanceof List<?> roles && !roles.isEmpty()) {
            return RolUsuario.valueOf(String.valueOf(roles.get(0)));
        }
        throw new AccesoNoAutorizadoException("El token no incluye un rol operativo válido.");
    }
}
