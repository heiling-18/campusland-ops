package com.campuslands.ops.service;

import com.campuslands.ops.config.BusinessRulesConfig;
import com.campuslands.ops.model.*;
import com.campuslands.ops.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OperacionesService {

    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final ImplementoRepository implementoRepository;
    private final ReservaRepository reservaRepository;
    private final PrestamoRepository prestamoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final AiOperationalService aiService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public OperacionesService(UsuarioRepository usuarioRepository,
                              EspacioRepository espacioRepository,
                              ImplementoRepository implementoRepository,
                              ReservaRepository reservaRepository,
                              PrestamoRepository prestamoRepository,
                              IncidenciaRepository incidenciaRepository,
                              AiOperationalService aiService) {
        this.usuarioRepository = usuarioRepository;
        this.espacioRepository = espacioRepository;
        this.implementoRepository = implementoRepository;
        this.reservaRepository = reservaRepository;
        this.prestamoRepository = prestamoRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.aiService = aiService;
    }

    public Usuario autenticar(String correo, String password) {
        if (correo == null || password == null) {
            throw new ReglaNegocioException("Debes ingresar correo institucional y contraseña.");
        }
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> new ReglaNegocioException("Credenciales inválidas: El correo '" + correo + "' no se encuentra registrado."));
        if (!usuario.getPassword().equals(password.trim())) {
            throw new ReglaNegocioException("Credenciales inválidas: La contraseña ingresada es incorrecta.");
        }
        return usuario;
    }

    public List<Espacio> obtenerEspaciosSegunRol(RolUsuario rol) {
        if (rol == RolUsuario.CAMPER) {
            return espacioRepository.findByCapacidadLessThanEqualOrderByCapacidadAsc(
                    BusinessRulesConfig.CAPACIDAD_MAXIMA_SALAS_CAMPER
            );
        }
        return espacioRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Espacio::getCapacidad).reversed())
                .toList();
    }

    @Transactional
    public Espacio guardarOEditarEspacioAdmin(RolUsuario rolSolicitante, Long id, String nombre, Integer capacidad,
                                              String categoria, String resumenImplementos) {
        if (rolSolicitante != RolUsuario.ADMIN) {
            throw new ReglaNegocioException("Acceso denegado: Solo el rol ADMINISTRADOR puede crear o modificar salones.");
        }
        if (nombre == null || nombre.isBlank() || capacidad == null || capacidad <= 0) {
            throw new ReglaNegocioException("El nombre del salón y una capacidad mayor a 0 son obligatorios.");
        }
        if (id != null) {
            Espacio existente = espacioRepository.findById(id)
                    .orElseThrow(() -> new ReglaNegocioException("Salón no encontrado."));
            existente.setNombre(nombre.trim());
            existente.setCapacidad(capacidad);
            if (categoria != null && !categoria.isBlank()) existente.setCategoria(categoria);
            if (resumenImplementos != null && !resumenImplementos.isBlank()) existente.setResumenImplementos(resumenImplementos);
            return espacioRepository.save(existente);
        } else {
            Espacio nuevo = new Espacio(
                    nombre.trim(),
                    capacidad,
                    (categoria != null && !categoria.isBlank()) ? categoria : "AULA_ENTRENAMIENTO",
                    "DISPONIBLE",
                    (resumenImplementos != null && !resumenImplementos.isBlank()) ? resumenImplementos : "1 Televisor con su Cable HDMI"
            );
            Espacio guardado = espacioRepository.save(nuevo);
            implementoRepository.save(new Implemento(guardado.getId(), guardado.getNombre(), "TELEVISOR_HDMI", 1,
                    "TV-NEW-" + guardado.getId(), "N/A", "N/A", "HDMI-NEW-" + guardado.getId(),
                    guardado.getResumenImplementos(), "DISPONIBLE", null));
            return guardado;
        }
    }

    @Transactional
    public void eliminarEspacioAdmin(RolUsuario rolSolicitante, Long espacioId) {
        if (rolSolicitante != RolUsuario.ADMIN) {
            throw new ReglaNegocioException("Acceso denegado: Solo el ADMINISTRADOR puede eliminar salones.");
        }
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ReglaNegocioException("Salón no encontrado."));
        long reservasActivas = reservaRepository.findByEspacioId(espacioId).stream()
                .filter(r -> !"RECHAZADA".equalsIgnoreCase(r.getEstado()))
                .count();
        if (reservasActivas > 0) {
            throw new ReglaNegocioException("Integridad de datos: No se puede eliminar '" + espacio.getNombre()
                    + "' porque tiene " + reservasActivas + " reserva(s) o solicitud(es) activa(s). Cancélalas o recházalas primero.");
        }
        implementoRepository.deleteAll(implementoRepository.findByEspacioIdOrderByPuestoNumeroAsc(espacioId));
        espacioRepository.deleteById(espacioId);
    }

    @Transactional
    public Espacio cambiarEstadoEspacioManual(RolUsuario rolSolicitante, Long espacioId, String nuevoEstado, String motivo) {
        if (rolSolicitante != RolUsuario.ADMIN && rolSolicitante != RolUsuario.SOPORTE) {
            throw new ReglaNegocioException("Acceso denegado: Solo ADMINISTRADOR y SOPORTE pueden alterar el bloqueo de salones.");
        }
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ReglaNegocioException("Salón no encontrado."));
        boolean bloquear = "BLOQUEADO".equalsIgnoreCase(nuevoEstado);
        espacio.setEstado(bloquear ? "BLOQUEADO" : "DISPONIBLE");
        espacio.setMotivoBloqueo(bloquear
                ? (motivo != null && !motivo.isBlank() ? motivo : "Inhabilitado manualmente por " + rolSolicitante)
                : null);
        espacio.setOrigenBloqueo(bloquear ? "MANUAL" : null);
        return espacioRepository.save(espacio);
    }

    @Transactional
    public Reserva crearReservaOSolicitud(Long usuarioId, Long espacioId, String fecha,
                                          String horaInicio, String horaFin, int cantidadAsistentes, String motivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no válido."));
        Espacio espacio = espacioRepository.findById(espacioId)
                .orElseThrow(() -> new ReglaNegocioException("El salón seleccionado no existe."));

        if (horaInicio == null || horaFin == null || horaInicio.compareTo(horaFin) >= 0) {
            throw new ReglaNegocioException("Horario inválido: La hora de inicio (" + horaInicio + ") debe ser anterior a la hora de finalización (" + horaFin + ").");
        }
        if (cantidadAsistentes <= 0) {
            throw new ReglaNegocioException("La cantidad de asistentes debe ser mayor a 0.");
        }

        RolUsuario rol = usuario.getRol();

        validarDisponibilidadYAforoFisico(espacio, cantidadAsistentes);

        if (rol == RolUsuario.CAMPER) {
            if (espacio.getCapacidad() > BusinessRulesConfig.CAPACIDAD_MAXIMA_SALAS_CAMPER) {
                throw new ReglaNegocioException("Restricción de Rol CAMPER: Solo puedes reservar salas de estudio de hasta "
                        + BusinessRulesConfig.CAPACIDAD_MAXIMA_SALAS_CAMPER + " personas.");
            }
        }

        if (rol == RolUsuario.TRAINER) {
            int minimoRequerido = (int) Math.ceil(espacio.getCapacidad() * BusinessRulesConfig.PORCENTAJE_MINIMO_OCUPACION_TRAINER);
            if (cantidadAsistentes < minimoRequerido) {
                List<Espacio> disponiblesEnHorario = obtenerEspaciosDisponiblesEnHorario(fecha, horaInicio, horaFin);
                List<Espacio> menoresDisponiblesDondeCabe = disponiblesEnHorario.stream()
                        .filter(e -> e.getCapacidad() < espacio.getCapacidad() && e.getCapacidad() >= cantidadAsistentes)
                        .toList();

                if (!menoresDisponiblesDondeCabe.isEmpty()) {
                    Espacio sugerido = menoresDisponiblesDondeCabe.stream()
                            .min(Comparator.comparingInt(Espacio::getCapacidad))
                            .get();
                    throw new ReglaNegocioException(String.format(
                            "Regla Anti-Subutilización (50%%): '%s' tiene capacidad para %d personas (mínimo requerido: %d). Para %d asistentes se encuentra disponible '%s' (Capacidad: %d) en ese mismo horario.",
                            espacio.getNombre(), espacio.getCapacidad(), minimoRequerido,
                            cantidadAsistentes, sugerido.getNombre(), sugerido.getCapacidad()
                    ));
                }
            }
        }

        validarNoSolapamiento(espacio, fecha, horaInicio, horaFin, null);

        String estadoInicial = (rol == RolUsuario.ADMIN) ? "APROBADA" : "PENDIENTE";

        long disponiblesSalon = implementoRepository.findByEspacioIdOrderByPuestoNumeroAsc(espacioId).stream()
                .filter(i -> "DISPONIBLE".equalsIgnoreCase(i.getEstado()))
                .count();
        String resumenImpl = espacio.getResumenImplementos() + " (" + disponiblesSalon + " unidades habilitadas)";

        Reserva nueva = new Reserva(
                espacio.getId(), espacio.getNombre(),
                usuario.getId(), usuario.getNombre(), rol,
                fecha, horaInicio, horaFin, cantidadAsistentes,
                (motivo != null && !motivo.isBlank()) ? motivo : "Sesión Académica / Estudio",
                estadoInicial,
                resumenImpl
        );
        return reservaRepository.save(nueva);
    }

    @Transactional
    public Reserva gestionarSolicitudTrainerPorAdmin(RolUsuario rolSolicitante, Long reservaId, boolean aprobar) {
        if (rolSolicitante != RolUsuario.ADMIN) {
            throw new ReglaNegocioException("Solo el ADMINISTRADOR puede aprobar o rechazar solicitudes de reserva.");
        }
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReglaNegocioException("Solicitud no encontrada."));
        if (aprobar) {
            Espacio espacio = espacioRepository.findById(reserva.getEspacioId())
                    .orElseThrow(() -> new ReglaNegocioException("El salón de la solicitud ya no existe."));
            validarDisponibilidadYAforoFisico(espacio, reserva.getCantidadAsistentes());
            validarNoSolapamiento(espacio, reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin(), reserva.getId());
            reserva.setEstado("APROBADA");
        } else {
            reserva.setEstado("RECHAZADA");
        }
        return reservaRepository.save(reserva);
    }

    @Transactional
    public void eliminarReserva(Long usuarioId, RolUsuario rol, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReglaNegocioException("La reserva o solicitud no existe."));
        if (rol != RolUsuario.ADMIN && !Objects.equals(reserva.getUsuarioId(), usuarioId)) {
            throw new ReglaNegocioException("Permiso denegado: Como " + rol + " solo puedes eliminar tus propias solicitudes.");
        }
        reservaRepository.deleteById(reservaId);
    }

    @Transactional
    public Map<String, Object> reubicarReservaConIa(Long usuarioId, Long reservaId, String descripcionFalla) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReglaNegocioException("Reserva no encontrada."));
        if (!Objects.equals(reserva.getUsuarioId(), usuarioId)) {
            throw new ReglaNegocioException("Solo puedes reubicar tus propias reservas activas.");
        }

        Usuario trainer = usuarioRepository.findById(usuarioId).orElseThrow();
        crearIncidenciaConTriajeIa(trainer.getId(), reserva.getEspacioNombre(), descripcionFalla);

        List<Espacio> libres = obtenerEspaciosDisponiblesEnHorario(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin())
                .stream()
                .filter(e -> !Objects.equals(e.getId(), reserva.getEspacioId()))
                .toList();

        Map<String, Object> recomendacion = aiService.recomendarSalonOptimo(
                reserva.getCantidadAsistentes(), reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin(),
                libres, descripcionFalla
        );

        if (!(Boolean) recomendacion.getOrDefault("exito", false)) {
            throw new ReglaNegocioException((String) recomendacion.get("mensaje"));
        }

        Long nuevoEspacioId = ((Number) recomendacion.get("espacioId")).longValue();
        Espacio nuevoEspacio = espacioRepository.findById(nuevoEspacioId).orElseThrow();
        String salonAnterior = reserva.getEspacioNombre();

        reserva.setEspacioId(nuevoEspacio.getId());
        reserva.setEspacioNombre(nuevoEspacio.getNombre());
        reserva.setImplementosIncluidos(nuevoEspacio.getResumenImplementos());
        reserva.setMotivo(reserva.getMotivo() + " [Reubicado por IA desde " + salonAnterior + "]");
        reservaRepository.save(reserva);

        return Map.of(
                "reservaActualizada", reserva,
                "salonAnterior", salonAnterior,
                "nuevoSalon", nuevoEspacio.getNombre(),
                "explicacionIa", recomendacion.get("mensaje")
        );
    }

    private void validarDisponibilidadYAforoFisico(Espacio espacio, int cantidadAsistentes) {
        if ("BLOQUEADO".equalsIgnoreCase(espacio.getEstado())) {
            throw new ReglaNegocioException("Regla de Integridad: El salón '" + espacio.getNombre()
                    + "' se encuentra BLOQUEADO (" + (espacio.getMotivoBloqueo() != null ? espacio.getMotivoBloqueo() : "Incidencia crítica activa") + ").");
        }
        if (cantidadAsistentes > espacio.getCapacidad()) {
            throw new ReglaNegocioException("Regla de Aforo Máximo: Solicitas espacio para " + cantidadAsistentes
                    + " personas, superando la capacidad máxima física de '" + espacio.getNombre() + "' (" + espacio.getCapacidad() + " personas).");
        }
    }

    private void validarNoSolapamiento(Espacio espacio, String fecha, String horaInicio, String horaFin, Long reservaIdExcluida) {
        List<Reserva> reservasMismoDia = reservaRepository.findByEspacioIdAndFechaAndEstadoNot(espacio.getId(), fecha, "RECHAZADA");
        for (Reserva r : reservasMismoDia) {
            if (reservaIdExcluida != null && Objects.equals(r.getId(), reservaIdExcluida)) {
                continue;
            }
            if (seSolapanHorarios(horaInicio, horaFin, r.getHoraInicio(), r.getHoraFin())) {
                throw new ReglaNegocioException(String.format(
                        "Conflicto de Horario: El salón '%s' ya cuenta con una reserva/solicitud activa de %s a %s registrada por %s (%s).",
                        espacio.getNombre(), r.getHoraInicio(), r.getHoraFin(), r.getUsuarioNombre(), r.getEstado()
                ));
            }
        }
    }

    public List<Espacio> obtenerEspaciosDisponiblesEnHorario(String fecha, String horaInicio, String horaFin) {
        List<Espacio> todosDisponibles = espacioRepository.findAll().stream()
                .filter(e -> "DISPONIBLE".equalsIgnoreCase(e.getEstado()))
                .toList();

        List<Espacio> resultado = new ArrayList<>();
        for (Espacio e : todosDisponibles) {
            List<Reserva> reservas = reservaRepository.findByEspacioIdAndFechaAndEstadoNot(e.getId(), fecha, "RECHAZADA");
            boolean ocupado = reservas.stream()
                    .anyMatch(r -> seSolapanHorarios(horaInicio, horaFin, r.getHoraInicio(), r.getHoraFin()));
            if (!ocupado) {
                resultado.add(e);
            }
        }
        return resultado;
    }

    private boolean seSolapanHorarios(String inicio1, String fin1, String inicio2, String fin2) {
        return inicio1.compareTo(fin2) < 0 && inicio2.compareTo(fin1) < 0;
    }

    public List<Implemento> obtenerInventarioPorSalon(Long espacioId) {
        return implementoRepository.findByEspacioIdOrderByPuestoNumeroAsc(espacioId);
    }

    public List<Implemento> obtenerEquiposParaPrestamoCamper() {
        return implementoRepository.findByTipoOrderByIdAsc("PRESTAMO_CAMPER");
    }

    @Transactional
    public Implemento cambiarEstadoImplementoSoporte(RolUsuario rol, Long implementoId, String nuevoEstado, String motivo) {
        if (rol != RolUsuario.SOPORTE && rol != RolUsuario.ADMIN) {
            throw new ReglaNegocioException("Solo SOPORTE o ADMIN pueden cambiar el estado técnico del inventario.");
        }
        if (!"DISPONIBLE".equalsIgnoreCase(nuevoEstado) && !"NO_DISPONIBLE".equalsIgnoreCase(nuevoEstado)) {
            throw new ReglaNegocioException("Estado inválido: Soporte solo puede alternar entre DISPONIBLE y NO_DISPONIBLE.");
        }
        Implemento item = implementoRepository.findById(implementoId)
                .orElseThrow(() -> new ReglaNegocioException("Implemento no encontrado."));
        item.setEstado(nuevoEstado.toUpperCase());
        item.setMotivoInhabilitado("NO_DISPONIBLE".equalsIgnoreCase(nuevoEstado)
                ? (motivo != null && !motivo.isBlank() ? motivo : "Inhabilitado por Soporte Técnico")
                : null);
        return implementoRepository.save(item);
    }

    @Transactional
    public Prestamo solicitarPrestamoCamper(Long camperId, Long implementoId, int cantidadSolicitada) {
        Usuario camper = usuarioRepository.findById(camperId)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado."));
        if (camper.getRol() != RolUsuario.CAMPER) {
            throw new ReglaNegocioException("El módulo de préstamos individuales corresponde exclusivamente al rol CAMPER.");
        }
        if (cantidadSolicitada > BusinessRulesConfig.MAX_PRESTAMOS_ACTIVOS_CAMPER) {
            throw new ReglaNegocioException("Regla de Negocio (Bloqueo): No se permiten préstamos por lotes. Un CAMPER solo puede solicitar máximo "
                    + BusinessRulesConfig.MAX_PRESTAMOS_ACTIVOS_CAMPER + " equipo tecnológico activo a la vez.");
        }

        List<Prestamo> activos = prestamoRepository.findByCamperIdAndEstado(camperId, "ACTIVO");
        if (activos.size() >= BusinessRulesConfig.MAX_PRESTAMOS_ACTIVOS_CAMPER) {
            Prestamo actual = activos.get(0);
            throw new ReglaNegocioException("Regla de Negocio (Bloqueo): Ya posees un préstamo activo sin devolver ('"
                    + actual.getNombreEquipo() + "'). Registra su devolución antes de solicitar otro recurso.");
        }

        Implemento equipo = implementoRepository.findById(implementoId)
                .orElseThrow(() -> new ReglaNegocioException("El equipo solicitado no existe."));
        if (!"PRESTAMO_CAMPER".equalsIgnoreCase(equipo.getTipo())) {
            throw new ReglaNegocioException("Regla de inventario: Solo se pueden prestar equipos del pool de Campers, no estaciones fijas de un salón.");
        }
        if (!"DISPONIBLE".equalsIgnoreCase(equipo.getEstado()) || equipo.isPrestadoActualmente()) {
            throw new ReglaNegocioException("El equipo '" + equipo.getNombreImplemento() + "' no se encuentra disponible (inhabilitado o ya prestado).");
        }

        equipo.setPrestadoActualmente(true);
        implementoRepository.save(equipo);

        Prestamo prestamo = new Prestamo(
                camper.getId(), camper.getNombre(),
                equipo.getId(), equipo.getNombreImplemento(),
                LocalDateTime.now().format(FMT)
        );
        return prestamoRepository.save(prestamo);
    }

    @Transactional
    public Prestamo devolverPrestamoCamper(Long usuarioId, RolUsuario rol, Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ReglaNegocioException("Préstamo no encontrado."));
        if (rol != RolUsuario.ADMIN && rol != RolUsuario.SOPORTE && !Objects.equals(prestamo.getCamperId(), usuarioId)) {
            throw new ReglaNegocioException("Solo puedes devolver los equipos que tienes a tu nombre.");
        }
        if ("DEVUELTO".equalsIgnoreCase(prestamo.getEstado())) {
            throw new ReglaNegocioException("Este equipo ya fue devuelto anteriormente.");
        }
        prestamo.setEstado("DEVUELTO");
        prestamo.setFechaHoraDevolucion(LocalDateTime.now().format(FMT));

        implementoRepository.findById(prestamo.getImplementoId()).ifPresent(equipo -> {
            equipo.setPrestadoActualmente(false);
            implementoRepository.save(equipo);
        });

        return prestamoRepository.save(prestamo);
    }

    @Transactional
    public Incidencia crearIncidenciaConTriajeIa(Long usuarioId, String espacioNombre, String descripcion) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado."));
        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaNegocioException("Debes describir el incidente para procesar el triaje con IA.");
        }

        List<Espacio> espacios = espacioRepository.findAll();
        Map<String, Object> triaje = aiService.clasificarIncidencia(descripcion, espacioNombre, espacios);

        String salonAfectado = String.valueOf(triaje.getOrDefault("espacioDetectado", espacioNombre));
        String categoria = String.valueOf(triaje.getOrDefault("categoria", "Soporte Técnico"));
        String prioridad = String.valueOf(triaje.getOrDefault("prioridad", "MEDIA"));
        boolean bloquear = Boolean.TRUE.equals(triaje.get("bloquearSalon"));
        String recomendacion = String.valueOf(triaje.getOrDefault("recomendacion", "Atender según protocolo técnico."));
        String fuente = String.valueOf(triaje.getOrDefault("fuente", "MODELO_IA"));

        boolean seBloqueoSalon = false;
        if ("CRITICA".equalsIgnoreCase(prioridad) && bloquear) {
            Optional<Espacio> optEspacio = espacioRepository.findByNombreIgnoreCase(salonAfectado);
            if (optEspacio.isPresent()) {
                Espacio esp = optEspacio.get();
                esp.setEstado("BLOQUEADO");
                esp.setOrigenBloqueo("IA");
                esp.setMotivoBloqueo("Bloqueo automático por IA — Incidencia CRÍTICA: " + descripcion);
                espacioRepository.save(esp);
                seBloqueoSalon = true;
            }
        }

        Incidencia incidencia = new Incidencia(
                usuario.getNombre(), usuario.getRol(), salonAfectado,
                descripcion.trim(), categoria, prioridad, recomendacion,
                seBloqueoSalon, LocalDateTime.now().format(FMT), fuente
        );
        return incidenciaRepository.save(incidencia);
    }

    @Transactional
    public Map<String, Object> resolverIncidenciaSoporte(RolUsuario rol, Long incidenciaId) {
        if (rol != RolUsuario.SOPORTE && rol != RolUsuario.ADMIN) {
            throw new ReglaNegocioException("Permiso denegado: Solo SOPORTE o ADMIN pueden marcar incidencias como resueltas.");
        }
        Incidencia inc = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new ReglaNegocioException("Incidencia no encontrada."));
        inc.setEstado("RESUELTA");
        incidenciaRepository.save(inc);

        boolean salonRehabilitado = false;
        List<Incidencia> criticasRestantes = incidenciaRepository
                .findByEspacioNombreIgnoreCaseAndEstadoAndPrioridadIa(inc.getEspacioNombre(), "ABIERTA", "CRITICA");

        if (criticasRestantes.isEmpty()) {
            Optional<Espacio> optEspacio = espacioRepository.findByNombreIgnoreCase(inc.getEspacioNombre());
            if (optEspacio.isPresent()
                    && "BLOQUEADO".equalsIgnoreCase(optEspacio.get().getEstado())
                    && "IA".equalsIgnoreCase(optEspacio.get().getOrigenBloqueo())) {
                Espacio esp = optEspacio.get();
                esp.setEstado("DISPONIBLE");
                esp.setMotivoBloqueo(null);
                esp.setOrigenBloqueo(null);
                espacioRepository.save(esp);
                salonRehabilitado = true;
            }
        }

        return Map.of(
                "incidencia", inc,
                "salonRehabilitadoAutomaticamente", salonRehabilitado,
                "mensaje", salonRehabilitado
                        ? "Incidencia marcada como RESUELTA. El salón '" + inc.getEspacioNombre() + "' ha sido rehabilitado automáticamente al estado DISPONIBLE."
                        : "Incidencia marcada como RESUELTA correctamente."
        );
    }

    public List<Incidencia> obtenerIncidenciasPriorizadasPorIa() {
        List<Incidencia> todas = incidenciaRepository.findAll();
        Map<String, Integer> pesoPrioridad = Map.of("CRITICA", 1, "MEDIA", 2, "BAJA", 3);
        return todas.stream()
                .sorted(Comparator
                        .comparing((Incidencia i) -> "ABIERTA".equalsIgnoreCase(i.getEstado()) ? 0 : 1)
                        .thenComparing(i -> pesoPrioridad.getOrDefault(i.getPrioridadIa(), 4))
                        .thenComparing(Incidencia::getId, Comparator.reverseOrder()))
                .toList();
    }

    public String construirResumenEstadoCampus() {
        List<Espacio> espacios = espacioRepository.findAll();
        long bloqueados = espacios.stream().filter(e -> "BLOQUEADO".equalsIgnoreCase(e.getEstado())).count();
        long reservasActivas = reservaRepository.findAll().stream().filter(r -> !"RECHAZADA".equalsIgnoreCase(r.getEstado())).count();
        long incidenciasAbiertas = incidenciaRepository.findAll().stream().filter(i -> "ABIERTA".equalsIgnoreCase(i.getEstado())).count();

        StringBuilder sb = new StringBuilder();
        sb.append("- Total Salones Configurados: ").append(espacios.size()).append(" (Bloqueados: ").append(bloqueados).append(")\n");
        sb.append("- Reservas y Solicitudes Activas: ").append(reservasActivas).append("\n");
        sb.append("- Incidencias Abiertas en Soporte: ").append(incidenciasAbiertas).append("\n");
        sb.append("- Desglose de Capacidad Instalada:\n");
        for (Espacio e : espacios) {
            sb.append("  * ").append(e.getNombre()).append(" | Capacidad: ").append(e.getCapacidad())
              .append(" | Estado: ").append(e.getEstado())
              .append(" | Implementos: ").append(e.getResumenImplementos()).append("\n");
        }
        return sb.toString();
    }
}
