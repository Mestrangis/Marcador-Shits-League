// ============================================================
//  sync.js — Capa de sincronización vía Ably
//
//  API pública (estado general):
//    publicarEstado(estado)
//    leerEstado()
//    onEstadoActualizado(callback)
//
//  API cronómetro (comandos, no ticks):
//    publicarComandoCrono(comando)
//    onComandoCrono(callback)
//
//  API conexión:
//    onConexionCambiada(callback)
//    solicitarEstado()
//    onSolicitudEstado(callback)
// ============================================================

const ABLY_API_KEY = 'uCTK1Q.6yX8Eg:JOYqaUCKbdvjnC6OWYLh1PbFMmDssY-Swq_0g89www4';
const CHANNEL_NAME = 'marcador-futbol-shitsleague';
const STORAGE_KEY = 'marcador-estado';

let canal = null;
let ably = null;

const estadoCallbacks = [];
const conexionCallbacks = [];
const solicitudCallbacks = [];
const cronoCallbacks = [];

function cargarSDK() {
    return new Promise((resolve, reject) => {
        if (window.Ably) { resolve(); return; }
        const script = document.createElement('script');
        script.src = 'https://cdn.ably.com/lib/ably.min-2.js';
        script.onload = resolve;
        script.onerror = () => reject(new Error('No se pudo cargar el SDK de Ably'));
        document.head.appendChild(script);
    });
}

const inicializado = (async () => {
    try {
        await cargarSDK();

        ably = new window.Ably.Realtime({
            key: ABLY_API_KEY,
            echoMessages: false
        });
        canal = ably.channels.get(CHANNEL_NAME);

        ['connected', 'connecting', 'disconnected', 'suspended', 'closed', 'failed'].forEach(ev => {
            ably.connection.on(ev, () => conexionCallbacks.forEach(cb => cb(ev)));
        });

        canal.subscribe('estado', (msg) => {
            const estado = msg.data;
            localStorage.setItem(STORAGE_KEY, JSON.stringify(estado));
            estadoCallbacks.forEach(cb => cb(estado));
        });

        canal.subscribe('cronometro', (msg) => {
            cronoCallbacks.forEach(cb => cb(msg.data));
        });

        canal.subscribe('solicitar_estado', () => {
            solicitudCallbacks.forEach(cb => cb());
        });

    } catch (e) {
        console.error('sync.js: Error inicializando Ably:', e);
    }
})();

// --- Estado ---

export function publicarEstado(estado) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(estado));
    if (canal) canal.publish('estado', estado).catch(() => {});
}

export function leerEstado() {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
}

export function onEstadoActualizado(callback) {
    estadoCallbacks.push(callback);
}

// --- Cronómetro ---

export function publicarComandoCrono(comando) {
    if (canal) canal.publish('cronometro', comando).catch(() => {});
}

export function onComandoCrono(callback) {
    cronoCallbacks.push(callback);
}

// --- Conexión ---

export function onConexionCambiada(callback) {
    conexionCallbacks.push(callback);
    if (ably) {
        callback(ably.connection.state);
    } else {
        callback('connecting');
        inicializado.then(() => { if (ably) callback(ably.connection.state); });
    }
}

export function solicitarEstado() {
    if (canal) canal.publish('solicitar_estado', { ts: Date.now() }).catch(() => {});
}

export function onSolicitudEstado(callback) {
    solicitudCallbacks.push(callback);
}
