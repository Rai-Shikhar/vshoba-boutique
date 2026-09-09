// Shop page logic: load products, render grid, handle cart + auth modal.
// Bound to the new storefront design (home.css).

let allProducts = [];
let currentCategory = '';

document.addEventListener('DOMContentLoaded', () => {
  loadCategories();
  loadProducts();
  renderUserArea();
  renderCart();

  document.getElementById('cartBtn').addEventListener('click', toggleCart);
  document.getElementById('cartOverlay').addEventListener('click', closeCart);
  document.getElementById('checkoutBtn').addEventListener('click', goCheckout);

  document.getElementById('searchBox').addEventListener('input', applyFilters);

  document.getElementById('closeModal').addEventListener('click', () =>
    document.getElementById('modalOverlay').classList.remove('show'));
  document.getElementById('tabLogin').addEventListener('click', () => showTab('login'));
  document.getElementById('tabRegister').addEventListener('click', () => showTab('register'));
  document.getElementById('loginForm').addEventListener('submit', doLogin);
  document.getElementById('registerForm').addEventListener('submit', doRegister);
});

// Nav links are built from the REAL categories returned by the backend.
// Clicking one filters the grid; clicking again clears the filter.
async function loadCategories() {
  const nav = document.getElementById('navLinks');
  try {
    const cats = await API.get('/api/products/categories');
    buildNavLinks(nav, cats);
  } catch {
    // No categories available - leave nav empty rather than inventing links.
    nav.innerHTML = '';
  }
}

function buildNavLinks(nav, cats) {
  const make = (label, value) => {
    const a = document.createElement('a');
    a.textContent = label;
    a.href = '#grid';
    a.dataset.cat = value;
    if (value === currentCategory) a.classList.add('active');
    a.addEventListener('click', () => {
      currentCategory = (currentCategory === value) ? '' : value;
      nav.querySelectorAll('a').forEach((el) =>
        el.classList.toggle('active', el.dataset.cat === currentCategory));
      applyFilters();
    });
    nav.appendChild(a);
  };
  if (cats.length > 1 || currentCategory) make('All', '');
  cats.slice(0, 5).forEach((c) => make(c, c));
}

async function loadProducts() {
  try {
    allProducts = await API.get('/api/products');
    applyFilters();
  } catch (err) {
    document.getElementById('grid').innerHTML =
      `<p class="empty">Could not load products: ${err.message}</p>`;
  }
}

function applyFilters() {
  const term = document.getElementById('searchBox').value.trim().toLowerCase();
  const filtered = allProducts.filter((p) => {
    const matchesCategory = !currentCategory || p.category === currentCategory;
    const matchesTerm = !term ||
      p.name.toLowerCase().includes(term) ||
      (p.description && p.description.toLowerCase().includes(term));
    return matchesCategory && matchesTerm;
  });
  renderGrid(filtered);
}

