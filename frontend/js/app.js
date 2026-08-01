'use strict';

/* ============================================================
   E-Commerce Platform — tesztfelület
   Szolgáltatások base URL-jei (a gateway bevezetése után ide
   elég a http://localhost:8080-ra váltani mindet).
   ============================================================ */
const API = {
    user:         'http://localhost:8081',
    product:      'http://localhost:8082',
    cart:         'http://localhost:8083',
    order:        'http://localhost:8084',
    payment:      'http://localhost:8085',
    notification: 'http://localhost:8086',
};

const AUTH_KEY = 'ec_auth';

const fmtHUF = new Intl.NumberFormat('hu-HU', {
    style: 'currency', currency: 'HUF', maximumFractionDigits: 0,
});
const fmtDate = v => v ? new Date(v).toLocaleString('hu-HU') : '—';

const $ = id => document.getElementById(id);
const esc = s => String(s ?? '').replace(/[&<>"']/g,
    c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const asArray = (data, label) => {
    if (Array.isArray(data)) return data;
    const raw = typeof data === 'string' ? data : JSON.stringify(data);
    throw new Error(`${label} válasza nem lista: ${String(raw).slice(0, 200)}`);
};

/* ---------- Auth (sessionStorage) ---------- */
const getAuth = () => {
    try { return JSON.parse(sessionStorage.getItem(AUTH_KEY)); } catch { return null; }
};
const setAuth = a => sessionStorage.setItem(AUTH_KEY, JSON.stringify(a));
const clearAuth = () => sessionStorage.removeItem(AUTH_KEY);

/* ---------- Toast ---------- */
let toastTimer = null;
function toast(message, type = 'ok') {
    const t = $('toast');
    t.textContent = message;
    t.className = 'toast show ' + type;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.className = 'toast'; }, 3500);
}

/* ---------- API log panel ---------- */
function logCall(method, url, status, data) {
    const body = $('apiLogBody');
    if (body.firstElementChild && body.firstElementChild.classList.contains('log-entry')) {
        body.firstElementChild.style.borderTop = '1px solid var(--border)';
    }
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    const st = status ? ` <span class="log-status ${status < 400 ? 'ok' : 'err'}">${status}</span>` : '';
    const pretty = typeof data === 'string' ? data
        : JSON.stringify(data, null, 2) ?? '—';
    entry.innerHTML =
        `<span class="log-method ${method}">${method}</span><span class="log-url">${esc(url)}</span>${st}` +
        `<pre>${esc(pretty)}</pre>`;
    body.prepend(entry);
    while (body.children.length > 40) body.lastElementChild.remove();
}

/* ---------- API réteg ---------- */
async function api(base, path, { method = 'GET', body, params, auth = true } = {}) {
    const url = params
        ? base + path + '?' + new URLSearchParams(params).toString()
        : base + path;
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const session = getAuth();
    if (auth && session?.token) headers['Authorization'] = 'Bearer ' + session.token;

    let res;
    try {
        res = await fetch(url, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined,
        });
    } catch {
        logCall(method, url, null, 'HÁLÓZATI HIBA — a szolgáltatás nem érhető el');
        throw new Error(`A(z) ${base.replace('http://', '')} szolgáltatás nem érhető el. Indítsd el, majd próbáld újra!`);
    }
    const text = await res.text();
    let data = text;
    try { data = text ? JSON.parse(text) : null; } catch { /* szöveg marad */ }
    logCall(method, url, res.status, data);

    if (!res.ok) {
        const msg = (data && (data.message || data.error))
            || (typeof data === 'string' && data)
            || ('HTTP ' + res.status);
        throw new Error(msg);
    }
    return data;
}

/* ============================================================
   Általános UI
   ============================================================ */

function updateUserChip() {
    const session = getAuth();
    const chip = $('userChip');
    const logout = $('logoutBtn');
    if (session?.token) {
        chip.innerHTML = `<span class="chip-user"><b>${esc(session.firstName)} ${esc(session.lastName)}</b> &nbsp;(${esc(session.email)}, ID: ${session.userId ?? '?'})</span>`;
        logout.classList.remove('hidden');
    } else {
        chip.innerHTML = '<span class="chip-guest">Nincs bejelentkezve</span>';
        logout.classList.add('hidden');
    }
}

function requireAuth() {
    const session = getAuth();
    if (!session?.token) {
        toast('Ehhez a művelethez jelentkezz be először!', 'err');
        switchTab('auth');
        return null;
    }
    return session;
}

