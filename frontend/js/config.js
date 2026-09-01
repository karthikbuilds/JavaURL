/*
 * Runtime configuration.
 * The API/WebSocket base URLs are auto-detected:
 *  - UI served by the backend itself (port 8080)  -> same-origin requests
 *  - UI served elsewhere (dev server on :3000, file://, other hosts) -> http://<host>:8080
 * Override by defining window.JAVAURL_CONFIG_OVERRIDE = { apiBase: "...", wsBase: "..." } before these scripts load.
 */
(function () {
    'use strict';

    var loc = window.location;
    var override = window.JAVAURL_CONFIG_OVERRIDE;

    if (override && override.apiBase !== undefined) {
        window.JavaURLConfig = { apiBase: override.apiBase, wsBase: override.wsBase || deriveWs(override.apiBase) };
        return;
    }

    function deriveWs(httpBase) {
        var wsProto = httpBase.indexOf('https:') === 0 ? 'wss:' : 'ws:';
        // Slice from "//" (not the first ":" which belongs to the scheme).
        return wsProto + httpBase.substring(httpBase.indexOf('//'));
    }

    var port = loc.port;
    var onBackendPort = port === '8080';
    var host = loc.hostname || 'localhost'; // file:// has an empty hostname
    var httpProto = loc.protocol === 'file:' ? 'http:' : loc.protocol; // never build "file://" API URLs
    var httpBase = onBackendPort ? '' : httpProto + '//' + host + ':8080';

    window.JavaURLConfig = {
        apiBase: httpBase,
        wsBase: onBackendPort ? (loc.protocol === 'https:' ? 'wss://' : 'ws://') + loc.host : deriveWs(httpBase)
    };
})();