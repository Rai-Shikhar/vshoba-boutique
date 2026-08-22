// Checkout page: confirm address, place the order (PENDING),
// then hand over to the payment page.

document.addEventListener('DOMContentLoaded', () => {
  const user = Session.user();
  if (!user) {
    window.location.href = '/';
    return;
  }

  const items = Cart.get();
  if (!items.length) {
    window.location.href = '/';
    return;
  }

  // Pre-fill the address from the user's profile if they have one.
  API.get('/api/auth/me')
    .then((me) => {
      if (me && me.address) {
        document.getElementById('shippingAddress').value = me.address;
      }
    })
    .catch(() => {});

  renderSummary();
  document.getElementById('addressForm').addEventListener('submit', placeOrder);
});

function renderSummary() {
  const items = Cart.get();
  document.getElementById('summary').innerHTML =
    items.map((i) => `
      <div class="summary-line">
        <span>${i.name} &times; ${i.quantity}</span>
        <span>${formatINR(i.unitPrice * i.quantity)}</span>
      </div>`).join('') +
    `<div class="summary-line grand">
      <span>Total</span><span>${formatINR(Cart.total())}</span>
    </div>`;
}

async function placeOrder(e) {
  e.preventDefault();
  const msgBox = document.getElementById('msg');
  const items = Cart.get();

  try {
    const order = await API.post('/api/orders', {
      shippingAddress: document.getElementById('shippingAddress').value.trim(),
      items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
    });

    // The order exists (PENDING, stock deducted). The cart's job is done.
    Cart.clear();
    msgBox.className = 'msg ok';
    msgBox.textContent = 'Order placed! Redirecting to payment...';
    window.location.href = `/payment.html?orderId=${order.id}&total=${order.totalAmount}`;
  } catch (err) {
    msgBox.className = 'msg err';
    msgBox.textContent = err.message;
  }
}
