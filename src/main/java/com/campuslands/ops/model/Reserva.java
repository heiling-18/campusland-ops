package com.campuslands.ops.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long espacioId;

    @Column(nullable = false)
    private String espacioNombre;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String usuarioNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rolUsuario;

    @Column(nullable = false)
    private String fecha; // YYYY-MM-DD

    @Column(nullable = false)
    private String horaInicio; // HH:mm (ej. 08:00)

    @Column(nullable = false)
    private String horaFin; // HH:mm (ej. 10:00)

    @Column(nullable = false)
    private Integer cantidadAsistentes;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false)
    private String estado; // APROBADA, PENDIENTE, RECHAZADA

    @Column(length = 500)
    private String implementosIncluidos;

    public Reserva() {}

    public Reserva(Long espacioId, String espacioNombre, Long usuarioId, String usuarioNombre,
                   RolUsuario rolUsuario, String fecha, String horaInicio, String horaFin,
                   Integer cantidadAsistentes, String motivo, String estado, String implementosIncluidos) {
        this.espacioId = espacioId;
        this.espacioNombre = espacioNombre;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.rolUsuario = rolUsuario;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.cantidadAsistentes = cantidadAsistentes;
        this.motivo = motivo;
        this.estado = estado;
        this.implementosIncluidos = implementosIncluidos;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEspacioId() { return espacioId; }
    public void setEspacioId(Long espacioId) { this.espacioId = espacioId; }
    public String getEspacioNombre() { return espacioNombre; }
    public void setEspacioNombre(String espacioNombre) { this.espacioNombre = espacioNombre; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public RolUsuario getRolUsuario() { return rolUsuario; }
    public void setRolUsuario(RolUsuario rolUsuario) { this.rolUsuario = rolUsuario; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    public Integer getCantidadAsistentes() { return cantidadAsistentes; }
    public void setCantidadAsistentes(Integer cantidadAsistentes) { this.cantidadAsistentes = cantidadAsistentes; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getImplementosIncluidos() { return implementosIncluidos; }
    public void setImplementosIncluidos(String implementosIncluidos) { this.implementosIncluidos = implementosIncluidos; }
}
