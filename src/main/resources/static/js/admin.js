// Admin panel: login gate, inventory CRUD, low-stock warnings, order status.

document.addEventListener('DOMContentLoaded', () => {
  if (Session.user() && Session.user().role === 'ADMIN') {
    showDashboard();
  }

  document.getElementById('adminLoginForm').addEventListener('submit', adminLogin);
  document.getElementById('productForm').addEventListener('submit', saveProduct);
  document.getElementById('tabInventory').addEventListener('click', () => {
    document.getElementById('inventoryTab').style.display = 'block';
    document.getElementById('ordersTab').style.display = 'none';
    loadInventory();
  });
  document.getElementById('tabOrders').addEventListener('click', () => {
    document.getElementById('inventoryTab').style.display = 'none';
    document.getElementById('ordersTab').style.display = 'block';
    loadOrders();
  });
});

function logout() {
  Session.clear();
  location.reload();
}

async function adminLogin(e) {
  e.preventDefault();
  document.getElementById('loginErr').textContent = '';
  try {
    const auth = await API.post('/api/auth/login', {
      email: document.getElementById('adminEmail').value,
      password: document.getElementById('adminPassword').value,
    });
    if (auth.role !== 'ADMIN') {
      throw new Error('This account is not an admin');
    }
    Session.save(auth);
    showDashboard();
  } catch (err) {
    document.getElementById('loginErr').textContent = err.message;
  }
}

function showDashboard() {
  document.getElementById('loginGate').style.display = 'none';
  document.getElementById('dashboard').style.display = 'block';
  document.getElementById('adminUser').innerHTML =
    `<span>${Session.user().fullName}</span>`;
  loadInventory();
}

// ---------- Inventory ----------

async function loadInventory() {
  try {
    const products = await API.get('/api/products/all');

    const low = products.filter((p) => p.stockQuantity <= 5 && p.active);
    document.getElementById('lowStock').innerHTML = low.length
      ? low.map((p) => `<div class="summary-line"><span>${p.name}</span><span class="status status-PENDING">${p.stockQuantity} left</span></div>`).join('')
      : '<div class="small">All good - nothing low in stock.</div>';

    document.getElementById('productsTable').innerHTML = `
      <table>
        <tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Status</th><th>Actions</th></tr>
        ${products.map((p) => `
          <tr>
            <td>${p.id}</td>
            <td>${p.name}</td>
            <td>${p.category || '-'}</td>
            <td>${formatINR(p.price)}</td>
            <td>${p.stockQuantity}</td>
            <td>${p.active ? '<span class="status status-PAID">active</span>' : '<span class="status status-CANCELLED">hidden</span>'}</td>
            <td class="row-actions">
              <button class="btn secondary" onclick='editProduct(${JSON.stringify(p).replace(/'/g, '&#39;')})'>Edit</button>
              <button class="btn secondary" onclick="toggleActive(${p.id}, ${!p.active})">${p.active ? 'Hide' : 'Show'}</button>
              <button class="btn danger" onclick="deleteProduct(${p.id})">Delete</button>
            </td>
          </tr>`).join('')}
      </table>`;
  } catch (err) {
    document.getElementById('productsTable').innerHTML = `<div class="msg err">${err.message}</div>`;
  }
}

function editProduct(p) {
  document.getElementById('pfId').value = p.id;
  document.getElementById('pfName').value = p.name;
  document.getElementById('pfCategory').value = p.category || '';
  document.getElementById('pfPrice').value = p.price;
  document.getElementById('pfStock').value = p.stockQuantity;
  document.getElementById('pfImage').value = p.imageUrl || '';
  document.getElementById('pfDescription').value = p.description;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function resetForm() {
  document.getElementById('productForm').reset();
  document.getElementById('pfId').value = '';
}

async function saveProduct(e) {
  e.preventDefault();
  const id = document.getElementById('pfId').value;
  const body = {
    name: document.getElementById('pfName').value.trim(),
    category: document.getElementById('pfCategory').value.trim() || null,
    price: Number(document.getElementById('pfPrice').value),
    stockQuantity: Number(document.getElementById('pfStock').value),
    imageUrl: document.getElementById('pfImage').value.trim() || null,
    description: document.getElementById('pfDescription').value.trim(),
  };

  try {
    if (id) {
      await API.put(`/api/products/${id}`, body);
    } else {
      await API.post('/api/products', body);
    }
    resetForm();
    loadInventory();
  } catch (err) {
    alert(err.message);
  }
}

async function toggleActive(id, active) {
  try {
    await API.put(`/api/products/${id}`, { active });
    loadInventory();
  } catch (err) {
    alert(err.message);
  }
}

async function deleteProduct(id) {
  if (!confirm('Delete product #' + id + '? (Fails if it still has stock)')) return;
  try {
    await API.del(`/api/products/${id}`);
    loadInventory();
  } catch (err) {
    alert(err.message);
  }
}

// ---------- Orders ----------

async function loadOrders() {
  try {
    const orders = await API.get('/api/orders');
    document.getElementById('ordersTable').innerHTML = `
      <table>
        <tr><th>ID</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th><th>Actions</th></tr>
        ${orders.map((o) => `
          <tr>
            <td>${o.id}</td>
            <td>${o.user.fullName}<div class="small">${o.user.email}</div></td>
            <td>${o.items.map((i) => `${i.productName} &times; ${i.quantity}`).join('<br>')}</td>
            <td>${formatINR(o.totalAmount)}</td>
            <td><span class="status status-${o.status}">${o.status}</span></td>
            <td class="row-actions">
              <select onchange="changeStatus(${o.id}, this.value)">
                ${['PENDING', 'PAID', 'SHIPPED', 'CANCELLED'].map((s) =>
                  `<option value="${s}" ${s === o.status ? 'selected' : ''}>${s}</option>`).join('')}
              </select>
            </td>
          </tr>`).join('')}
      </table>`;
  } catch (err) {
    document.getElementById('ordersTable').innerHTML = `<div class="msg err">${err.message}</div>`;
  }
}

async function changeStatus(orderId, status) {
  try {
    await API.put(`/api/orders/${orderId}/status?status=${status}`, {});
    loadOrders();
  } catch (err) {
    alert(err.message);
    loadOrders();
  }
}
