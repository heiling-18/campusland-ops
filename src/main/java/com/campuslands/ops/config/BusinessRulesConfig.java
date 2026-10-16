package com.campuslands.ops.config;

/**
 * Único archivo que el jurado debe pedir cambiar en el reto de 3 minutos.
 * Ejemplo: PORCENTAJE_MINIMO_OCUPACION_TRAINER = 0.70  →  el Trainer necesita 70% de ocupación.
 */
public class BusinessRulesConfig {

    // Regla 1 (Trainer): Porcentaje minimo de ocupacion para evitar subutilizacion (50% = 0.50)
    public static final double PORCENTAJE_MINIMO_OCUPACION_TRAINER = 0.50;

    // Regla 2 (Camper): Capacidad maxima de las salas pequenas que puede ver y reservar un Camper (<= 12)
    public static final int CAPACIDAD_MAXIMA_SALAS_CAMPER = 12;

    // Regla 3 (Camper): Maximo de equipos tecnologicos prestados activos a la vez por Camper
    public static final int MAX_PRESTAMOS_ACTIVOS_CAMPER = 1;

    private BusinessRulesConfig() {}
}
