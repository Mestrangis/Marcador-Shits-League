const CACHE_NAME = 'marcador-v1';

const ASSETS_TO_CACHE = [
    './panel.html',
    './overlay.html',
    './sync.js',
    './jugadores.json',
    './manifest.json',
    './assets/icons/icon-192.png',
    './assets/icons/icon-512.png',
    './assets/images/fondo.png',
    './assets/images/caja_cronometro.png',
    './assets/images/caja_parte.png',
    './assets/images/caja_goleador_local.png',
    './assets/images/caja_goleador_visitante.png',
    './assets/images/escudo_local.png',
    './assets/images/escudo_visitante.png'
];

self.addEventListener('install', (e) => {
    e.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(ASSETS_TO_CACHE))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (e) => {
    e.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
        ).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (e) => {
    const url = new URL(e.request.url);

    if (url.origin !== location.origin) return;

    e.respondWith(
        caches.match(e.request).then(cached => {
            if (cached) {
                fetch(e.request).then(fresh => {
                    if (fresh && fresh.status === 200) {
                        caches.open(CACHE_NAME).then(cache => cache.put(e.request, fresh));
                    }
                }).catch(() => {});
                return cached;
            }
            return fetch(e.request).then(resp => {
                if (resp && resp.status === 200) {
                    const clone = resp.clone();
                    caches.open(CACHE_NAME).then(cache => cache.put(e.request, clone));
                }
                return resp;
            });
        })
    );
});
