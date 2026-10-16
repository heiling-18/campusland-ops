package com.campuslands.ops.security;

import org.springframework.context.annotation.Configuration;

/**
 * La API es stateless: JwtAuthFilter exige Bearer válido en /api/** (excepto /api/auth/login)
 * y RequestAuth toma usuario/rol del token, no del body del cliente.
 */
@Configuration
public class SecurityConfig {
}
