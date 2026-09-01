/*
 * JavaURL frontend controller.
 */
(function () {
    'use strict';

    var state = { page: 0, size: 10, totalPages: 1 };

    function $(id) { return document.getElementById(id); }

    function show(el) { el.hidden = false; }
    function hide(el) { el.hidden = true; }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function formatDateTime(iso) {
        if (!iso) return 'Never';
        var d = new Date(iso);
        return isNaN(d.getTime()) ? iso : d.toLocaleString();
    }

    function fieldErrorsText(err) {
        if (!err.fieldErrors) return '';
        var parts = Object.keys(err.fieldErrors).map(function (k) {
            return k + ': ' + err.fieldErrors[k];
        });
        return parts.length ? ' (' + parts.join('; ') + ')' : '';
    }

    function showError(el, message) {
        el.textContent = message;
        show(el);
    }

    function validateLongUrl(value) {
        if (!value) return 'Destination URL is required.';
        try {
            var u = new URL(value);
            if (u.protocol !== 'http:' && u.protocol !== 'https:') {
                return 'Only http(s) URLs are supported.';
            }
            if (!u.hostname) return 'The URL must include a host, e.g. https://example.com.';
            return null;
        } catch (e) {
            return 'That does not look like a valid absolute http(s) URL.';
        }
    }

    /* ---------- create ---------- */

    async function onSubmit(event) {
        event.preventDefault();
        hide($('result'));
        hide($('form-error'));

        var form = event.target;
        var longUrl = form.longUrl.value.trim();
        var urlProblem = validateLongUrl(longUrl);
        if (urlProblem) return showError($('form-error'), urlProblem);

        var payload = { longUrl: longUrl };
        var alias = form.customAlias.value.trim();
        if (alias) payload.customAlias = alias;
        var days = form.expiresInDays.value;
        if (days) payload.expiresInDays = Number(days);

        $('submit-btn').disabled = true;
        try {
            var created = await window.JavaURLApi.createShortUrl(payload);
            renderResult(created);
            form.reset();
            loadPage(0);
        } catch (err) {
            showError($('form-error'), err.message + fieldErrorsText(err));
        } finally {
            $('submit-btn').disabled = false;
        }
    }

    function renderResult(created) {
        var link = $('result-url');
        link.textContent = created.shortUrl;
        link.href = created.shortUrl;
        show($('result'));

        $('copy-btn').onclick = function () {
            navigator.clipboard.writeText(created.shortUrl).then(function () {
                $('copy-btn').textContent = 'Copied ✓';
                setTimeout(function () { $('copy-btn').textContent = 'Copy'; }, 1500);
            }).catch(function () {
                window.prompt('Copy this link:', created.shortUrl);
            });
        };
        $('test-btn').onclick = function () {
            window.open(created.shortUrl, '_blank', 'noopener');
        };
    }

    /* ---------- list ---------- */

    function statusOf(item) {
        if (!item.active) return { label: 'Deleted', css: 'deleted' };
        if (item.expiresAt && new Date(item.expiresAt) < new Date()) return { label: 'Expired', css: 'expired' };
        return { label: 'Active', css: 'active' };
    }

    function renderTable(data) {
        var meta = data.page || {};
        state.totalPages = meta.totalPages || 1;
        var rows = data.content.map(function (item) {
            var st = statusOf(item);
            var display = item.shortUrl.replace(/^https?:\/\//, '');
            return '<tr data-code="' + escapeHtml(item.shortCode) + '">' +
                '<td><a class="link" href="' + escapeHtml(item.shortUrl) + '" target="_blank" rel="noopener">' +
                escapeHtml(display) + '</a></td>' +
                '<td><span class="dest">' + escapeHtml(item.longUrl) + '</span></td>' +
                '<td>' + escapeHtml(formatDateTime(item.createdAt)) + '</td>' +
                '<td>' + escapeHtml(item.expiresAt ? formatDateTime(item.expiresAt) : 'Never') + '</td>' +
                '<td class="clicks-cell" data-clicks>' + item.clickCount + '</td>' +
                '<td><span class="chip ' + st.css + '">' + st.label + '</span></td>' +
                '<td class="actions">' +
                '<button class="ghost small act-stats">Stats</button> ' +
                '<button class="ghost small danger act-delete">Delete</button>' +
                '</td></tr>';
        });
        $('links-body').innerHTML = rows.length ? rows.join('') :
            '<tr><td colspan="7" class="muted">No links yet — shorten your first URL above.</td></tr>';

        Array.prototype.forEach.call($('links-body').querySelectorAll('tr[data-code]'), function (tr) {
            var code = tr.getAttribute('data-code');
            tr.querySelector('.act-stats').addEventListener('click', function () { openStats(code); });
            tr.querySelector('.act-delete').addEventListener('click', function () { onDelete(code); });
        });

        $('page-info').textContent = 'Page ' + ((meta.number || 0) + 1) + ' / ' +
            Math.max(state.totalPages, 1) + ' · ' + (meta.totalElements || 0) + ' links';
        $('prev-page').disabled = (meta.number || 0) <= 0;
        $('next-page').disabled = (meta.number || 0) >= state.totalPages - 1;
    }

    async function loadPage(page) {
        hide($('list-error'));
        try {
            var data = await window.JavaURLApi.list(page, state.size);
            state.page = page;
            renderTable(data);
        } catch (err) {
            showError($('list-error'), err.message);
            $('links-body').innerHTML = '<tr><td colspan="7" class="muted">Could not load links.</td></tr>';
        }
    }

    async function onDelete(code) {
        if (!window.confirm('Deactivate "' + code + '"? It will stop redirecting (410 Gone).')) return;
        try {
            await window.JavaURLApi.remove(code);
            loadPage(state.page);
        } catch (err) {
            showError($('list-error'), err.message);
        }
    }

    /* ---------- live stats panel ---------- */

    var pollTimer = null;
    var liveHandle = null;

    function setClicks(code, count, bump) {
        var el = $('stats-clicks');
        el.textContent = count;
        if (bump) {
            el.classList.add('bump');
            setTimeout(function () { el.classList.remove('bump'); }, 160);
        }
        var row = document.querySelector('tr[data-code="' + CSS.escape(code) + '"] [data-clicks]');
        if (row) row.textContent = count;
    }

    function stopLive() {
        if (liveHandle) { liveHandle.close(); liveHandle = null; }
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    }

    function closeStats() {
        stopLive();
        hide($('stats-panel'));
    }

    async function openStats(code) {
        stopLive();
        show($('stats-panel'));

        try {
            var stats = await window.JavaURLApi.stats(code);
            $('stats-code').textContent = stats.shortCode;
            $('stats-longurl').textContent = stats.longUrl;
            $('stats-created').textContent = formatDateTime(stats.createdAt);
            $('stats-expires').textContent = stats.expiresAt ? formatDateTime(stats.expiresAt) : 'Never';
            var st = statusOf(stats);
            $('stats-active').innerHTML = '<span class="chip ' + st.css + '">' + st.label + '</span>';
            setClicks(stats.shortCode, stats.clickCount, false);
        } catch (err) {
            $('live-note').textContent = err.message;
            return;
        }

        liveHandle = window.JavaURLWs.watch(code,
            function (totalClicks) { setClicks(code, totalClicks, true); },
            function (status) {
                $('live-note').textContent = ({
                    connected: '● live — updates in real time via WebSocket',
                    connecting: 'connecting to live updates…',
                    reconnecting: 'reconnecting…',
                    error: 'WebSocket error — falling back to polling',
                    unsupported: 'STOMP library unavailable — polling every 5 s'
                })[status] || status;
                if (status === 'error' || status === 'unsupported') startPolling(code);
            });
    }

    function startPolling(code) {
        if (pollTimer) return;
        pollTimer = setInterval(async function () {
            try {
                var stats = await window.JavaURLApi.stats(code);
                setClicks(code, stats.clickCount, false);
            } catch (ignore) { /* transient network issue */ }
        }, 5000);
    }

    /* ---------- boot ---------- */

    document.addEventListener('DOMContentLoaded', function () {
        $('shorten-form').addEventListener('submit', onSubmit);
        $('prev-page').addEventListener('click', function () { loadPage(Math.max(0, state.page - 1)); });
        $('next-page').addEventListener('click', function () { loadPage(Math.min(state.totalPages - 1, state.page + 1)); });
        $('refresh-btn').addEventListener('click', function () { loadPage(state.page); });
        $('close-stats').addEventListener('click', closeStats);
        $('api-base-hint').textContent =
            (window.JavaURLConfig.apiBase || window.location.origin) + '/api/v1/urls';
        loadPage(0);
    });
})();