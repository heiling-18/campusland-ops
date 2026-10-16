import { Icons } from './icons.js';

let authToken = null;
let loadingCount = 0;
let onUnauthorized = null;

export function setOnUnauthorized(handler) {
    onUnauthorized = handler;
}

export function setAuthToken(token) {
    authToken = token;
    if (token) {
        sessionStorage.setItem('campusland_jwt', token);
    } else {
        sessionStorage.removeItem('campusland_jwt');
    }
}

export function getAuthToken() {
    return authToken || sessionStorage.getItem('campusland_jwt');
}

function setLoading(visible) {
    const overlay = document.getElementById('globalLoading');
    if (!overlay) return;
    loadingCount = visible ? loadingCount + 1 : Math.max(0, loadingCount - 1);
    overlay.classList.toggle('hidden', loadingCount === 0);
    overlay.setAttribute('aria-busy', loadingCount > 0 ? 'true' : 'false');
}

export function setLoadingMessage(text) {
    const label = document.getElementById('globalLoadingLabel');
    if (label && text) label.textContent = text;
}

export async function apiRequest(url, options = {}) {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const showLoading = options.loading !== false;
    if (showLoading) {
        setLoadingMessage(options.loadingMessage || 'Procesando operación...');
        setLoading(true);
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers
        });

        const raw = await response.text();
        let data = {};
        if (raw) {
            try {
                data = JSON.parse(raw);
            } catch {
                throw new Error('El servidor devolvió una respuesta inesperada. Inténtalo de nuevo.');
            }
        }

        if (response.status === 401) {
            setAuthToken(null);
            if (typeof onUnauthorized === 'function') {
                onUnauthorized(data.mensaje || 'Sesión expirada. Inicia sesión de nuevo.');
            }
            throw new Error(data.mensaje || 'Sesión expirada. Inicia sesión de nuevo.');
        }
        if (!response.ok || data.error) {
            throw new Error(data.mensaje || data.error || 'Error procesando la solicitud en el servidor.');
        }
        if (data && data.token) {
            setAuthToken(data.token);
        }
        return data;
    } finally {
        if (showLoading) setLoading(false);
    }
}

export function showToast(message, isError = false) {
    const toast = document.getElementById('toastNotification');
    if (!toast) return;

    toast.innerHTML = `
        <div class="flex items-start gap-3">
            <span class="mt-0.5 shrink-0 ${isError ? 'text-red-600' : 'text-emerald-600'}">
                ${isError ? Icons.alert : Icons.check}
            </span>
            <div class="text-xs leading-relaxed font-medium text-slate-800">
                <p class="font-semibold ${isError ? 'text-red-700' : 'text-emerald-700'}">
                    ${isError ? 'Validación de regla de negocio' : 'Operación confirmada'}
                </p>
                <p class="mt-0.5 text-slate-600">${message}</p>
            </div>
        </div>
    `;
    toast.className = `fixed top-5 right-5 z-50 max-w-md bg-white px-4 py-3.5 rounded-lg shadow-xl border-l-4 border border-slate-200 transition-all ${
        isError ? 'border-l-red-600' : 'border-l-emerald-600'
    }`;
    toast.classList.remove('hidden');

    clearTimeout(window.__toastTimer);
    window.__toastTimer = setTimeout(() => {
        toast.classList.add('hidden');
    }, 5500);
}

export function initFormValidationBridge(root = document) {
    const dirtyState = new WeakMap();

    const syncAria = (el) => {
        if (!el || typeof el.checkValidity !== 'function') return;
        const isInvalid = el.matches?.(':user-invalid') || !el.checkValidity();
        if (isInvalid) {
            el.setAttribute('aria-invalid', 'true');
        } else {
            el.removeAttribute('aria-invalid');
        }
    };

    root.addEventListener('blur', (e) => {
        const el = e.target;
        if (el && el.matches?.('input[required], select[required], textarea[required]')) {
            if (!CSS.supports('selector(:user-invalid)')) {
                const st = dirtyState.get(el) || { interacted: false };
                if (st.interacted) {
                    el.classList.toggle('user-invalid-fallback', !el.checkValidity());
                }
            }
            syncAria(el);
        }
    }, true);

    root.addEventListener('input', (e) => {
        const el = e.target;
        if (el && el.matches?.('input[required], select[required], textarea[required]')) {
            dirtyState.set(el, { interacted: true });
            if (el.checkValidity()) {
                el.classList.remove('user-invalid-fallback');
                el.removeAttribute('aria-invalid');
            }
        }
    });
}
