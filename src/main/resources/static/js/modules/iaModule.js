import { apiRequest, showToast } from '../api.js';

export function inicializarModuloIa(getUsuarioActual, onDataUpdated) {
    const formIncidencia = document.getElementById('formReportarIncidencia');
    if (formIncidencia) {
        formIncidencia.addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const espacioNombre = document.getElementById('incidenciaSalon').value;
                const descripcion = document.getElementById('incidenciaDescripcion').value;
                const inc = await apiRequest('/api/incidencias', {
                    method: 'POST',
                    loadingMessage: 'La IA está clasificando el incidente...',
                    body: JSON.stringify({
                        espacioNombre,
                        descripcion
                    })
                });
                document.getElementById('incidenciaDescripcion').value = '';
                const fuente = inc.fuenteClasificacion === 'MODELO_IA' ? 'Modelo IA' : 'Motor de reglas local';
                showToast(
                    `Reporte registrado (${fuente}) con prioridad ${inc.prioridadIa} (${inc.categoriaIa}). ${
                        inc.bloqueoSalonAplicado ? `El salón ${inc.espacioNombre} fue bloqueado preventivamente.` : ''
                    }`
                );
                await onDataUpdated();
            } catch (err) {
                showToast(err.message, true);
            }
        });
    }

    const formConsulta = document.getElementById('formConsultaGeneralIa');
    if (formConsulta) {
        formConsulta.addEventListener('submit', async (e) => {
            e.preventDefault();
            const out = document.getElementById('respuestaGeneralIa');
            out.classList.remove('hidden');
            out.textContent = 'Consultando disponibilidad actual del campus...';

            try {
                const pregunta = document.getElementById('inputPreguntaGeneralIa').value;
                const data = await apiRequest('/api/ia/consulta-general', {
                    method: 'POST',
                    loadingMessage: 'Consultando copiloto con el estado vivo del campus...',
                    body: JSON.stringify({ pregunta })
                });
                out.textContent = data.respuesta;
            } catch (err) {
                out.textContent = 'No se pudo procesar la consulta en este momento: ' + err.message;
            }
        });
    }
}
