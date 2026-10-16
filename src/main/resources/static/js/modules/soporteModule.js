import { apiRequest, showToast } from '../api.js';
import { Icons } from '../icons.js';

export async function cargarDatosSoporte(usuarioActual, onDataUpdated) {
    await Promise.all([
        cargarBandejaIncidenciasSoporte(usuarioActual, onDataUpdated),
        cargarInventarioSalonSeleccionado(usuarioActual, onDataUpdated)
    ]);
}

export async function cargarInventarioSalonSeleccionado(usuarioActual, onDataUpdated) {
    const select = document.getElementById('soporteSelectSalon');
    const contenedor = document.getElementById('tablaInventarioSoporte');
    if (!select || !contenedor || !select.value) return;

    const items = await apiRequest(`/api/espacios/${select.value}/implementos`, { loadingMessage: 'Cargando inventario del salón...' });
    contenedor.innerHTML = '';

    items.forEach(item => {
        const disponible = item.estado === 'DISPONIBLE';
        const nuevoEstado = disponible ? 'NO_DISPONIBLE' : 'DISPONIBLE';

        const fila = document.createElement('div');
        fila.className = `p-3.5 rounded-xl border flex flex-wrap items-center justify-between gap-3 text-xs ${
            disponible ? 'bg-white border-slate-200' : 'bg-red-50/40 border-red-200'
        }`;

        fila.innerHTML = `
            <div class="space-y-1">
                <div class="flex items-center gap-2">
                    <span class="font-bold text-slate-900">${item.nombreImplemento}</span>
                    <span class="px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                        disponible ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-red-100 text-red-800 border border-red-200'
                    }">${item.estado}</span>
                </div>
                <p class="text-slate-500">
                    Monitor/TV: <b class="text-slate-700">${item.serialMonitor}</b> |
                    Mouse: <b class="text-slate-700">${item.serialMouse}</b> |
                    Torre: <b class="text-slate-700">${item.serialTorre}</b> |
                    Teclado/HDMI: <b class="text-slate-700">${item.serialTeclado}</b>
                </p>
                ${item.motivoInhabilitado ? `<p class="text-red-600 font-medium">Motivo inhabilitación: ${item.motivoInhabilitado}</p>` : ''}
            </div>
            <button data-item-id="${item.id}" data-nuevo-estado="${nuevoEstado}"
                    class="px-3 py-1.5 rounded-lg font-semibold transition ${
                        disponible
                            ? 'bg-slate-100 hover:bg-red-50 text-slate-700 hover:text-red-700 border border-slate-200'
                            : 'bg-emerald-600 hover:bg-emerald-700 text-white'
                    }">
                ${disponible ? 'Marcar No Disponible' : 'Habilitar Disponible'}
            </button>
        `;
        contenedor.appendChild(fila);
    });

    contenedor.onclick = async (e) => {
        const btn = e.target.closest('button[data-item-id]');
        if (!btn) return;
        const implementoId = Number(btn.dataset.itemId);
        const nuevoEstado = btn.dataset.nuevoEstado;
        let motivo = '';
        if (nuevoEstado === 'NO_DISPONIBLE') {
            motivo = prompt('Indica el motivo técnico por el cual el equipo queda NO DISPONIBLE:') || 'Mantenimiento técnico';
        }
        try {
            await apiRequest(`/api/implementos/${implementoId}/estado`, {
                method: 'PATCH',
                loadingMessage: 'Actualizando disponibilidad del equipo...',
                body: JSON.stringify({
                    estado: nuevoEstado,
                    motivo
                })
            });
            showToast(`Estado del implemento actualizado a ${nuevoEstado}.`);
            await onDataUpdated();
        } catch (err) {
            showToast(err.message, true);
        }
    };
}

export async function cargarBandejaIncidenciasSoporte(usuarioActual, onDataUpdated) {
    const incidencias = await apiRequest('/api/incidencias', { loadingMessage: 'Cargando bandeja priorizada por IA...' });
    const contenedores = [
        document.getElementById('listaIncidenciasSoporte'),
        document.getElementById('listaIncidenciasAdmin')
    ].filter(Boolean);

    contenedores.forEach(contenedor => {
        contenedor.innerHTML = '';

        if (incidencias.length === 0) {
            contenedor.innerHTML = `
                <div class="p-8 text-center bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-500">
                    No existen reportes de daños o incidencias pendientes.
                </div>
            `;
            return;
        }

        incidencias.forEach(inc => {
            const abierta = inc.estado === 'ABIERTA';
            const prioBadge = inc.prioridadIa === 'CRITICA'
                ? 'bg-red-100 text-red-800 border-red-200'
                : inc.prioridadIa === 'MEDIA'
                ? 'bg-amber-100 text-amber-800 border-amber-200'
                : 'bg-blue-50 text-blue-700 border-blue-200';

            const card = document.createElement('div');
            card.className = `p-4 rounded-xl border space-y-2.5 text-xs ${
                abierta ? 'bg-white border-slate-200 shadow-sm' : 'bg-slate-50 border-slate-200 opacity-75'
            }`;
            card.innerHTML = `
                <div class="flex items-center justify-between gap-2">
                    <div class="flex items-center gap-2">
                        <span class="px-2.5 py-0.5 rounded-full font-bold border text-[11px] ${prioBadge}">
                            PRIORIDAD IA: ${inc.prioridadIa}
                        </span>
                        <span class="px-2 py-0.5 rounded-full font-semibold border border-slate-200 bg-slate-50 text-slate-600 text-[10px]">
                            ${inc.fuenteClasificacion === 'MODELO_IA' ? 'Modelo IA' : 'Reglas locales'}
                        </span>
                        <span class="font-bold text-slate-900 text-sm">${inc.espacioNombre}</span>
                        <span class="text-slate-500">(${inc.categoriaIa})</span>
                    </div>
                    <span class="font-semibold ${abierta ? 'text-amber-700' : 'text-emerald-700'}">${inc.estado}</span>
                </div>
                <p class="text-slate-800 font-medium">"${inc.descripcion}"</p>
                <div class="p-2.5 rounded-lg bg-slate-50 border border-slate-200 text-slate-600">
                    ${inc.recomendacionIa}
                </div>
                <div class="flex items-center justify-between pt-1 text-slate-500">
                    <span>Reportado por: <b>${inc.reportadoPorNombre}</b> (${inc.rolReportante}) — ${inc.fechaHora}</span>
                    ${abierta ? `
                        <button data-resolver-id="${inc.id}"
                                class="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-semibold">
                            ${Icons.check}
                            <span>Aceptar / Marcar Resuelta</span>
                        </button>
                    ` : ''}
                </div>
            `;
            contenedor.appendChild(card);
        });

        contenedor.onclick = async (e) => {
            const btn = e.target.closest('button[data-resolver-id]');
            if (!btn) return;
            const id = Number(btn.dataset.resolverId);
            try {
                const res = await apiRequest(`/api/incidencias/${id}/resolver`, {
                    method: 'PATCH',
                    loadingMessage: 'Resolviendo incidencia y evaluando desbloqueo...',
                    body: JSON.stringify({})
                });
                showToast(res.mensaje);
                await onDataUpdated();
            } catch (err) {
                showToast(err.message, true);
            }
        };
    });
}

export function inicializarModuloSoporte(getUsuarioActual, onDataUpdated) {
    const select = document.getElementById('soporteSelectSalon');
    if (select) {
        select.addEventListener('change', () => {
            cargarInventarioSalonSeleccionado(getUsuarioActual(), onDataUpdated);
        });
    }
}
