import { apiRequest, showToast } from '../api.js';
import { Icons } from '../icons.js';

export async function cargarDatosPrestamosCamper(usuarioActual, onDataUpdated) {
    const [catalogo, misPrestamos] = await Promise.all([
        apiRequest('/api/prestamos/catalogo', { loadingMessage: 'Cargando catálogo de préstamos...' }),
        apiRequest('/api/prestamos/mis-prestamos', { loadingMessage: 'Cargando tus préstamos...' })
    ]);

    const select = document.getElementById('camperSelectEquipo');
    if (select) {
        select.innerHTML = '';
        catalogo.forEach(eq => {
            const opt = document.createElement('option');
            opt.value = eq.id;
            opt.disabled = eq.prestadoActualmente;
            opt.textContent = `${eq.nombreImplemento} — ${eq.prestadoActualmente ? 'NO DISPONIBLE (En préstamo)' : 'DISPONIBLE'}`;
            select.appendChild(opt);
        });
    }

    const contenedor = document.getElementById('listaPrestamosCamper');
    if (!contenedor) return;
    contenedor.innerHTML = '';

    if (misPrestamos.length === 0) {
        contenedor.innerHTML = `
            <div class="p-6 text-center bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-500">
                No tienes préstamos activos ni historial registrado.
            </div>
        `;
        return;
    }

    misPrestamos.forEach(p => {
        const activo = p.estado === 'ACTIVO';
        const row = document.createElement('div');
        row.className = 'p-4 rounded-xl bg-white border border-slate-200 shadow-sm flex items-center justify-between gap-3 text-xs';
        row.innerHTML = `
            <div class="flex items-start gap-3">
                <div class="p-2 rounded-lg bg-slate-100 text-slate-700">
                    ${Icons.laptop}
                </div>
                <div>
                    <p class="font-bold text-slate-900">${p.nombreEquipo}</p>
                    <p class="text-slate-500 mt-0.5">Fecha préstamo: ${p.fechaHoraPrestamo}</p>
                    <span class="inline-block mt-1 px-2 py-0.5 rounded text-[10px] font-semibold ${
                        activo ? 'bg-amber-100 text-amber-800' : 'bg-emerald-50 text-emerald-700'
                    }">${p.estado}</span>
                </div>
            </div>
            ${activo ? `
                <button data-devolver-id="${p.id}"
                        class="px-3.5 py-2 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-semibold">
                    Registrar Devolución
                </button>
            ` : `<span class="text-emerald-700 font-semibold">Devuelto (${p.fechaHoraDevolucion || ''})</span>`}
        `;
        contenedor.appendChild(row);
    });

    contenedor.onclick = async (e) => {
        const btn = e.target.closest('button[data-devolver-id]');
        if (!btn) return;
        const prestamoId = Number(btn.dataset.devolverId);
        try {
            await apiRequest(`/api/prestamos/${prestamoId}/devolver`, { method: 'POST' });
            showToast('Devolución registrada. Tu cupo de préstamo individual ha quedado habilitado nuevamente.');
            await onDataUpdated();
        } catch (err) {
            showToast(err.message, true);
        }
    };
}

export function inicializarModuloCamper(getUsuarioActual, onDataUpdated) {
    const form = document.getElementById('formPrestamoCamper');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            const implementoId = document.getElementById('camperSelectEquipo').value;
            const cantidad = document.getElementById('camperCantidadEquipo').value;
            const res = await apiRequest('/api/prestamos', {
                method: 'POST',
                loadingMessage: 'Validando cupo de préstamo e inventario...',
                body: JSON.stringify({
                    implementoId,
                    cantidad
                })
            });
            showToast(`Préstamo registrado exitosamente: ${res.nombreEquipo}.`);
            await onDataUpdated();
        } catch (err) {
            showToast(err.message, true);
        }
    });
}
