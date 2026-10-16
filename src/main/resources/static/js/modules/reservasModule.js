import { apiRequest, showToast } from '../api.js';
import { Icons } from '../icons.js';
import { actualizarDetalleImplementosReserva, getSalonesCache } from './espaciosModule.js';

export async function cargarYRenderizarReservas(usuarioActual, onDataUpdated) {
    const reservas = await apiRequest('/api/reservas', { loadingMessage: 'Cargando reservas y solicitudes...' });
    renderizarTablaReservas(reservas, usuarioActual, onDataUpdated);

    if (usuarioActual.rol === 'ADMIN') {
        renderizarBandejaSolicitudesAdmin(reservas, usuarioActual, onDataUpdated);
    }
}

function renderizarBandejaSolicitudesAdmin(reservas, usuarioActual, onDataUpdated) {
    const contenedor = document.getElementById('listaSolicitudesPendientesAdmin');
    if (!contenedor) return;
    contenedor.innerHTML = '';

    const pendientes = reservas.filter(r => r.estado === 'PENDIENTE');
    if (pendientes.length === 0) {
        contenedor.innerHTML = `
            <div class="p-6 text-center bg-slate-50 rounded-lg border border-slate-200 text-xs text-slate-500">
                No existen solicitudes pendientes de Trainers o Campers por aprobar.
            </div>
        `;
        return;
    }

    pendientes.forEach(p => {
        const row = document.createElement('div');
        row.className = 'p-4 rounded-xl bg-white border border-amber-200 shadow-sm flex flex-wrap items-center justify-between gap-4';
        row.innerHTML = `
            <div class="space-y-1 text-xs">
                <div class="flex items-center gap-2">
                    <span class="font-bold text-slate-900 text-sm">${p.espacioNombre}</span>
                    <span class="px-2 py-0.5 rounded bg-amber-100 text-amber-800 font-semibold text-[11px]">PENDIENTE APROBACIÓN</span>
                    <span class="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-semibold text-[11px]">${p.rolUsuario}</span>
                </div>
                <p class="text-slate-600">
                    Solicitante: <b>${p.usuarioNombre}</b> (${p.rolUsuario}) | Fecha: <b>${p.fecha}</b> (${p.horaInicio} - ${p.horaFin}) | Asistentes: <b>${p.cantidadAsistentes}</b>
                </p>
                <p class="text-slate-500">Motivo: ${p.motivo} | Implementos: ${p.implementosIncluidos}</p>
            </div>
            <div class="flex items-center gap-2">
                <button data-approve="true" data-id="${p.id}"
                        class="px-3.5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold">
                    Aprobar Solicitud
                </button>
                <button data-approve="false" data-id="${p.id}"
                        class="px-3.5 py-2 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 text-xs font-semibold">
                    Rechazar
                </button>
            </div>
        `;
        contenedor.appendChild(row);
    });

    contenedor.onclick = async (e) => {
        const btn = e.target.closest('button[data-approve]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        const aprobar = btn.dataset.approve === 'true';
        try {
            await apiRequest(`/api/reservas/${id}/aprobacion`, {
                method: 'PATCH',
                loadingMessage: aprobar ? 'Revalidando integridad y aprobando...' : 'Rechazando solicitud...',
                body: JSON.stringify({ aprobar })
            });
            showToast(aprobar ? 'Solicitud de reserva aprobada por Administración.' : 'Solicitud de reserva rechazada por Administración.');
            await onDataUpdated();
        } catch (err) {
            showToast(err.message, true);
        }
    };
}

function renderizarTablaReservas(reservas, usuarioActual, onDataUpdated) {
    const contenedor = document.getElementById('listaReservasGeneral');
    if (!contenedor) return;
    contenedor.innerHTML = '';

    const visibles = usuarioActual.rol === 'ADMIN'
        ? reservas
        : reservas.filter(r => Number(r.usuarioId) === Number(usuarioActual.id));

    if (visibles.length === 0) {
        contenedor.innerHTML = `
            <div class="p-8 text-center bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-500">
                No tienes reservas ni solicitudes registradas.
            </div>
        `;
        return;
    }

    visibles.forEach(r => {
        const esMia = Number(r.usuarioId) === Number(usuarioActual.id);
        const puedeEliminar = usuarioActual.rol === 'ADMIN' || esMia;
        const puedeReubicarIa = usuarioActual.rol === 'TRAINER' && esMia && r.estado !== 'RECHAZADA';

        const badgeClass = r.estado === 'APROBADA'
            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
            : r.estado === 'PENDIENTE'
            ? 'bg-amber-50 text-amber-700 border-amber-200'
            : 'bg-red-50 text-red-700 border-red-200';

        const item = document.createElement('div');
        item.className = 'p-4 rounded-xl bg-white border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-4';
        item.innerHTML = `
            <div class="space-y-1 text-xs">
                <div class="flex items-center gap-2.5">
                    <span class="font-bold text-slate-900 text-sm">${r.espacioNombre}</span>
                    <span class="px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${badgeClass}">${r.estado}</span>
                    <span class="text-slate-500 font-medium">${r.fecha} | ${r.horaInicio} - ${r.horaFin}</span>
                </div>
                <p class="text-slate-600">
                    Responsable: <b>${r.usuarioNombre}</b> (${r.rolUsuario}) | Asistentes: <b>${r.cantidadAsistentes} personas</b> | Motivo: ${r.motivo}
                </p>
                <p class="text-slate-500">Implementos asociados: ${r.implementosIncluidos || 'Estándar del salón'}</p>
            </div>
            <div class="flex items-center gap-2">
                ${puedeReubicarIa ? `
                    <button data-action="reubicar-ia" data-id="${r.id}"
                            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold">
                        ${Icons.sparkles}
                        <span>Reportar Falla y Reubicar con IA</span>
                    </button>
                ` : ''}
                ${puedeEliminar ? `
                    <button data-action="cancelar-reserva" data-id="${r.id}"
                            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 text-xs font-semibold">
                        ${Icons.trash}
                        <span>Cancelar</span>
                    </button>
                ` : `<span class="text-[11px] text-slate-400 font-medium">Solo lectura</span>`}
            </div>
        `;
        contenedor.appendChild(item);
    });

    contenedor.onclick = async (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const id = Number(btn.dataset.id);
        const action = btn.dataset.action;

        try {
            if (action === 'cancelar-reserva') {
                await apiRequest(`/api/reservas/${id}`, {
                    method: 'DELETE',
                    loadingMessage: 'Cancelando reserva...'
                });
                showToast('Reserva o solicitud eliminada correctamente.');
                await onDataUpdated();
            } else if (action === 'reubicar-ia') {
                const falla = prompt('Describe la falla técnica ocurrida en el salón durante tu clase para que la IA traslade el grupo:');
                if (!falla) return;
                const res = await apiRequest(`/api/reservas/${id}/reubicar-ia`, {
                    method: 'POST',
                    loadingMessage: 'La IA está clasificando la falla y buscando un salón válido...',
                    body: JSON.stringify({ descripcionFalla: falla })
                });
                showToast(`Reubicación con IA completada: Grupo trasladado de ${res.salonAnterior} a ${res.nuevoSalon}. ${res.explicacionIa || ''}`);
                await onDataUpdated();
            }
        } catch (err) {
            showToast(err.message, true);
        }
    };
}

export function inicializarModuloReservas(getUsuarioActual, onDataUpdated) {
    const selectSalon = document.getElementById('reservaEspacioId');
    if (selectSalon) {
        selectSalon.addEventListener('change', actualizarDetalleImplementosReserva);
    }

    const form = document.getElementById('formReserva');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const espacioId = document.getElementById('reservaEspacioId').value;
                const fecha = document.getElementById('reservaFecha').value;
                const horaInicio = document.getElementById('reservaHoraInicio').value;
                const horaFin = document.getElementById('reservaHoraFin').value;
                const cantidadAsistentes = Number(document.getElementById('reservaAsistentes').value);
                const motivo = document.getElementById('reservaMotivo').value;

                const salon = getSalonesCache().find(s => String(s.id) === String(espacioId));
                if (salon) {
                    if (salon.estado === 'BLOQUEADO') {
                        throw new Error(`Regla de Integridad: El salón '${salon.nombre}' se encuentra BLOQUEADO y no puede reservarse hasta ser habilitado.`);
                    }
                    if (cantidadAsistentes > salon.capacidad) {
                        throw new Error(`Regla de Aforo Máximo: Solicitas espacio para ${cantidadAsistentes} personas, superando la capacidad máxima física de '${salon.nombre}' (${salon.capacidad} personas).`);
                    }
                }
                if (horaInicio >= horaFin) {
                    throw new Error(`Horario inválido: La hora de inicio (${horaInicio}) debe ser anterior a la hora de finalización (${horaFin}).`);
                }

                const res = await apiRequest('/api/reservas', {
                    method: 'POST',
                    loadingMessage: 'Validando aforo, solape y estado del salón...',
                    body: JSON.stringify({
                        espacioId,
                        fecha,
                        horaInicio,
                        horaFin,
                        cantidadAsistentes,
                        motivo
                    })
                });
                showToast(
                    res.estado === 'PENDIENTE'
                        ? 'Solicitud registrada en estado PENDIENTE y enviada al módulo de aprobación del Administrador.'
                        : `Reserva confirmada en ${res.espacioNombre} (${res.horaInicio} - ${res.horaFin}).`
                );
                await onDataUpdated();
            } catch (err) {
                showToast(err.message, true);
            }
        });
    }

    const btnSugerirIa = document.getElementById('btnRecomendarSalonIa');
    if (btnSugerirIa) {
        btnSugerirIa.addEventListener('click', async () => {
            try {
                const asistentes = document.getElementById('reservaAsistentes').value;
                const fecha = document.getElementById('reservaFecha').value;
                const horaInicio = document.getElementById('reservaHoraInicio').value;
                const horaFin = document.getElementById('reservaHoraFin').value;

                const data = await apiRequest('/api/ia/recomendar-salon', {
                    method: 'POST',
                    loadingMessage: 'Consultando modelo de IA y validando reglas de aforo...',
                    body: JSON.stringify({ cantidadAsistentes: asistentes, fecha, horaInicio, horaFin })
                });

                const fuente = data.fuente === 'MODELO_IA' ? 'Modelo IA (validado por reglas)' : 'Motor de reglas local (API no disponible)';
                const texto = `${data.mensaje}\n\nFuente: ${fuente}`;
                document.getElementById('textoRecomendacionTrainerIa').textContent = texto;
                if (data.exito && data.espacioId) {
                    document.getElementById('reservaEspacioId').value = data.espacioId;
                    actualizarDetalleImplementosReserva();
                    showToast(texto);
                } else {
                    showToast(data.mensaje, true);
                }
            } catch (err) {
                showToast(err.message, true);
            }
        });
    }
}
