package com.campuslands.ops.model;

import jakarta.persistence.*;

@Entity
@Table(name = "implementos")
public class Implemento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long espacioId;

    private String espacioNombre;

    @Column(nullable = false)
    private String tipo; // PC_ESTACION, TELEVISOR_HDMI, LABORATORIO, PRESTAMO_CAMPER

    private Integer puestoNumero;

    private String serialMonitor;
    private String serialMouse;
    private String serialTorre;
    private String serialTeclado;

    @Column(nullable = false)
    private String nombreImplemento;

    @Column(nullable = false)
    private String estado; // DISPONIBLE, NO_DISPONIBLE

    private boolean prestadoActualmente;

    private String motivoInhabilitado;

    public Implemento() {}

    public Implemento(Long espacioId, String espacioNombre, String tipo, Integer puestoNumero,
                      String serialMonitor, String serialMouse, String serialTorre, String serialTeclado,
                      String nombreImplemento, String estado, String motivoInhabilitado) {
        this.espacioId = espacioId;
        this.espacioNombre = espacioNombre;
        this.tipo = tipo;
        this.puestoNumero = puestoNumero;
        this.serialMonitor = serialMonitor;
        this.serialMouse = serialMouse;
        this.serialTorre = serialTorre;
        this.serialTeclado = serialTeclado;
        this.nombreImplemento = nombreImplemento;
        this.estado = estado;
        this.motivoInhabilitado = motivoInhabilitado;
        this.prestadoActualmente = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEspacioId() { return espacioId; }
    public void setEspacioId(Long espacioId) { this.espacioId = espacioId; }
    public String getEspacioNombre() { return espacioNombre; }
    public void setEspacioNombre(String espacioNombre) { this.espacioNombre = espacioNombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getPuestoNumero() { return puestoNumero; }
    public void setPuestoNumero(Integer puestoNumero) { this.puestoNumero = puestoNumero; }
    public String getSerialMonitor() { return serialMonitor; }
    public void setSerialMonitor(String serialMonitor) { this.serialMonitor = serialMonitor; }
    public String getSerialMouse() { return serialMouse; }
    public void setSerialMouse(String serialMouse) { this.serialMouse = serialMouse; }
    public String getSerialTorre() { return serialTorre; }
    public void setSerialTorre(String serialTorre) { this.serialTorre = serialTorre; }
    public String getSerialTeclado() { return serialTeclado; }
    public void setSerialTeclado(String serialTeclado) { this.serialTeclado = serialTeclado; }
    public String getNombreImplemento() { return nombreImplemento; }
    public void setNombreImplemento(String nombreImplemento) { this.nombreImplemento = nombreImplemento; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isPrestadoActualmente() { return prestadoActualmente; }
    public void setPrestadoActualmente(boolean prestadoActualmente) { this.prestadoActualmente = prestadoActualmente; }
    public String getMotivoInhabilitado() { return motivoInhabilitado; }
    public void setMotivoInhabilitado(String motivoInhabilitado) { this.motivoInhabilitado = motivoInhabilitado; }
}
