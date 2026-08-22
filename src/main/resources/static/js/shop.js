// Shop page logic: load products, render grid, handle cart + auth modal.

let allProducts = [];

document.addEventListener('DOMContentLoaded', () => {
  loadCategories();
  loadProducts();
  renderUserArea();
  renderCart();

  document.getElementById('cartBtn').addEventListener('click', toggleCart);
  document.getElementById('cartOverlay').addEventListener('click', closeCart);
  document.getElementById('checkoutBtn').addEventListener('click', goCheckout);

  document.getElementById('searchBox').addEventListener('input', applyFilters);
  document.getElementById('categorySelect').addEventListener('change', applyFilters);

  document.getElementById('closeModal').addEventListener('click', () =>
    document.getElementById('modalOverlay').classList.remove('show'));
  document.getElementById('tabLogin').addEventListener('click', () => showTab('login'));
  document.getElementById('tabRegister').addEventListener('click', () => showTab('register'));
  document.getElementById('loginForm').addEventListener('submit', doLogin);
  document.getElementById('registerForm').addEventListener('submit', doRegister);
});

async function loadCategories() {
  try {
    const cats = await API.get('/api/products/categories');
    const select = document.getElementById('categorySelect');
    cats.forEach((c) => {
      const opt = document.createElement('option');
      opt.value = c;
      opt.textContent = c;
      select.appendChild(opt);
    });
  } catch { /* categories are a nice-to-have */ }
}

async function loadProducts() {
  try {
    allProducts = await API.get('/api/products');
    renderGrid(allProducts);
  } catch (err) {
    document.getElementById('grid').innerHTML =
      `<p class="empty">Could not load products: ${err.message}</p>`;
  }
}

function applyFilters() {
  const term = document.getElementById('searchBox').value.trim().toLowerCase();
  const category = document.getElementById('categorySelect').value;
  const filtered = allProducts.filter((p) => {
    const matchesCategory = !category || p.category === category;
    const matchesTerm = !term ||
      p.name.toLowerCase().includes(term) ||
      (p.description && p.description.toLowerCase().includes(term));
    return matchesCategory && matchesTerm;
  });
  renderGrid(filtered);
}

function renderGrid(products) {
  const grid = document.getElementById('grid');
  if (!products.length) {
    grid.innerHTML = '<p class="empty">No products found.</p>';
    return;
  }
  grid.innerHTML = products.map((p, index) => {
    const inStock = p.stockQuantity > 0;
    return `
      <div class="card" style="animation-delay:${Math.min(index * 70, 700)}ms">
        <div class="thumb">
          ${productImage(p)}
          ${p.category ? `<span class="cat-tag">${p.category}</span>` : ''}
        </div>
        <div class="body">
          <h3>${p.name}</h3>
          <p class="desc">${p.description}</p>
          <div class="price-row">
            <span class="price">${formatINR(p.price)}</span>
            ${inStock
              ? `<button onclick="addToCart(${p.id}, this)">Add to Cart</button>`
              : `<span class="badge-out">Out of stock</span>`}
          </div>
        </div>
      </div>`;
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
    button.textContent = 'Add to Cart';
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
  count.classList.remove('pop');
  void count.offsetWidth; // restart the CSS animation
  count.classList.add('pop');
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
        <div class="price">${formatINR(i.unitPrice)} &times; ${i.quantity}</div>
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
  if (user) {
    area.innerHTML = `
      <span>Hello, ${user.fullName.split(' ')[0]}!</span>
      <a href="/orders.html">My Orders</a>
      ${user.role === 'ADMIN' ? '<a href="/admin.html">Admin Panel</a>' : ''}
      <button class="link-btn" onclick="logout()">Logout</button>`;
  } else {
    area.innerHTML = `<button class="link-btn" onclick="openModal('login')">Login / Register</button>`;
  }
  document.getElementById('welcome').textContent = user
    ? `Welcome back to V Shoba's Boutique, ${user.fullName}!`
    : 'Welcome to V Shoba\'s Boutique - sarees, kurtis and more, delivered to your door.';
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