async function pingServices() {
    const session = getAuth();
    for (const [svc, base] of Object.entries(API)) {
        const dot = document.querySelector(`.status-dot[data-svc="${svc}"]`);
        dot.classList.remove('up', 'down');
        dot.classList.add('checking');
        dot.title = 'Ellenőrzés...';
        const ctrl = new AbortController();
        const timer = setTimeout(() => ctrl.abort(), 2500);
        try {
            const headers = {};
            if (session?.token) headers['Authorization'] = 'Bearer ' + session.token;
            const res = await fetch(base + '/', { method: 'GET', headers, signal: ctrl.signal });
            dot.classList.remove('checking');
            dot.classList.add('up');
            dot.title = `${base} — fut (HTTP ${res.status})`;
        } catch {
            dot.classList.remove('checking');
            dot.classList.add('down');
            dot.title = `${base} — nem fut`;
        } finally {
            clearTimeout(timer);
        }
    }
}

function switchTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(b =>
        b.classList.toggle('active', b.dataset.tab === tab));
    document.querySelectorAll('.view').forEach(v =>
        v.classList.toggle('active', v.id === 'tab-' + tab));

    if (tab === 'products') { loadCategories(true); loadProducts(); }
    if (tab === 'categories') { loadCategories(); }
    if (tab === 'cart') { loadCart(); }
    if (tab === 'orders') { loadOrders(); }
}

/* ============================================================
   FIÓK — bejelentkezés / regisztráció
   ============================================================ */
$('loginForm').addEventListener('submit', async e => {
    e.preventDefault();
    const f = e.target;
    try {
        const data = await api(API.user, '/api/auth/login', {
            method: 'POST',
            body: {
                email: f.email.value.trim(),
                password: f.password.value,
            },
            auth: false,
        });
        setAuth(data);
        updateUserChip();
        toast(`Üdv újra, ${data.firstName}!`);
        $('authInfo').textContent = JSON.stringify(data, null, 2);
        pingServices();
        switchTab('products');
    } catch (err) {
        toast(err.message, 'err');
    }
});

$('registerForm').addEventListener('submit', async e => {
    e.preventDefault();
    const f = e.target;
    try {
        const data = await api(API.user, '/api/auth/register', {
            method: 'POST',
            body: {
                email: f.email.value.trim(),
                password: f.password.value,
                firstName: f.firstName.value.trim(),
                lastName: f.lastName.value.trim(),
            },
            auth: false,
        });
        setAuth(data);
        updateUserChip();
        toast(`Sikeres regisztráció, ${data.firstName}!`);
        $('authInfo').textContent = JSON.stringify(data, null, 2);
        pingServices();
        switchTab('products');
    } catch (err) {
        toast(err.message, 'err');
    }
});

$('logoutBtn').addEventListener('click', () => {
    clearAuth();
    updateUserChip();
    $('authInfo').textContent = 'Kijelentkeztél.';
    toast('Kijelentkezve.');
    pingServices();
    switchTab('auth');
});

/* ============================================================
   TERMÉKEK
   ============================================================ */
let categoriesCache = [];

async function loadCategories(fillSelect = false) {
    try {
        const cats = await api(API.product, '/api/categories');
        categoriesCache = cats;

        const select = $('productCategoryFilter');
        const current = select.value;
        select.innerHTML = '<option value="">Minden kategória</option>' +
            cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
        select.value = current;

        $('categoryTable').innerHTML = cats.map(c => `
            <tr>
                <td>${c.id}</td>
                <td><b>${esc(c.name)}</b></td>
                <td>${esc(c.description ?? '—')}</td>
                <td><span class="badge ${c.active ? 'badge-delivered' : 'badge-cancelled'}">${c.active ? 'Aktív' : 'Inaktív'}</span></td>
            </tr>`).join('') || '<tr><td colspan="4" class="muted">Nincs kategória.</td></tr>';
    } catch (err) {
        toast(err.message, 'err');
    }
}

$('categoryForm').addEventListener('submit', async e => {
    e.preventDefault();
    const f = e.target;
    if (!requireAuth()) return;
    try {
        await api(API.product, '/api/categories', {
            method: 'POST',
            body: {
                name: f.name.value.trim(),
                description: f.description.value.trim(),
                imageUrl: f.imageUrl.value.trim(),
            },
        });
        toast('Kategória létrehozva!');
        f.reset();
        loadCategories();
    } catch (err) {
        toast(err.message, 'err');
    }
});

