package com.campuslands.ops.service;

import com.campuslands.ops.config.BusinessRulesConfig;
import com.campuslands.ops.config.DataSourceConfig;
import com.campuslands.ops.model.Espacio;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiOperationalService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini";
    private static final String FUENTE_MODELO = "MODELO_IA";
    private static final String FUENTE_RESPALDO = "MOTOR_REGLAS_LOCAL";
    private static final Set<String> CATEGORIAS_VALIDAS = Set.of(
            "Infraestructura / Eléctrico",
            "Equipos de Cómputo",
            "Audio y Video (TV / HDMI)",
            "Mobiliario"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String obtenerApiKeyActiva() {
        return DataSourceConfig.obtenerConfig("AI_API_KEY", "");
    }

    private boolean hayApiKey() {
        String key = obtenerApiKeyActiva();
        return key != null && !key.isBlank()
                && !key.equals("tu_api_key_openai_aqui")
                && !key.equals("tu_api_key_de_gemini_aqui");
    }

    public Map<String, Object> clasificarIncidencia(String descripcion, String espacioSeleccionado, List<Espacio> espacios) {
        String textoLower = descripcion.toLowerCase(Locale.ROOT);
        String espacioDetectado = detectarEspacioEnTexto(textoLower, espacioSeleccionado, espacios);

        if (hayApiKey()) {
            try {
                String systemPrompt = "Eres el motor de triaje técnico de Campusland Ops. Responde únicamente con un objeto JSON válido, sin markdown.";
                String userPrompt = """
                    Analiza el siguiente incidente reportado en el campus:
                    Salón reportado: %s
                    Descripción: "%s"
                    Reglas de clasificación:
                    1. En "categoria", elige SOLO UNA: "Infraestructura / Eléctrico", "Equipos de Cómputo", "Audio y Video (TV / HDMI)" o "Mobiliario".
                    2. En "prioridad", elige SOLO UNA:
                       - "CRITICA": riesgo eléctrico, humo, olor a quemado, inundación, corte total de energía o falla masiva que impida usar todo el salón.
                       - "MEDIA": falla de televisor, HDMI, proyector o un computador individual.
                       - "BAJA": estética, periférico menor o mobiliario.
                    3. "bloquearSalon" debe ser true SOLO si la prioridad es CRITICA.
                    4. "recomendacion": acción concreta para Soporte, sin emojis.
                    """.formatted(espacioDetectado, descripcion.replace("\"", "'"));

                String respuestaJson = invocarOpenAi(obtenerApiKeyActiva(), systemPrompt, userPrompt, true);
                JsonObject json = extraerObjetoJson(respuestaJson);
                return controlarSalidaTriaje(json, espacioDetectado, FUENTE_MODELO);
            } catch (Exception ex) {
                System.out.println("[AVISO IA] Triaje: la API no respondió de forma usable (" + ex.getMessage() + "). Se aplica motor de reglas local.");
            }
        }

        return clasificacionHeuristica(textoLower, espacioDetectado);
    }

    public Map<String, Object> recomendarSalonOptimo(int asistentes, String fecha, String horaInicio, String horaFin,
                                                     List<Espacio> espaciosDisponiblesEnHorario, String consultaNatural) {
        List<Espacio> candidatosQueCaben = espaciosDisponiblesEnHorario.stream()
                .filter(e -> e.getCapacidad() >= asistentes)
                .sorted(Comparator.comparingInt(Espacio::getCapacidad))
                .toList();

        if (candidatosQueCaben.isEmpty()) {
            Map<String, Object> vacio = new HashMap<>();
            vacio.put("exito", false);
            vacio.put("fuente", FUENTE_RESPALDO);
            vacio.put("validadoPorReglasNegocio", true);
            vacio.put("mensaje", "No existe ningún salón disponible entre " + horaInicio + " y " + horaFin
                    + " con capacidad suficiente para " + asistentes + " personas.");
            return vacio;
        }

        if (hayApiKey()) {
            try {
                StringBuilder catalogo = new StringBuilder();
                for (Espacio e : candidatosQueCaben) {
                    catalogo.append(String.format(Locale.ROOT,
                            "- id=%d | nombre=%s | capacidad=%d | implementos=%s%n",
                            e.getId(), e.getNombre(), e.getCapacidad(), e.getResumenImplementos()));
                }
                String systemPrompt = "Eres el asignador de aulas de Campusland Ops. Responde únicamente JSON válido, sin markdown.";
                String userPrompt = """
                    Elige UN salón del catálogo (usa el id exacto) para %d asistentes el %s de %s a %s.
                    Contexto adicional del trainer: %s
                    Catálogo de salones YA filtrados como libres y con aforo suficiente:
                    %s
                    Preferencia: el salón más pequeño donde quepa el grupo (eficiencia de aforo, umbral 50%%).
                    JSON: {"espacioId": <numero>, "mensaje": "<explicación breve sin emojis>"}
                    """.formatted(
                        asistentes, fecha, horaInicio, horaFin,
                        (consultaNatural == null || consultaNatural.isBlank()) ? "sin comentarios extra" : consultaNatural.replace("\"", "'"),
                        catalogo
                );

                String respuestaJson = invocarOpenAi(obtenerApiKeyActiva(), systemPrompt, userPrompt, true);
                JsonObject json = extraerObjetoJson(respuestaJson);
                Long propuestoId = json.has("espacioId") ? json.get("espacioId").getAsLong() : null;
                String mensajeModelo = json.has("mensaje") ? json.get("mensaje").getAsString() : "";
                Map<String, Object> controlado = controlarRecomendacion(propuestoId, mensajeModelo, asistentes, horaInicio, horaFin, candidatosQueCaben);
                controlado.put("fuente", FUENTE_MODELO);
                return controlado;
            } catch (Exception ex) {
                System.out.println("[AVISO IA] Recomendación: la API no respondió de forma usable (" + ex.getMessage() + "). Se aplica motor de reglas local.");
            }
        }

        Map<String, Object> local = construirRecomendacionPorReglas(asistentes, horaInicio, horaFin, candidatosQueCaben, null, null);
        local.put("fuente", FUENTE_RESPALDO);
        local.put("mensaje", local.get("mensaje") + " (Motor de reglas local: la API de IA no está disponible o no devolvió un JSON válido.)");
        return local;
    }

    public String responderConsultaGeneral(String pregunta, String rolUsuario, String resumenEstadoCampus) {
        if (pregunta == null || pregunta.isBlank()) {
            return "Escribe una consulta operativa para analizar el estado actual del campus.";
        }
        if (hayApiKey()) {
            try {
                String systemPrompt = "Eres el asistente operativo de Campusland Ops. "
                        + "Responde claro y directo, sin emojis y sin mencionar APIs, bases de datos o nombres de modelos. "
                        + "Usa SOLO el estado operativo que se te entrega. Si no hay dato, dilo. "
                        + "No inventes salones, seriales ni reservas que no aparezcan en el resumen.";
                String userPrompt = """
                    Rol del usuario que consulta: %s
                    Estado actual de los espacios del campus:
                    %s
                    Pregunta del usuario: "%s"
                    """.formatted(rolUsuario, resumenEstadoCampus, pregunta.replace("\"", "'"));
                return invocarOpenAi(obtenerApiKeyActiva(), systemPrompt, userPrompt, false);
            } catch (Exception ex) {
                System.out.println("Consulta general: usando resumen local (" + ex.getMessage() + ")");
            }
        }

        return "No fue posible consultar el modelo de IA en este momento. Resumen operativo local para \""
                + pregunta + "\":\n\n" + resumenEstadoCampus;
    }

    private Map<String, Object> controlarSalidaTriaje(JsonObject json, String espacioDetectado, String fuente) {
        String categoriaRaw = json.has("categoria") ? json.get("categoria").getAsString() : "Equipos de Cómputo";
        String categoria = CATEGORIAS_VALIDAS.stream()
                .filter(c -> c.equalsIgnoreCase(categoriaRaw) || categoriaRaw.toLowerCase(Locale.ROOT).contains(c.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse("Equipos de Cómputo");

        String prioridadRaw = json.has("prioridad") ? json.get("prioridad").getAsString().toUpperCase(Locale.ROOT) : "MEDIA";
        boolean esCritica = prioridadRaw.contains("CRITIC");
        String prioridadFinal = esCritica ? "CRITICA" : (prioridadRaw.contains("BAJA") ? "BAJA" : "MEDIA");
        boolean bloquearModelo = json.has("bloquearSalon") && json.get("bloquearSalon").getAsBoolean();
        boolean bloquear = esCritica && (bloquearModelo || !json.has("bloquearSalon"));
        if (!esCritica) {
            bloquear = false;
        }

        String recomendacion = json.has("recomendacion") ? json.get("recomendacion").getAsString() : "Atender según protocolo técnico.";

        Map<String, Object> out = new HashMap<>();
        out.put("espacioDetectado", espacioDetectado);
        out.put("categoria", categoria);
        out.put("prioridad", prioridadFinal);
        out.put("bloquearSalon", bloquear);
        out.put("recomendacion", recomendacion);
        out.put("fuente", fuente);
        out.put("validadoPorReglasNegocio", true);
        return out;
    }

    private Map<String, Object> clasificacionHeuristica(String textoLower, String espacioDetectado) {
        boolean esCritica = textoLower.contains("quemado") || textoLower.contains("humo")
                || textoLower.contains("corto") || textoLower.contains("agua")
                || textoLower.contains("inund") || textoLower.contains("no hay luz")
                || textoLower.contains("electric") || textoLower.contains("caida total")
                || textoLower.contains("caída total") || textoLower.contains("crítico")
                || textoLower.contains("critico");

        boolean esBaja = textoLower.contains("mouse") || textoLower.contains("tecla")
                || textoLower.contains("limpieza") || textoLower.contains("silla");

        String prioridad = esCritica ? "CRITICA" : (esBaja ? "BAJA" : "MEDIA");
        String categoria = (textoLower.contains("luz") || textoLower.contains("quemado") || textoLower.contains("aire") || textoLower.contains("agua"))
                ? "Infraestructura / Eléctrico"
                : (textoLower.contains("hdmi") || textoLower.contains("televisor") || textoLower.contains("pantalla") || textoLower.contains("video"))
                ? "Audio y Video (TV / HDMI)"
                : (textoLower.contains("silla") || textoLower.contains("mesa") || textoLower.contains("mobiliario"))
                ? "Mobiliario"
                : "Equipos de Cómputo";

        String recomendacion = esCritica
                ? "Se bloquea preventivamente el salón '" + espacioDetectado + "' por seguridad operativa. Soporte debe intervenir de inmediato."
                : "Verificar el recurso reportado en '" + espacioDetectado + "' y actualizar su disponibilidad en el inventario.";

        Map<String, Object> out = new HashMap<>();
        out.put("espacioDetectado", espacioDetectado);
        out.put("categoria", categoria);
        out.put("prioridad", prioridad);
        out.put("bloquearSalon", esCritica);
        out.put("recomendacion", recomendacion);
        out.put("fuente", FUENTE_RESPALDO);
        out.put("validadoPorReglasNegocio", true);
        return out;
    }

    private Map<String, Object> controlarRecomendacion(Long propuestoId, String mensajeModelo, int asistentes,
                                                       String horaInicio, String horaFin, List<Espacio> candidatosQueCaben) {
        Espacio propuesto = candidatosQueCaben.stream()
                .filter(e -> propuestoId != null && e.getId().equals(propuestoId))
                .findFirst()
                .orElse(null);

        if (propuesto == null) {
            Map<String, Object> local = construirRecomendacionPorReglas(asistentes, horaInicio, horaFin, candidatosQueCaben, null, null);
            local.put("mensaje", "El modelo propuso un salón fuera del catálogo válido. Se aplicó la regla de aforo verificada. " + local.get("mensaje"));
            local.put("ajusteControlRespuesta", true);
            return local;
        }

        Espacio optimoRegla = candidatosQueCaben.stream()
                .filter(e -> asistentes >= Math.ceil(e.getCapacidad() * BusinessRulesConfig.PORCENTAJE_MINIMO_OCUPACION_TRAINER))
                .findFirst()
                .orElse(candidatosQueCaben.get(0));

        boolean hayMejorMasChico = optimoRegla.getCapacidad() < propuesto.getCapacidad()
                && asistentes >= Math.ceil(optimoRegla.getCapacidad() * BusinessRulesConfig.PORCENTAJE_MINIMO_OCUPACION_TRAINER);

        if (hayMejorMasChico) {
            Map<String, Object> ajustado = construirRecomendacionPorReglas(asistentes, horaInicio, horaFin, candidatosQueCaben, optimoRegla, mensajeModelo);
            ajustado.put("ajusteControlRespuesta", true);
            ajustado.put("mensaje", "Control de respuesta: el modelo sugirió '" + propuesto.getNombre()
                    + "', pero las reglas de ocupación eligen '" + optimoRegla.getNombre() + "'. " + ajustado.get("mensaje"));
            return ajustado;
        }

        return construirRecomendacionPorReglas(asistentes, horaInicio, horaFin, candidatosQueCaben, propuesto, mensajeModelo);
    }

    private Map<String, Object> construirRecomendacionPorReglas(int asistentes, String horaInicio, String horaFin,
                                                                List<Espacio> candidatosQueCaben, Espacio forzado, String notaModelo) {
        Espacio optimoConRegla50 = candidatosQueCaben.stream()
                .filter(e -> asistentes >= Math.ceil(e.getCapacidad() * BusinessRulesConfig.PORCENTAJE_MINIMO_OCUPACION_TRAINER))
                .findFirst()
                .orElse(null);

        Espacio elegido = forzado != null ? forzado : (optimoConRegla50 != null ? optimoConRegla50 : candidatosQueCaben.get(0));
        boolean aplicoExcepcionLleno = (optimoConRegla50 == null);
        int porcentajeOcupacion = (int) Math.round((asistentes * 100.0) / elegido.getCapacidad());

        String explicacion = aplicoExcepcionLleno
                ? String.format(Locale.ROOT,
                "Se asigna '%s' (Capacidad: %d | Ocupación: %d%%). Se autoriza por excepción: no hay salón menor libre en %s-%s.",
                elegido.getNombre(), elegido.getCapacidad(), porcentajeOcupacion, horaInicio, horaFin)
                : String.format(Locale.ROOT,
                "Salón recomendado: '%s' (Capacidad: %d | Implementos: %s). Con %d asistentes se alcanza un %d%% de ocupación.",
                elegido.getNombre(), elegido.getCapacidad(), elegido.getResumenImplementos(), asistentes, porcentajeOcupacion);

        if (notaModelo != null && !notaModelo.isBlank()) {
            explicacion = explicacion + " Criterio del modelo: " + notaModelo;
        }

        Map<String, Object> out = new HashMap<>();
        out.put("exito", true);
        out.put("espacioId", elegido.getId());
        out.put("espacioNombre", elegido.getNombre());
        out.put("capacidad", elegido.getCapacidad());
        out.put("implementos", elegido.getResumenImplementos());
        out.put("ocupacionPorcentaje", porcentajeOcupacion);
        out.put("excepcionAplicada", aplicoExcepcionLleno);
        out.put("validadoPorReglasNegocio", true);
        out.put("ajusteControlRespuesta", false);
        out.put("mensaje", explicacion);
        return out;
    }

    private String detectarEspacioEnTexto(String textoLower, String porDefecto, List<Espacio> espacios) {
        for (Espacio e : espacios) {
            if (textoLower.contains(e.getNombre().toLowerCase(Locale.ROOT))) {
                return e.getNombre();
            }
        }
        if (porDefecto != null && !porDefecto.isBlank()) {
            return porDefecto;
        }
        return "General Campus";
    }

    private JsonObject extraerObjetoJson(String contenido) {
        String trimmed = contenido == null ? "" : contenido.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return JsonParser.parseString(trimmed).getAsJsonObject();
    }

    private String invocarOpenAi(String apiKey, String systemPrompt, String userPrompt, boolean jsonMode) throws Exception {
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);

        JsonObject usrMsg = new JsonObject();
        usrMsg.addProperty("role", "user");
        usrMsg.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(usrMsg);

        JsonObject payload = new JsonObject();
        payload.addProperty("model", OPENAI_MODEL);
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.2);

        if (jsonMode) {
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_object");
            payload.add("response_format", responseFormat);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        return root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
