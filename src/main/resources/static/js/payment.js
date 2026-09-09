// Payment page: after the customer pays (really or simulated),
// the backend marks the order PAID via POST /api/orders/{id}/pay.

// === RAZORPAY SETUP (optional) ===
// The key is NOT hardcoded here anymore. The backend serves it from the
// environment (razorpay.key=${RAZORPAY_KEY} in prod) via GET /api/config/razorpay-key.
//   1. Create a free account at https://razorpay.com (test mode works)
//   2. On Render set the RAZORPAY_KEY env var (test key id) + RAZORPAY_SECRET.
//   3. The "Pay Now" button will open Razorpay's checkout and the
//      payment gets verified before the order is marked PAID.
// Without a key configured, the button simulates a successful payment instead.
let RAZORPAY_KEY_ID = null;

let orderId = null;
let method = 'online';

document.addEventListener('DOMContentLoaded', async () => {
  const user = Session.user();
  if (!user) {
    window.location.href = '/';
    return;
  }

  // Pull the Razorpay key from the backend (empty when not configured).
  try {
    const config = await API.get('/api/config/razorpay-key');
    if (config && config.key) RAZORPAY_KEY_ID = config.key;
  } catch {
    // Key unavailable -> stay in simulated-payment mode.
  }

  const params = new URLSearchParams(window.location.search);
  orderId = params.get('orderId');
  const total = params.get('total');

  if (!orderId) {
    window.location.href = '/';
    return;
  }

  document.getElementById('orderId').textContent = orderId;
  document.getElementById('orderTotal').textContent = formatINR(total || 0);

  document.querySelectorAll('.pay-opt').forEach((opt) => {
    opt.addEventListener('click', () => {
      document.querySelectorAll('.pay-opt').forEach((o) => o.classList.remove('selected'));
      opt.classList.add('selected');
      method = opt.dataset.method;
    });
  });
  document.querySelector('.pay-opt').classList.add('selected');

  document.getElementById('payBtn').addEventListener('click', pay);
});

async function pay() {
  const msg = document.getElementById('msg');
  msg.className = '';
  msg.textContent = '';

  if (method === 'online' && RAZORPAY_KEY_ID && window.Razorpay) {
    try {
      // Create a Razorpay order on the server side via their API, then
      // open the checkout. On success, mark our order PAID.
      await startRazorpay();
    } catch (err) {
      msg.className = 'msg err';
      msg.textContent = 'Payment failed: ' + err.message;
    }
    return;
  }

  // Simulated gateway (or Pay on Delivery): confirm, then mark PAID.
  try {
    await API.post(`/api/orders/${orderId}/pay`, {});
    showSuccess();
  } catch (err) {
    msg.className = 'msg err';
    msg.textContent = err.message;
  }
}

// Real Razorpay flow (used only when RAZORPAY_KEY_ID is set).
async function startRazorpay() {
  const order = await API.get(`/api/orders/${orderId}`);
  const options = {
    key: RAZORPAY_KEY_ID,
    amount: Math.round(Number(order.totalAmount) * 100), // rupees -> paise
    currency: 'INR',
    name: 'V Shoba\'s Boutique',
    description: `Order #${order.id}`,
    handler: async (response) => {
      // Payment succeeded on Razorpay's side.
      await API.post(`/api/orders/${order.id}/pay`, {});
      showSuccess();
    },
    modal: { ondismiss: () => {} },
  };
  const rzp = new window.Razorpay(options);
  rzp.open();
}

function showSuccess() {
  document.querySelector('.panel').innerHTML = `
    <h2 style="color:#155724">Payment Successful!</h2>
    <p style="margin:12px 0">Thank you for shopping with V Shoba's Boutique.
    Your order #${orderId} has been paid and is being prepared.</p>
    <a class="btn" href="/orders.html">View My Orders</a>
    <a class="btn secondary" href="/">Continue Shopping</a>`;
}