function formatPrice(n) {
  const num = Number(n);
  return '₹' + num.toLocaleString('en-IN',
    Number.isInteger(num)
      ? { maximumFractionDigits: 0 }
      : { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Product photo with graceful glyph fallback (first letter of the name).
function cardVisual(p) {
  const glyph = `<span class="glyph"${p.imageUrl ? ' style="display:none"' : ''}>${(p.name[0] || '?').toUpperCase()}</span>`;
  const img = p.imageUrl
    ? `<img src="${p.imageUrl}" alt="${p.name}" loading="lazy"
         onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
    : '';
  const tag = p.category ? `<span class="tag">${p.category}</span>` : '';
  return `${img}${glyph}${tag}`;
}

function renderGrid(products) {
  const grid = document.getElementById('grid');
  if (!products.length) {
    grid.innerHTML = '<p class="empty">No pieces found.</p>';
    return;
  }
  grid.innerHTML = products.map((p, index) => {
    const inStock = p.stockQuantity > 0;
    return `
      <article class="card" style="animation-delay:${Math.min(index * 70, 700)}ms">
        <div class="card-visual">${cardVisual(p)}</div>
        <div class="card-info">
          <h3>${p.name}</h3>
          <p class="desc">${p.description}</p>
          <div class="price-row">
            <span class="price">${formatPrice(p.price)}</span>
            ${inStock
              ? `<button class="add" onclick="addToCart(${p.id}, this)">Add to cart</button>`
              : `<span class="badge-out">Out of stock</span>`}
          </div>
        </div>
      </article>`;
  }).join('');
}

// Adds a product to the cart (button feedback + toast).
async function addToCart(productId, button) {
  const product = allProducts.find((p) => p.id === productId);
  if (!product) return;

  Cart.add(product);
  renderCart();

  button.textContent = 'Added ✓';
  button.classList.add('added');
  button.disabled = true;
  setTimeout(() => {
    button.textContent = 'Add to cart';
    button.classList.remove('added');
    button.disabled = false;
  }, 1200);

  showToast(`${product.name} added to cart`);
}

function showToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => toast.classList.remove('show'), 2200);
}

// ---------- Cart ----------

function refreshCartCount() {
  const count = document.getElementById('cartCount');
  count.textContent = Cart.count();
}

function toggleCart() {
  const drawer = document.getElementById('cartDrawer');
  const open = drawer.classList.toggle('open');
  document.getElementById('cartOverlay').style.display = open ? 'block' : 'none';
}

function closeCart() {
  document.getElementById('cartDrawer').classList.remove('open');
  document.getElementById('cartOverlay').style.display = 'none';
}

function renderCart() {
  refreshCartCount();
  const items = Cart.get();
  const list = document.getElementById('cartItems');

  if (!items.length) {
    list.innerHTML = '<p class="empty">Your cart is empty.</p>';
    document.getElementById('cartTotal').textContent = '₹0.00';
    return;
  }

  list.innerHTML = items.map((i) => `
    <div class="cart-line">
      <div class="info">
        <div class="name">${i.name}</div>
        <div class="price">${formatPrice(i.unitPrice)} &times; ${i.quantity}</div>
      </div>
      <button onclick="Cart.setQuantity(${i.productId}, ${i.quantity - 1}); renderCart();">-</button>
      <button onclick="Cart.setQuantity(${i.productId}, ${i.quantity + 1}); renderCart();">+</button>
      <button class="del" onclick="Cart.remove(${i.productId}); renderCart();">&times;</button>
    </div>`).join('');

  document.getElementById('cartTotal').textContent = formatINR(Cart.total());
}

function goCheckout() {
  if (!Cart.get().length) {
    document.getElementById('cartHint').textContent = 'Cart is empty.';
    return;
  }
  if (!Session.user()) {
    closeCart();
    openModal('login');
    document.getElementById('cartHint').textContent = 'Login or register first, then checkout.';
    return;
  }
  window.location.href = '/checkout.html';
}

// ---------- Auth ----------

function renderUserArea() {
  const user = Session.user();
  const area = document.getElementById('userArea');
  const title = document.getElementById('welcomeTitle');
  const sub = document.getElementById('welcomeSub');

  if (user) {
    area.innerHTML = `
      <span class="greet">Hello, ${user.fullName.split(' ')[0]}</span>
      <a href="/profile.html" class="link-btn">Profile</a>
      ${user.role === 'ADMIN' ? '<a href="/admin.html" class="link-btn">Admin</a>' : ''}
      <button class="link-btn" onclick="logout()">Logout</button>`;
    title.textContent = `Welcome back, ${user.fullName}`;
    sub.textContent = 'Fresh from this week\u2019s handpicked collection';
  } else {
    area.innerHTML = `<button class="link-btn" onclick="openModal('login')">Sign in</button>`;
    title.textContent = 'Welcome to V Shoba\u2019s Boutique';
    sub.textContent = 'Handpicked sarees, kurtis and festive classics \u2014 chosen by Shoba herself.';
  }

  // Guests have no order history to view yet.
  document.getElementById('ordersLink').style.display = user ? '' : 'none';
}

function logout() {
  Session.clear();
  renderUserArea();
  renderCart();
}

function openModal(tab) {
  showTab(tab);
  document.getElementById('modalOverlay').classList.add('show');
}

function showTab(tab) {
  const isLogin = tab === 'login';
  document.getElementById('tabLogin').classList.toggle('active', isLogin);
  document.getElementById('tabRegister').classList.toggle('active', !isLogin);
  document.getElementById('loginForm').style.display = isLogin ? 'flex' : 'none';
  document.getElementById('registerForm').style.display = isLogin ? 'none' : 'flex';
  document.getElementById('modalTitle').textContent = isLogin ? 'Login' : 'Create Account';
  document.getElementById('authErr').textContent = '';
}

function setAuthErr(msg) {
  document.getElementById('authErr').textContent = msg || '';
}

async function doLogin(e) {
  e.preventDefault();
  setAuthErr('');
  try {
    const auth = await API.post('/api/auth/login', {
      email: document.getElementById('loginEmail').value,
      password: document.getElementById('loginPassword').value,
    });
    Session.save(auth);
    document.getElementById('modalOverlay').classList.remove('show');
    e.target.reset();
    renderUserArea();
  } catch (err) {
    setAuthErr(err.message);
  }
}

async function doRegister(e) {
  e.preventDefault();
  setAuthErr('');
  try {
    const auth = await API.post('/api/auth/register', {
      fullName: document.getElementById('regName').value,
      email: document.getElementById('regEmail').value,
      password: document.getElementById('regPassword').value,
      phone: document.getElementById('regPhone').value,
      address: document.getElementById('regAddress').value,
    });
    Session.save(auth);
    document.getElementById('modalOverlay').classList.remove('show');
    e.target.reset();
    renderUserArea();
  } catch (err) {
    setAuthErr(err.message);
  }
}
