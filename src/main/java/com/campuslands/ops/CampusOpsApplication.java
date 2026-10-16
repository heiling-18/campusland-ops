package com.campuslands.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class CampusOpsApplication {

    public static void main(String[] args) {
        cargarDotEnvSinPisarVariablesDelSistema();
        SpringApplication.run(CampusOpsApplication.class, args);
    }

    /**
     * Carga .env a system properties ANTES de que Spring resuelva ${JWT_SECRET} y similares.
     * No pisa variables ya definidas en el sistema. El archivo .env no se versiona.
     */
    private static void cargarDotEnvSinPisarVariablesDelSistema() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile.toPath());
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if (key.isEmpty() || value.isEmpty()) {
                    continue;
                }
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (Exception ignored) {
            // Sin .env el sistema sigue con variables de entorno o valores por defecto locales.
        }
    }
}
