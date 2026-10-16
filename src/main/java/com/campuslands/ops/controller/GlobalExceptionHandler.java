package com.campuslands.ops.controller;

import com.campuslands.ops.service.AccesoNoAutorizadoException;
import com.campuslands.ops.service.ReglaNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "error", true,
                "tipo", "REGLA_NEGOCIO",
                "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(AccesoNoAutorizadoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", true,
                "tipo", "NO_AUTORIZADO",
                "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", true,
                "tipo", "EXCEPCION_CONTROLADA",
                "mensaje", ex.getMessage() != null ? ex.getMessage() : "No se pudo completar la operación. Revisa los datos e inténtalo de nuevo."
        ));
    }
}
