/*
 * Live click analytics over STOMP.
 *
 * The backend exposes a SockJS endpoint at /ws; SockJS-compatible servers accept
 * a plain WebSocket on the "/ws/websocket" transport path, which lets us talk STOMP
 * without pulling in the SockJS shim. If the STOMP library failed to load (offline),
 * consumers should fall back to polling — see watch()'s status callback.
 */
(function () {
    'use strict';

    var client = null;
    var connected = false;
    var subscriptions = new Map();   // code -> stomp subscription
    var handlers = new Map();        // code -> Set<fn(totalClicks, clickedAt)>
    var statusListeners = new Set();

    function notifyStatus(status) {
        statusListeners.forEach(function (fn) {
            try { fn(status); } catch (ignore) { /* listener error must not break others */ }
        });
    }

    function subscribeTopic(code) {
        if (subscriptions.has(code)) return;
        subscriptions.set(code, client.subscribe('/topic/clicks/' + encodeURIComponent(code), function (message) {
            var body;
            try { body = JSON.parse(message.body); } catch (ignore) { return; }
            var set = handlers.get(code);
            if (set) set.forEach(function (fn) { fn(body.totalClicks, body.clickedAt); });
        }));
    }

    function getClient() {
        if (client || typeof StompJs === 'undefined') return client;
        client = new StompJs.Client({
            brokerURL: window.JavaURLConfig.wsBase + '/ws/websocket',
            reconnectDelay: 4000,
            onConnect: function () {
                connected = true;
                notifyStatus('connected');
                handlers.forEach(function (_set, code) { subscribeTopic(code); });
            },
            onWebSocketClose: function () {
                if (connected) { connected = false; notifyStatus('reconnecting'); }
            },
            onStompError: function () { notifyStatus('error'); }
        });
        client.activate();
        return client;
    }

    /**
     * Watch live clicks for a short code.
     * @param code short code
     * @param onUpdate fn(totalClicks, clickedAtIso)
     * @param onStatus fn('connecting'|'connected'|'reconnecting'|'error'|'unsupported')
     * @returns handle with close()
     */
    function watch(code, onUpdate, onStatus) {
        if (onStatus) statusListeners.add(onStatus);
        if (!handlers.has(code)) handlers.set(code, new Set());
        handlers.get(code).add(onUpdate);

        var c = getClient();
        if (!c) {
            if (onStatus) onStatus('unsupported');
        } else if (connected) {
            subscribeTopic(code);
            if (onStatus) onStatus('connected');
        } else if (onStatus) {
            onStatus('connecting');
        }

        return {
            close: function () {
                var set = handlers.get(code);
                if (set) {
                    set.delete(onUpdate);
                    if (set.size === 0) {
                        var sub = subscriptions.get(code);
                        if (sub) { try { sub.unsubscribe(); } catch (ignore) {} }
                        subscriptions.delete(code);
                        handlers.delete(code);
                    }
                }
                if (onStatus) statusListeners.delete(onStatus);
            }
        };
    }

    window.JavaURLWs = { watch: watch };
})();