$('productSearchBtn').addEventListener('click', loadProducts);
$('productSearch').addEventListener('keydown', e => { if (e.key === 'Enter') loadProducts(); });
$('productCategoryFilter').addEventListener('change', loadProducts);
$('productRefreshBtn').addEventListener('click', () => { loadCategories(true); loadProducts(); });

async function loadProducts() {
    const params = {};
    const catId = $('productCategoryFilter').value;
    const name = $('productSearch').value.trim();
    if (catId) params.categoryId = catId;
    else if (name) params.name = name;
    try {
        const products = await api(API.product, '/api/products', { params });
        renderProducts(asArray(products, 'GET /api/products'));
    } catch (err) {
        toast(err.message, 'err');
    }
}

function renderProducts(products) {
    const grid = $('productGrid');
    if (!products.length) {
        grid.innerHTML = '<div class="card muted">Nincs találat.</div>';
        return;
    }
    grid.innerHTML = products.map(p => {
        const stockBadge = p.stock > 10
            ? `<span class="badge badge-stock-ok">Raktáron: ${p.stock}</span>`
            : p.stock > 0
                ? `<span class="badge badge-stock-low">Csak ${p.stock} db</span>`
                : '<span class="badge badge-stock-out">Nincs készleten</span>';
        return `
        <div class="product-card">
            <div class="product-img" data-name="${esc(p.name)}"></div>
            <div class="product-body">
                <div class="product-name">${esc(p.name)}</div>
                <div class="product-man">${esc(p.manufacturer ?? '')} · ${esc(p.sku ?? '')}</div>
                <div class="product-row">
                    <span class="product-price">${fmtHUF.format(p.price)}</span>
                    ${stockBadge}
                </div>
                <div class="product-add">
                    <input type="number" min="1" max="${p.stock || 1}" value="1"
                        data-cart-qty="${p.id}">
                    <button class="btn btn-primary" data-add-cart="${p.id}">Kosárba</button>
                </div>
            </div>
        </div>`;
    }).join('');

    grid.querySelectorAll('[data-name]').forEach(container => {
        const name = container.dataset.name;
        const img = new Image();
        const p = products.find(x => x.name === name);
        img.onload = () => container.appendChild(img);
        img.onerror = () => {
            const fb = document.createElement('div');
            fb.className = 'img-fallback';
            fb.textContent = (name.trim()[0] || '?').toUpperCase();
            container.appendChild(fb);
        };
        img.alt = name;
        if (p?.imageUrl) img.src = p.imageUrl;
        else img.onerror();
    });

    grid.querySelectorAll('[data-add-cart]').forEach(btn => {
        btn.addEventListener('click', () => {
            const session = requireAuth();
            if (!session) return;
            const qty = Number(grid.querySelector(`[data-cart-qty="${btn.dataset.addCart}"]`).value) || 1;
            addToCart(session.userId, btn.dataset.addCart, qty);
        });
    });
}

async function addToCart(userId, productId, quantity) {
    try {
        await api(API.cart, '/api/cart/add', {
            method: 'POST',
            params: { userId, productId, quantity },
        });
        toast('A kosárba került!');
    } catch (err) {
        toast(err.message, 'err');
    }
}

/* ============================================================
   KOSÁR
   ============================================================ */
async function loadCart() {
    const session = requireAuth();
    if (!session) return;
    try {
        const [items, products] = await Promise.all([
            api(API.cart, '/api/cart', { params: { userId: session.userId } }),
            api(API.product, '/api/products'),
        ]);
        const byId = Object.fromEntries(products.map(p => [p.id, p]));
        renderCart(items, byId);
    } catch (err) {
        toast(err.message, 'err');
    }
}

