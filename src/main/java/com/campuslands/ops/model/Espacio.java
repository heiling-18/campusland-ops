package com.campuslands.ops.model;

import jakarta.persistence.*;

@Entity
@Table(name = "espacios")
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(nullable = false)
    private String categoria; // AUDITORIO, AULA_ENTRENAMIENTO, LABORATORIO, SALA_ESTUDIO

    @Column(nullable = false)
    private String estado; // DISPONIBLE, BLOQUEADO

    @Column(length = 500)
    private String resumenImplementos;

    @Column(length = 300)
    private String motivoBloqueo;

    /** IA = bloqueo automático por incidencia crítica; MANUAL = Admin/Soporte. Null si está disponible. */
    @Column(length = 20)
    private String origenBloqueo;

    public Espacio() {}

    public Espacio(String nombre, Integer capacidad, String categoria, String estado, String resumenImplementos) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.categoria = categoria;
        this.estado = estado;
        this.resumenImplementos = resumenImplementos;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getResumenImplementos() { return resumenImplementos; }
    public void setResumenImplementos(String resumenImplementos) { this.resumenImplementos = resumenImplementos; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public void setMotivoBloqueo(String motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; }
    public String getOrigenBloqueo() { return origenBloqueo; }
    public void setOrigenBloqueo(String origenBloqueo) { this.origenBloqueo = origenBloqueo; }
}
