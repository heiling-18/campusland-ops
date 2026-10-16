import { apiRequest, showToast } from '../api.js';
import { Icons } from '../icons.js';

let salonesCache = [];

export function getSalonesCache() {
    return salonesCache;
}

export async function cargarYRenderizarEspacios(usuarioActual, onDataUpdated) {
    salonesCache = await apiRequest('/api/espacios', { loadingMessage: 'Cargando espacios oficiales...' });
    const grid = document.getElementById('gridSalones');
    if (!grid) return;
    grid.innerHTML = '';

    // Actualizar contadores KPI superiores
    const totalSalones = salonesCache.length;
    const disponibles = salonesCache.filter(s => s.estado === 'DISPONIBLE').length;
    const bloqueados = totalSalones - disponibles;
    const aforoTotal = salonesCache.reduce((acc, s) => acc + s.capacidad, 0);

    document.getElementById('kpiTotalSalones').textContent = totalSalones;
    document.getElementById('kpiSalonesDisponibles').textContent = disponibles;
    document.getElementById('kpiSalonesBloqueados').textContent = bloqueados;
    document.getElementById('kpiAforoInstalado').textContent = `${aforoTotal} puestos`;

    salonesCache.forEach(salon => {
        const esBloqueado = salon.estado === 'BLOQUEADO';
        const card = document.createElement('div');
        card.className = `bg-white rounded-xl border ${
            esBloqueado ? 'border-red-300 bg-red-50/20' : 'border-slate-200'
        } p-5 shadow-sm flex flex-col justify-between transition hover:shadow-md`;

        let controlesAccion = '';
        if (usuarioActual.rol === 'ADMIN' || usuarioActual.rol === 'SOPORTE') {
            const nuevoEstado = esBloqueado ? 'DISPONIBLE' : 'BLOQUEADO';
            controlesAccion += `
                <div class="pt-4 mt-4 border-t border-slate-100 flex items-center gap-2">
                    <button data-action="toggle-estado" data-id="${salon.id}" data-estado="${nuevoEstado}"
                            class="flex-1 inline-flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg text-xs font-semibold transition ${
                                esBloqueado
                                    ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                                    : 'bg-slate-100 hover:bg-red-50 text-slate-700 hover:text-red-700 border border-slate-200'
                            }">
                        ${esBloqueado ? Icons.unlock : Icons.lock}
                        <span>${esBloqueado ? 'Habilitar Salón' : 'Bloquear Salón'}</span>
                    </button>
                    ${usuarioActual.rol === 'ADMIN' ? `
                        <button data-action="editar-salon" data-id="${salon.id}" title="Editar capacidad o nombre"
                                class="p-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200">
                            ${Icons.edit}
                        </button>
                        <button data-action="eliminar-salon" data-id="${salon.id}" title="Eliminar salón"
                                class="p-2 rounded-lg bg-red-50 hover:bg-red-100 text-red-600 border border-red-200">
                            ${Icons.trash}
                        </button>
                    ` : ''}
                </div>
            `;
        }

        card.innerHTML = `
            <div>
                <div class="flex items-start justify-between gap-2">
                    <div class="flex items-center gap-2.5">
                        <div class="p-2 rounded-lg ${esBloqueado ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-700'}">
                            ${Icons.building}
                        </div>
                        <div>
                            <h4 class="font-bold text-slate-900 text-sm">${salon.nombre}</h4>
                            <span class="text-[11px] font-medium text-slate-500 uppercase tracking-wider">${salon.categoria}</span>
                        </div>
                    </div>
                    <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold ${
                        esBloqueado
                            ? 'bg-red-100 text-red-800 border border-red-200'
                            : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    }">
                        <span class="w-1.5 h-1.5 rounded-full ${esBloqueado ? 'bg-red-600' : 'bg-emerald-600'}"></span>
                        ${salon.estado}
                    </span>
                </div>

                <div class="mt-4 pt-3 border-t border-slate-100 space-y-2 text-xs">
                    <div class="flex items-center justify-between">
                        <span class="text-slate-500 font-medium">Capacidad Instalada</span>
                        <span class="font-bold text-slate-900">${salon.capacidad} personas</span>
                    </div>
                    <div class="bg-slate-50 rounded-lg p-2.5 border border-slate-100">
                        <span class="block text-[11px] font-semibold text-slate-500 uppercase">Implementos del Salón</span>
                        <p class="text-slate-700 font-medium mt-0.5">${salon.resumenImplementos}</p>
                    </div>
                    ${salon.motivoBloqueo ? `
                        <div class="bg-red-50 border border-red-200 rounded-lg p-2 text-red-700 text-[11px] font-medium">
                            Motivo bloqueo: ${salon.motivoBloqueo}
                        </div>
                    ` : ''}
                </div>
            </div>
            ${controlesAccion}
        `;
        grid.appendChild(card);
    });

    // Delegacion de eventos de tarjetas
    grid.onclick = async (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        const action = btn.dataset.action;

        try {
            if (action === 'toggle-estado') {
                const nuevoEstado = btn.dataset.estado;
                await apiRequest(`/api/espacios/${id}/estado`, {
                    method: 'PATCH',
                    loadingMessage: 'Actualizando estado del salón...',
                    body: JSON.stringify({
                        estado: nuevoEstado,
                        motivo: nuevoEstado === 'BLOQUEADO' ? `Bloqueado manualmente por ${usuarioActual.rol}` : ''
                    })
                });
                showToast(`Estado del salón actualizado a ${nuevoEstado}.`);
                await onDataUpdated();
            } else if (action === 'editar-salon') {
                const s = salonesCache.find(item => item.id === id);
                if (!s) return;
                document.getElementById('adminSalonId').value = s.id;
                document.getElementById('adminSalonNombre').value = s.nombre;
                document.getElementById('adminSalonCapacidad').value = s.capacidad;
                document.getElementById('adminSalonImplementos').value = s.resumenImplementos;
                document.querySelector('[data-view="viewAdmin"]').click();
            } else if (action === 'eliminar-salon') {
                if (!confirm('¿Eliminar este salón? Solo se permite si no tiene reservas activas.')) return;
                await apiRequest(`/api/espacios/${id}`, { method: 'DELETE', loadingMessage: 'Eliminando salón...' });
                showToast('Salón eliminado correctamente del catálogo.');
                await onDataUpdated();
            }
        } catch (err) {
            showToast(err.message, true);
        }
    };

    sincronizarSelectoresSalones();
}