function renderCart(items, byId) {
    const tbody = $('cartTable');
    if (!items.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="muted">A kosár üres.</td></tr>';
        $('cartTotal').textContent = 'Összesen: 0 Ft';
        return;
    }
    tbody.innerHTML = items.map(it => {
        const p = byId[it.productId];
        return `
        <tr>
            <td><b>${esc(p?.name ?? `Termék #${it.productId}`)}</b><br>
                <span class="muted">ID: ${it.productId}</span></td>
            <td>${p ? fmtHUF.format(p.price) : '—'}</td>
            <td>
                <div class="qty-wrap">
                    <button class="qty-btn" data-qty-dec="${it.id}">−</button>
                    <span>${it.quantity}</span>
                    <button class="qty-btn" data-qty-inc="${it.id}">+</button>
                </div>
            </td>
            <td>${p ? fmtHUF.format(p.price * it.quantity) : '—'}</td>
            <td><button class="btn btn-danger btn-sm" data-cart-remove="${it.id}">Törlés</button></td>
        </tr>`;
    }).join('');
    const total = items.reduce((s, it) => s + (byId[it.productId]?.price ?? 0) * it.quantity, 0);
    $('cartTotal').textContent = 'Összesen: ' + fmtHUF.format(total);

    tbody.querySelectorAll('[data-qty-inc]').forEach(b =>
        b.addEventListener('click', () => updateQty(b.dataset.qtyInc, +1)));
    tbody.querySelectorAll('[data-qty-dec]').forEach(b =>
        b.addEventListener('click', () => updateQty(b.dataset.qtyDec, -1)));
    tbody.querySelectorAll('[data-cart-remove]').forEach(b =>
        b.addEventListener('click', () => removeCartItem(b.dataset.cartRemove)));
}

async function updateQty(cartItemId, delta) {
    try {
        const items = await api(API.cart, '/api/cart', { params: { userId: getAuth().userId } });
        const it = items.find(x => x.id == cartItemId);
        const qty = Math.max(1, (it?.quantity ?? 1) + delta);
        await api(API.cart, `/api/cart/${cartItemId}`, { method: 'PUT', params: { quantity: qty } });
        loadCart();
    } catch (err) {
        toast(err.message, 'err');
    }
}

async function removeCartItem(cartItemId) {
    try {
        await api(API.cart, `/api/cart/${cartItemId}`, { method: 'DELETE' });
        toast('Tétel törölve a kosárból.');
        loadCart();
    } catch (err) {
        toast(err.message, 'err');
    }
}

$('cartClearBtn').addEventListener('click', async () => {
    const session = requireAuth();
    if (!session) return;
    try {
        await api(API.cart, '/api/cart/clear', { method: 'DELETE', params: { userId: session.userId } });
        toast('A kosár kiürítve.');
        loadCart();
    } catch (err) {
        toast(err.message, 'err');
    }
});
$('cartRefreshBtn').addEventListener('click', loadCart);

/* ============================================================
   RENDELÉSEK
   ============================================================ */
$('orderForm').addEventListener('submit', async e => {
    e.preventDefault();
    const session = requireAuth();
    if (!session) return;
    const f = e.target;
    try {
        await api(API.order, '/api/orders', {
            method: 'POST',
            params: { userId: session.userId },
            body: {
                shippingAddress: f.shippingAddress.value.trim(),
                shippingCity: f.shippingCity.value.trim(),
                shippingState: f.shippingState.value.trim(),
                shippingZipCode: f.shippingZipCode.value.trim(),
                notes: f.notes.value.trim(),
            },
        });
        toast('Rendelés leadva!');
        f.reset();
        loadOrders();
    } catch (err) {
        toast(err.message, 'err');
    }
});
$('ordersRefreshBtn').addEventListener('click', loadOrders);

async function loadOrders() {
    const session = requireAuth();
    if (!session) return;
    try {
        const [orders, products] = await Promise.all([
            api(API.order, '/api/orders', { params: { userId: session.userId } }),
            api(API.product, '/api/products'),
        ]);
        const byId = Object.fromEntries(products.map(p => [p.id, p]));
        renderOrders(orders, byId);
    } catch (err) {
        toast(err.message, 'err');
    }
}

function renderOrders(orders, byId) {
    const box = $('ordersList');
    if (!orders.length) {
        box.innerHTML = '<div class="card muted">Még nincs rendelésed.</div>';
        return;
    }
    const statuses = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];
    box.innerHTML = orders.map(o => `
        <div class="order-item">
            <div class="order-head">
                <span class="order-id">Rendelés #${o.id}</span>
                <span class="badge badge-${o.status.toLowerCase()}">${o.status}</span>
                <span class="order-total">${fmtHUF.format(o.totalAmount)}</span>
                <span class="order-date">${fmtDate(o.createdAt)}</span>
            </div>
            ${o.orderItems?.length ? `
            <table class="table">
                <thead><tr><th>Termék</th><th>Menny.</th><th>Egységár</th><th>Összeg</th></tr></thead>
                <tbody>
                ${o.orderItems.map(oi => `
                    <tr>
                        <td>${esc(byId[oi.productId]?.name ?? `Termék #${oi.productId}`)}</td>
                        <td>${oi.quantity}</td>
                        <td>${fmtHUF.format(oi.unitPrice)}</td>
                        <td>${fmtHUF.format(oi.totalPrice)}</td>
                    </tr>`).join('')}
                </tbody>
            </table>` : ''}
            <p class="muted">Szállítás: ${esc(o.shippingAddress ?? '—')}, ${esc(o.shippingCity ?? '')} ${esc(o.shippingZipCode ?? '')} ${esc(o.shippingState ?? '')}</p>
            <div class="status-form mt">
                <select data-order-status="${o.id}">
                    ${statuses.map(s => `<option ${s === o.status ? 'selected' : ''}>${s}</option>`).join('')}
                </select>
                <button class="btn btn-sm" data-order-status-save="${o.id}">Státusz mentése</button>
            </div>
        </div>`).join('');

    box.querySelectorAll('[data-order-status-save]').forEach(btn =>
        btn.addEventListener('click', () => updateStatus(btn.dataset.orderStatusSave)));
}

async function updateStatus(orderId) {
    const select = document.querySelector(`[data-order-status="${orderId}"]`);
    try {
        await api(API.order, `/api/orders/${orderId}/status`, {
            method: 'PATCH',
            params: { status: select.value },
        });
        toast(`Rendelés #${orderId} státusza: ${select.value}`);
        loadOrders();
    } catch (err) {
        toast(err.message, 'err');
    }
}

/* ============================================================
   FIZETÉS
   ============================================================ */
$('paymentLoadBtn').addEventListener('click', async () => {
    const session = requireAuth();
    if (!session) return;
    const orderId = $('paymentOrderId').value.trim();
    if (!orderId) return toast('Add meg a rendelés azonosítóját!', 'err');
    try {
        const order = await api(API.order, `/api/orders/${orderId}`);
        $('paymentOrderInfo').textContent = JSON.stringify(order, null, 2);
        $('paymentForm').amount.value = order.totalAmount;
        toast('Rendelés betöltve.');
    } catch (err) {
        toast(err.message, 'err');
    }
});

$('paymentForm').addEventListener('submit', async e => {
    e.preventDefault();
    const session = requireAuth();
    if (!session) return;
    const f = e.target;
    const orderId = $('paymentOrderId').value.trim();
    if (!orderId) return toast('Add meg a rendelés azonosítóját!', 'err');
    try {
        const result = await api(API.payment, `/api/payments/${orderId}`, {
            method: 'POST',
            body: {
                cardNumber: f.cardNumber.value.trim(),
                cardHolder: f.cardHolder.value.trim(),
                expiryDate: f.expiryDate.value.trim(),
                cvv: f.cvv.value.trim(),
                amount: Number(f.amount.value),
            },
        });
        $('paymentResult').textContent = JSON.stringify(result, null, 2);
        toast(result.success ? 'Fizetés sikeres!' : 'Fizetés sikertelen', result.success ? 'ok' : 'err');
    } catch (err) {
        toast(err.message, 'err');
    }
});

/* ============================================================
   ÉRTESÍTÉSEK
   ============================================================ */
$('notificationSendBtn').addEventListener('click', async () => {
    const message = $('notificationMessage').value.trim();
    if (!message) return toast('Írj egy üzenetet!', 'err');
    try {
        const result = await api(API.notification, '/api/notifications/send', {
            method: 'POST',
            body: message,
        });
        $('notificationResult').textContent = String(result);
        toast('Értesítés elküldve.');
    } catch (err) {
        toast(err.message, 'err');
    }
});

/* ============================================================
   Indítás
   ============================================================ */
document.querySelectorAll('.tab-btn').forEach(btn =>
    btn.addEventListener('click', () => switchTab(btn.dataset.tab)));

$('apiLogClear').addEventListener('click', () => {
    $('apiLogBody').innerHTML = '<p class="muted">Még nem történt API hívás.</p>';
});
$('apiLogToggle').addEventListener('click', () => {
    $('apiLog').classList.toggle('collapsed');
    $('apiLogToggle').textContent = $('apiLog').classList.contains('collapsed') ? 'Kinyitás' : 'Összecsukás';
});

updateUserChip();
pingServices();
setInterval(pingServices, 30000);
