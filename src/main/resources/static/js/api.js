// Shared helpers for the boutique frontend.
// Every page uses these - one place for fetch logic, login state and money.

const API = {
  // Call any backend endpoint. Throws an Error with the backend's
  // message on failure (e.g. "Not enough stock for ...").
  async request(path, options = {}) {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('vshoba_token');
    if (token) headers['Authorization'] = 'Bearer ' + token;

    let res;
    try {
      res = await fetch(path, { ...options, headers });
    } catch {
      // The request never reached the server (offline, network error, timeout,
      // or the free-tier site waking from a 15-min sleep). This is NOT a
      // 401 - show a distinct message so we don't misread it as a login issue.
      throw new Error('Cannot reach the server. It may still be waking up from the free-tier sleep - try again in a few seconds.');
    }

    if (res.status === 204) return null;

    const data = await res.json().catch(() => null);
    if (!res.ok) {
      // A real "not logged in" means the stored token is dead (expired, or
      // signed under an old JWT secret). Discard it so the user can log
      // in fresh instead of retrying a broken token forever on this page.
      if (res.status === 401 && data && data.message === 'You are not logged in') {
        Session.clear();
      }
      throw new Error(data && data.message ? data.message : 'Something went wrong');
    }
    return data;
  },

  get(path) {
    return API.request(path);
  },

  post(path, body) {
    return API.request(path, { method: 'POST', body: JSON.stringify(body) });
  },

  put(path, body) {
    return API.request(path, { method: 'PUT', body: JSON.stringify(body) });
  },

  patch(path, body) {
    return API.request(path, { method: 'PATCH', body: JSON.stringify(body) });
  },

  del(path) {
    return API.request(path, { method: 'DELETE' });
  },
};

const Session = {
  save(auth) {
    localStorage.setItem('vshoba_token', auth.token);
    localStorage.setItem('vshoba_user', JSON.stringify({
      userId: auth.userId,
      fullName: auth.fullName,
      email: auth.email,
      role: auth.role,
    }));
  },

  user() {
    try {
      return JSON.parse(localStorage.getItem('vshoba_user'));
    } catch {
      return null;
    }
  },

  clear() {
    localStorage.removeItem('vshoba_token');
    localStorage.removeItem('vshoba_user');
  },
};

// ₹1,234.50 style money formatting
const formatINR = (n) => '₹' + Number(n).toLocaleString('en-IN', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

// Cart lives in the browser (localStorage), not the server - it becomes
// an Order only at checkout.
const Cart = {
  key: 'vshoba_cart',

  get() {
    try {
      return JSON.parse(localStorage.getItem(Cart.key)) || [];
    } catch {
      return [];
    }
  },

  save(items) {
    localStorage.setItem(Cart.key, JSON.stringify(items));
  },

  count() {
    return Cart.get().reduce((sum, i) => sum + i.quantity, 0);
  },

  total() {
    return Cart.get().reduce((sum, i) => sum + i.unitPrice * i.quantity, 0);
  },

  add(product) {
    const items = Cart.get();
    const existing = items.find((i) => i.productId === product.id);
    if (existing) {
      existing.quantity += 1;
    } else {
      items.push({
        productId: product.id,
        name: product.name,
        unitPrice: product.price,
        imageUrl: product.imageUrl,
        stock: product.stockQuantity,
        quantity: 1,
      });
    }
    Cart.save(items);
  },

  setQuantity(productId, quantity) {
    const items = Cart.get();
    const item = items.find((i) => i.productId === productId);
    if (item) {
      if (quantity <= 0) {
        Cart.remove(productId);
        return;
      }
      item.quantity = Math.min(quantity, item.stock);
      Cart.save(items);
    }
  },

  remove(productId) {
    Cart.save(Cart.get().filter((i) => i.productId !== productId));
  },

  clear() {
    localStorage.removeItem(Cart.key);
  },
};

// Helper to render a product image or a placeholder.
const productImage = (p) =>
  p.imageUrl
    ? `<img src="${p.imageUrl}" alt="${p.name}" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'"><div class="no-img" style="display:none">${p.name[0]}</div>`
    : `<div class="no-img">${p.name[0]}</div>`;
