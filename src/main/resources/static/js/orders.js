// "My Orders" page: the logged-in customer's order history.

document.addEventListener('DOMContentLoaded', async () => {
  const user = Session.user();
  if (!user) {
    window.location.href = '/';
    return;
  }

  const box = document.getElementById('orders');
  try {
    const orders = await API.get('/api/orders/mine');
    if (!orders.length) {
      box.innerHTML = '<div class="panel"><p class="empty">No orders yet. Time to shop!</p>' +
        '<div style="text-align:center"><a class="btn" href="/">Browse Products</a></div></div>';
      return;
    }

    box.innerHTML = orders.map((o) => `
      <div class="panel">
        <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:8px;margin-bottom:10px">
          <strong>Order #${o.id}</strong>
          <span class="status status-${o.status}">${o.status}</span>
        </div>
        <div class="small">Placed: ${new Date(o.createdAt).toLocaleString()}</div>
        <div class="small" style="margin-bottom:8px">Ship to: ${o.shippingAddress}</div>
        ${o.items.map((i) => `
          <div class="summary-line">
            <span>${i.productName} &times; ${i.quantity}</span>
            <span>${formatINR(i.subtotal)}</span>
          </div>`).join('')}
        <div class="summary-line grand"><span>Total</span><span>${formatINR(o.totalAmount)}</span></div>
        ${o.status === 'PENDING'
          ? `<a class="btn" style="margin-top:10px" href="/payment.html?orderId=${o.id}&total=${o.totalAmount}">Complete Payment</a>`
          : ''}
      </div>`).join('');
  } catch (err) {
    box.innerHTML = `<div class="msg err">${err.message}</div>`;
  }
});
