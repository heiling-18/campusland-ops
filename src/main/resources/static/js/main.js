import { apiRequest, setAuthToken, showToast, initFormValidationBridge, setOnUnauthorized, setLoadingMessage } from './api.js';
import {
    cargarYRenderizarEspacios,
    inicializarFormularioCrudAdmin
} from './modules/espaciosModule.js';
import {
    cargarYRenderizarReservas,
    inicializarModuloReservas
} from './modules/reservasModule.js';
import {
    cargarDatosSoporte,
    cargarBandejaIncidenciasSoporte,
    inicializarModuloSoporte
} from './modules/soporteModule.js';
import {
    cargarDatosPrestamosCamper,
    inicializarModuloCamper
} from './modules/camperModule.js';
import { inicializarModuloIa } from './modules/iaModule.js';
import { inicializarEfectos3D } from './modules/fx3dModule.js';

let usuarioActual = null;

document.addEventListener('DOMContentLoaded', () => {
    const hoy = new Date().toISOString().split('T')[0];
    const inputFecha = document.getElementById('reservaFecha');
    if (inputFecha) inputFecha.value = hoy;

    initFormValidationBridge(document);
    inicializarTemaVisual();
    inicializarEfectos3D();

    document.getElementById('loginForm').addEventListener('submit', iniciarSesion);

    document.getElementById('btnCerrarSesion').addEventListener('click', cerrarSesion);
    document.getElementById('btnRefrescarDatos').addEventListener('click', sincronizarDatosRol);

    document.querySelectorAll('.nav-item[data-view]').forEach(btn => {
        btn.addEventListener('click', () => activarVista(btn.dataset.view));
    });

    const getUser = () => usuarioActual;
    inicializarFormularioCrudAdmin(getUser, sincronizarDatosRol);
    inicializarModuloReservas(getUser, sincronizarDatosRol);
    inicializarModuloSoporte(getUser, sincronizarDatosRol);
    inicializarModuloCamper(getUser, sincronizarDatosRol);
    inicializarModuloIa(getUser, sincronizarDatosRol);

    setOnUnauthorized((mensaje) => {
        cerrarSesion();
        showToast(mensaje || 'Sesión expirada. Inicia sesión de nuevo.', true);
    });
});

function inicializarTemaVisual() {
    const meta = document.querySelector('meta[name="color-scheme"]');
    if (!meta) return;

    document.querySelectorAll('[data-theme-toggle]').forEach(btn => {
        btn.addEventListener('click', () => {
            const actual = meta.content === 'light' ? 'light' : 'dark';
            const siguiente = actual === 'dark' ? 'light' : 'dark';
            meta.content = siguiente;
            try {
                localStorage.setItem('color-scheme', siguiente);
            } catch (e) {}
        });
    });

    window.addEventListener('storage', (e) => {
        if (e.key === 'color-scheme' && e.newValue) {
            meta.content = e.newValue;
        }
    });
}

async function iniciarSesion(e) {
    e.preventDefault();
    try {
        const correo = document.getElementById('loginCorreo').value;
        const password = document.getElementById('loginPassword').value;
        const authResponse = await apiRequest('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ correo, password }),
            loadingMessage: 'Validando credenciales...'
        });

        // Guardar JWT firmado en memoria y sessionStorage
        setAuthToken(authResponse.token);
        usuarioActual = authResponse;

        document.getElementById('loginView').classList.add('hidden');
        document.getElementById('appView').classList.remove('hidden');

        configurarNavegacionPorRol(usuarioActual);
        await sincronizarDatosRol();
        showToast(`Bienvenido, ${usuarioActual.nombre} (${usuarioActual.rol}).`);
    } catch (err) {
        showToast(err.message, true);
    }
}

function cerrarSesion() {
    usuarioActual = null;
    setAuthToken(null);
    document.getElementById('appView').classList.add('hidden');
    document.getElementById('loginView').classList.remove('hidden');
}

function configurarNavegacionPorRol(user) {
    document.getElementById('headerUserName').textContent = user.nombre;
    document.getElementById('headerUserEmail').textContent = user.correo;
    document.getElementById('headerRoleBadge').textContent = user.rol;

    const navReservas = document.getElementById('navReservas');
    const navAdmin = document.getElementById('navAdmin');
    const navSoporte = document.getElementById('navSoporte');
    const navCamper = document.getElementById('navCamper');
    const cajaRecomendadorIa = document.getElementById('cajaRecomendadorTrainerIa');
    const bloqueReporteInc = document.getElementById('bloqueReporteIncidencia');
    const kpiRowAdmin = document.getElementById('kpiRowAdmin');

    navReservas.classList.add('hidden');
    navAdmin.classList.add('hidden');
    navSoporte.classList.add('hidden');
    navCamper.classList.add('hidden');
    cajaRecomendadorIa.classList.add('hidden');
    bloqueReporteInc.classList.remove('hidden');
    if (kpiRowAdmin) kpiRowAdmin.classList.add('hidden');

    if (user.rol === 'ADMIN') {
        navReservas.classList.remove('hidden');
        navAdmin.classList.remove('hidden');
        if (kpiRowAdmin) kpiRowAdmin.classList.remove('hidden');
        document.getElementById('subtituloFormReserva').textContent =
            'Control Administrador: Reserva con aprobación directa respetando el aforo físico máximo y la disponibilidad horaria de cada salón.';
    } else if (user.rol === 'TRAINER') {
        navReservas.classList.remove('hidden');
        cajaRecomendadorIa.classList.remove('hidden');
        document.getElementById('subtituloFormReserva').textContent =
            'Solicitud sujeta a aprobación del Administrador. Aplica regla de mínimo 50% de ocupación (con excepción automática si los salones menores están llenos).';
    } else if (user.rol === 'SOPORTE') {
        navSoporte.classList.remove('hidden');
        bloqueReporteInc.classList.add('hidden');
    } else if (user.rol === 'CAMPER') {
        navReservas.classList.remove('hidden');
        navCamper.classList.remove('hidden');
        document.getElementById('subtituloFormReserva').textContent =
            'Catálogo exclusivo para Campers: Solicitud de salas de estudio de hasta 12 personas (Amarilla, Verde, Azul y Blanca) sujeta a aprobación del Administrador.';
    }

    activarVista('viewEspacios');
}

export function activarVista(viewId) {
    document.querySelectorAll('.workspace-view').forEach(v => v.classList.add('hidden'));
    const target = document.getElementById(viewId);
    if (target) target.classList.remove('hidden');

    document.querySelectorAll('.nav-item[data-view]').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.view === viewId);
    });
}

async function sincronizarDatosRol() {
    if (!usuarioActual) return;
    setLoadingMessage('Sincronizando catálogo oficial y operaciones...');
    await cargarYRenderizarEspacios(usuarioActual, sincronizarDatosRol);

    if (usuarioActual.rol === 'ADMIN' || usuarioActual.rol === 'TRAINER' || usuarioActual.rol === 'CAMPER') {
        await cargarYRenderizarReservas(usuarioActual, sincronizarDatosRol);
    }
    if (usuarioActual.rol === 'ADMIN') {
        await cargarBandejaIncidenciasSoporte(usuarioActual, sincronizarDatosRol);
    }
    if (usuarioActual.rol === 'SOPORTE') {
        await cargarDatosSoporte(usuarioActual, sincronizarDatosRol);
    }
    if (usuarioActual.rol === 'CAMPER') {
        await cargarDatosPrestamosCamper(usuarioActual, sincronizarDatosRol);
    }
}