export function sincronizarSelectoresSalones() {
    const selectReserva = document.getElementById('reservaEspacioId');
    const selectIncidencia = document.getElementById('incidenciaSalon');
    const selectSoporte = document.getElementById('soporteSelectSalon');

    if (selectReserva) {
        const prev = selectReserva.value;
        selectReserva.innerHTML = '';
        salonesCache.forEach(s => {
            const opt = document.createElement('option');
            opt.value = s.id;
            opt.textContent = `${s.nombre} — Capacidad: ${s.capacidad} (${s.estado})`;
            selectReserva.appendChild(opt);
        });
        if (prev && [...selectReserva.options].some(o => o.value === prev)) selectReserva.value = prev;
        actualizarDetalleImplementosReserva();
    }

    if (selectIncidencia) {
        selectIncidencia.innerHTML = '';
        salonesCache.forEach(s => {
            const opt = document.createElement('option');
            opt.value = s.nombre;
            opt.textContent = `${s.nombre} (Capacidad: ${s.capacidad})`;
            selectIncidencia.appendChild(opt);
        });
    }

    if (selectSoporte) {
        const prevSop = selectSoporte.value;
        selectSoporte.innerHTML = '';
        salonesCache.forEach(s => {
            const opt = document.createElement('option');
            opt.value = s.id;
            opt.textContent = `${s.nombre} — ${s.resumenImplementos}`;
            selectSoporte.appendChild(opt);
        });
        if (prevSop && [...selectSoporte.options].some(o => o.value === prevSop)) selectSoporte.value = prevSop;
    }
}

export function actualizarDetalleImplementosReserva() {
    const select = document.getElementById('reservaEspacioId');
    const box = document.getElementById('infoImplementosReserva');
    if (!select || !box || !select.value) return;

    const salon = salonesCache.find(s => String(s.id) === String(select.value));
    if (!salon) return;

    const min50 = Math.ceil(salon.capacidad * 0.5);
    const inputAsistentes = document.getElementById('reservaAsistentes');
    if (inputAsistentes) {
        inputAsistentes.max = String(salon.capacidad);
    }
    box.innerHTML = `
        <div class="flex items-start gap-2.5">
            <span class="text-blue-600 mt-0.5">${Icons.cpu}</span>
            <div>
                <p class="font-semibold text-slate-900">Implementos en ${salon.nombre}: ${salon.resumenImplementos}</p>
                <p class="text-slate-500 mt-0.5">
                    Aforo máximo físico: <b>${salon.capacidad} personas</b> | Umbral de eficiencia (50%): <b>${min50} personas</b>
                </p>
            </div>
        </div>
    `;
}

export function inicializarFormularioCrudAdmin(getUsuarioActual, onDataUpdated) {
    const form = document.getElementById('formCrudSalonAdmin');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await apiRequest('/api/espacios', {
                method: 'POST',
                loadingMessage: 'Guardando salón en el catálogo oficial...',
                body: JSON.stringify({
                    id: document.getElementById('adminSalonId').value,
                    nombre: document.getElementById('adminSalonNombre').value,
                    capacidad: document.getElementById('adminSalonCapacidad').value,
                    resumenImplementos: document.getElementById('adminSalonImplementos').value
                })
            });
            limpiarFormularioSalonAdmin();
            showToast('Configuración del salón guardada correctamente.');
            await onDataUpdated();
        } catch (err) {
            showToast(err.message, true);
        }
    });

    const btnLimpiar = document.getElementById('btnLimpiarSalonAdmin');
    if (btnLimpiar) {
        btnLimpiar.addEventListener('click', limpiarFormularioSalonAdmin);
    }
}

export function limpiarFormularioSalonAdmin() {
    document.getElementById('adminSalonId').value = '';
    document.getElementById('adminSalonNombre').value = '';
    document.getElementById('adminSalonCapacidad').value = '';
    document.getElementById('adminSalonImplementos').value = '';
}
