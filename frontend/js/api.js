/*
 * Thin REST client for the JavaURL backend.
 * All errors are normalised into ApiError { status, message, fieldErrors }.
 */
(function () {
    'use strict';

    function ApiError(status, message, fieldErrors) {
        this.status = status;
        this.message = message;
        this.fieldErrors = fieldErrors || null;
    }
    ApiError.prototype = Object.create(Error.prototype);

    function base() {
        // Defensive: if config.js ever fails to load, degrade to same-origin instead of crashing.
        var cfg = window.JavaURLConfig || {};
        return cfg.apiBase || '';
    }

    async function request(path, options) {
        var res;
        try {
            res = await fetch(base() + path, Object.assign(
                { headers: { Accept: 'application/json' } }, options || {}));
        } catch (networkErr) {
            throw new ApiError(0, 'Cannot reach the API at "' + (base() || window.location.origin) +
                '" — is the backend running?', null);
        }
        if (res.status === 204) return null;

        var body = null;
        try { body = await res.json(); } catch (ignore) { /* non-JSON */ }

        if (!res.ok) {
            throw new ApiError(res.status,
                (body && body.message) || 'Request failed (' + res.status + ')',
                body && body.fieldErrors);
        }
        return body;
    }

    function jsonInit(method, payload) {
        return {
            method: method,
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(payload)
        };
    }

    window.JavaURLApi = {
        Error: ApiError,

        createShortUrl: function (payload) {
            return request('/api/v1/urls', jsonInit('POST', payload));
        },

        list: function (page, size) {
            return request('/api/v1/urls?page=' + encodeURIComponent(page) +
                '&size=' + encodeURIComponent(size));
        },

        stats: function (code) {
            return request('/api/v1/urls/' + encodeURIComponent(code));
        },

        remove: function (code) {
            return request('/api/v1/urls/' + encodeURIComponent(code), { method: 'DELETE' });
        }
    };
})();