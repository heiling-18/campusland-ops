package com.campuslands.ops.model;

import jakarta.persistence.*;

@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportadoPorNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rolReportante;

    @Column(nullable = false)
    private String espacioNombre;

    @Column(nullable = false, length = 800)
    private String descripcion;

    @Column(nullable = false)
    private String categoriaIa;

    @Column(nullable = false)
    private String prioridadIa; // CRITICA, MEDIA, BAJA

    @Column(length = 1000)
    private String recomendacionIa;

    @Column(nullable = false)
    private String estado; // ABIERTA, RESUELTA

    private boolean bloqueoSalonAplicado;

    /** MODELO_IA o MOTOR_REGLAS_LOCAL (respaldo si la API no responde). */
    @Column(length = 40)
    private String fuenteClasificacion;

    @Column(nullable = false)
    private String fechaHora;

    public Incidencia() {}

    public Incidencia(String reportadoPorNombre, RolUsuario rolReportante, String espacioNombre,
                      String descripcion, String categoriaIa, String prioridadIa,
                      String recomendacionIa, boolean bloqueoSalonAplicado, String fechaHora) {
        this(reportadoPorNombre, rolReportante, espacioNombre, descripcion, categoriaIa, prioridadIa,
                recomendacionIa, bloqueoSalonAplicado, fechaHora, "MODELO_IA");
    }

    public Incidencia(String reportadoPorNombre, RolUsuario rolReportante, String espacioNombre,
                      String descripcion, String categoriaIa, String prioridadIa,
                      String recomendacionIa, boolean bloqueoSalonAplicado, String fechaHora,
                      String fuenteClasificacion) {
        this.reportadoPorNombre = reportadoPorNombre;
        this.rolReportante = rolReportante;
        this.espacioNombre = espacioNombre;
        this.descripcion = descripcion;
        this.categoriaIa = categoriaIa;
        this.prioridadIa = prioridadIa;
        this.recomendacionIa = recomendacionIa;
        this.estado = "ABIERTA";
        this.bloqueoSalonAplicado = bloqueoSalonAplicado;
        this.fechaHora = fechaHora;
        this.fuenteClasificacion = fuenteClasificacion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportadoPorNombre() { return reportadoPorNombre; }
    public void setReportadoPorNombre(String reportadoPorNombre) { this.reportadoPorNombre = reportadoPorNombre; }
    public RolUsuario getRolReportante() { return rolReportante; }
    public void setRolReportante(RolUsuario rolReportante) { this.rolReportante = rolReportante; }
    public String getEspacioNombre() { return espacioNombre; }
    public void setEspacioNombre(String espacioNombre) { this.espacioNombre = espacioNombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoriaIa() { return categoriaIa; }
    public void setCategoriaIa(String categoriaIa) { this.categoriaIa = categoriaIa; }
    public String getPrioridadIa() { return prioridadIa; }
    public void setPrioridadIa(String prioridadIa) { this.prioridadIa = prioridadIa; }
    public String getRecomendacionIa() { return recomendacionIa; }
    public void setRecomendacionIa(String recomendacionIa) { this.recomendacionIa = recomendacionIa; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isBloqueoSalonAplicado() { return bloqueoSalonAplicado; }
    public void setBloqueoSalonAplicado(boolean bloqueoSalonAplicado) { this.bloqueoSalonAplicado = bloqueoSalonAplicado; }
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
    public String getFuenteClasificacion() { return fuenteClasificacion; }
    public void setFuenteClasificacion(String fuenteClasificacion) { this.fuenteClasificacion = fuenteClasificacion; }
}
