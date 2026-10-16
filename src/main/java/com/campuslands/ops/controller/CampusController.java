package com.campuslands.ops.controller;

import com.campuslands.ops.model.*;
import com.campuslands.ops.repository.PrestamoRepository;
import com.campuslands.ops.repository.ReservaRepository;
import com.campuslands.ops.security.JwtUtil;
import com.campuslands.ops.security.RequestAuth;
import com.campuslands.ops.service.AiOperationalService;
import com.campuslands.ops.service.OperacionesService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CampusController {

    private final OperacionesService operacionesService;
    private final AiOperationalService aiService;
    private final ReservaRepository reservaRepository;
    private final PrestamoRepository prestamoRepository;
    private final JwtUtil jwtUtil;
    private final RequestAuth requestAuth;

    public CampusController(OperacionesService operacionesService,
                            AiOperationalService aiService,
                            ReservaRepository reservaRepository,
                            PrestamoRepository prestamoRepository,
                            JwtUtil jwtUtil,
                            RequestAuth requestAuth) {
        this.operacionesService = operacionesService;
        this.aiService = aiService;
        this.reservaRepository = reservaRepository;
        this.prestamoRepository = prestamoRepository;
        this.jwtUtil = jwtUtil;
        this.requestAuth = requestAuth;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        Usuario user = operacionesService.autenticar(body.get("correo"), body.get("password"));
        List<String> roles = List.of(user.getRol().name());

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getNombre(),
                roles
        );

        return Map.of(
                "token", token,
                "id", user.getId(),
                "userId", user.getId(),
                "nombre", user.getNombre(),
                "fullName", user.getNombre(),
                "correo", user.getCorreo(),
                "rol", user.getRol().name(),
                "roles", roles
        );
    }

    @GetMapping("/espacios")
    public List<Espacio> listarEspacios(HttpServletRequest request) {
        return operacionesService.obtenerEspaciosSegunRol(requestAuth.rol(request));
    }

    @PostMapping("/espacios")
    public Espacio guardarOEditarEspacio(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        RolUsuario rol = requestAuth.rol(request);
        Long id = body.get("id") != null && !body.get("id").toString().isBlank()
                ? Long.valueOf(body.get("id").toString()) : null;
        String nombre = (String) body.get("nombre");
        Integer capacidad = Integer.valueOf(body.get("capacidad").toString());
        String categoria = (String) body.get("categoria");
        String resumenImplementos = (String) body.get("resumenImplementos");
        return operacionesService.guardarOEditarEspacioAdmin(rol, id, nombre, capacidad, categoria, resumenImplementos);
    }

    @DeleteMapping("/espacios/{id}")
    public Map<String, Object> eliminarEspacio(HttpServletRequest request, @PathVariable Long id) {
        operacionesService.eliminarEspacioAdmin(requestAuth.rol(request), id);
        return Map.of("exito", true, "mensaje", "Salón eliminado correctamente por el Administrador.");
    }

    @PatchMapping("/espacios/{id}/estado")
    public Espacio cambiarEstadoEspacio(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return operacionesService.cambiarEstadoEspacioManual(
                requestAuth.rol(request), id, body.get("estado"), body.get("motivo"));
    }

    @GetMapping("/espacios/{id}/implementos")
    public List<Implemento> obtenerImplementosSalon(@PathVariable Long id) {
        return operacionesService.obtenerInventarioPorSalon(id);
    }

    @PatchMapping("/implementos/{id}/estado")
    public Implemento cambiarEstadoImplemento(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return operacionesService.cambiarEstadoImplementoSoporte(
                requestAuth.rol(request), id, body.get("estado"), body.get("motivo"));
    }

    @GetMapping("/reservas")
    public List<Reserva> listarReservas(HttpServletRequest request) {
        RolUsuario rol = requestAuth.rol(request);
        Long usuarioId = requestAuth.userId(request);
        if (rol == RolUsuario.ADMIN) {
            return reservaRepository.findAll();
        }
        return reservaRepository.findAll().stream()
                .filter(r -> r.getUsuarioId() != null && r.getUsuarioId().equals(usuarioId))
                .toList();
    }

    @PostMapping("/reservas")
    public Reserva crearReserva(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long usuarioId = requestAuth.userId(request);
        Long espacioId = Long.valueOf(body.get("espacioId").toString());
        String fecha = (String) body.get("fecha");
        String horaInicio = (String) body.get("horaInicio");
        String horaFin = (String) body.get("horaFin");
        int cantidadAsistentes = Integer.parseInt(body.get("cantidadAsistentes").toString());
        String motivo = (String) body.get("motivo");
        return operacionesService.crearReservaOSolicitud(usuarioId, espacioId, fecha, horaInicio, horaFin, cantidadAsistentes, motivo);
    }

    @PatchMapping("/reservas/{id}/aprobacion")
    public Reserva aprobarORechazarSolicitud(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean aprobar = Boolean.parseBoolean(body.get("aprobar").toString());
        return operacionesService.gestionarSolicitudTrainerPorAdmin(requestAuth.rol(request), id, aprobar);
    }

    @DeleteMapping("/reservas/{id}")
    public Map<String, Object> eliminarReserva(HttpServletRequest request, @PathVariable Long id) {
        operacionesService.eliminarReserva(requestAuth.userId(request), requestAuth.rol(request), id);
        return Map.of("exito", true, "mensaje", "Reserva o solicitud cancelada exitosamente.");
    }

    @PostMapping("/reservas/{id}/reubicar-ia")
    public Map<String, Object> reubicarReservaTrainerConIa(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String descripcionFalla = (String) body.get("descripcionFalla");
        return operacionesService.reubicarReservaConIa(requestAuth.userId(request), id, descripcionFalla);
    }

    @GetMapping("/prestamos/catalogo")
    public List<Implemento> catalogoPrestamosCamper() {
        return operacionesService.obtenerEquiposParaPrestamoCamper();
    }

    @GetMapping("/prestamos/mis-prestamos")
    public List<Prestamo> misPrestamos(HttpServletRequest request) {
        return prestamoRepository.findByCamperIdOrderByIdDesc(requestAuth.userId(request));
    }

    @PostMapping("/prestamos")
    public Prestamo solicitarPrestamo(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long implementoId = Long.valueOf(body.get("implementoId").toString());
        int cantidad = body.get("cantidad") != null ? Integer.parseInt(body.get("cantidad").toString()) : 1;
        return operacionesService.solicitarPrestamoCamper(requestAuth.userId(request), implementoId, cantidad);
    }

    @PostMapping("/prestamos/{id}/devolver")
    public Prestamo devolverPrestamo(HttpServletRequest request, @PathVariable Long id) {
        return operacionesService.devolverPrestamoCamper(requestAuth.userId(request), requestAuth.rol(request), id);
    }

    @GetMapping("/incidencias")
    public List<Incidencia> listarIncidencias() {
        return operacionesService.obtenerIncidenciasPriorizadasPorIa();
    }

    @PostMapping("/incidencias")
    public Incidencia reportarIncidencia(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String espacioNombre = (String) body.get("espacioNombre");
        String descripcion = (String) body.get("descripcion");
        return operacionesService.crearIncidenciaConTriajeIa(requestAuth.userId(request), espacioNombre, descripcion);
    }

    @PatchMapping("/incidencias/{id}/resolver")
    public Map<String, Object> resolverIncidencia(HttpServletRequest request, @PathVariable Long id) {
        return operacionesService.resolverIncidenciaSoporte(requestAuth.rol(request), id);
    }

    @PostMapping("/ia/recomendar-salon")
    public Map<String, Object> recomendarSalonConIa(@RequestBody Map<String, Object> body) {
        int asistentes = Integer.parseInt(body.get("cantidadAsistentes").toString());
        String fecha = (String) body.get("fecha");
        String horaInicio = (String) body.get("horaInicio");
        String horaFin = (String) body.get("horaFin");
        String consulta = (String) body.getOrDefault("consulta", "");
        List<Espacio> libres = operacionesService.obtenerEspaciosDisponiblesEnHorario(fecha, horaInicio, horaFin);
        return aiService.recomendarSalonOptimo(asistentes, fecha, horaInicio, horaFin, libres, consulta);
    }

    @PostMapping("/ia/consulta-general")
    public Map<String, String> consultaGeneralIa(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String pregunta = body.get("pregunta");
        String rol = requestAuth.rol(request).name();
        String resumen = operacionesService.construirResumenEstadoCampus();
        String respuesta = aiService.responderConsultaGeneral(pregunta, rol, resumen);
        return Map.of("respuesta", respuesta);
    }
}
