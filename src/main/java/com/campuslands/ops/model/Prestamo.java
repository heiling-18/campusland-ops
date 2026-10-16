package com.campuslands.ops.model;

import jakarta.persistence.*;

@Entity
@Table(name = "prestamos")
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long camperId;

    @Column(nullable = false)
    private String camperNombre;

    @Column(nullable = false)
    private Long implementoId;

    @Column(nullable = false)
    private String nombreEquipo;

    @Column(nullable = false)
    private String fechaHoraPrestamo;

    private String fechaHoraDevolucion;

    @Column(nullable = false)
    private String estado; // ACTIVO, DEVUELTO

    public Prestamo() {}

    public Prestamo(Long camperId, String camperNombre, Long implementoId, String nombreEquipo, String fechaHoraPrestamo) {
        this.camperId = camperId;
        this.camperNombre = camperNombre;
        this.implementoId = implementoId;
        this.nombreEquipo = nombreEquipo;
        this.fechaHoraPrestamo = fechaHoraPrestamo;
        this.estado = "ACTIVO";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCamperId() { return camperId; }
    public void setCamperId(Long camperId) { this.camperId = camperId; }
    public String getCamperNombre() { return camperNombre; }
    public void setCamperNombre(String camperNombre) { this.camperNombre = camperNombre; }
    public Long getImplementoId() { return implementoId; }
    public void setImplementoId(Long implementoId) { this.implementoId = implementoId; }
    public String getNombreEquipo() { return nombreEquipo; }
    public void setNombreEquipo(String nombreEquipo) { this.nombreEquipo = nombreEquipo; }
    public String getFechaHoraPrestamo() { return fechaHoraPrestamo; }
    public void setFechaHoraPrestamo(String fechaHoraPrestamo) { this.fechaHoraPrestamo = fechaHoraPrestamo; }
    public String getFechaHoraDevolucion() { return fechaHoraDevolucion; }
    public void setFechaHoraDevolucion(String fechaHoraDevolucion) { this.fechaHoraDevolucion = fechaHoraDevolucion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
