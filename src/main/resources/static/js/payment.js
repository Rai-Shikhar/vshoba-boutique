// Payment page: after the customer pays (really or simulated),
// the backend verifies the Razorpay signature and marks the order PAID.

// === RAZORPAY SETUP (optional) ===
// The secret is NEVER in the browser. The backend owns both credentials:
//   - POST /api/razorpay/create-order   creates the Razorpay order server-side
//   - POST /api/razorpay/verify-payment verifies the HMAC signature
// The frontend only ever receives the public KEY_ID (from the create-order
// response), which is what the checkout widget needs.
// Without keys configured, the button simulates a successful payment instead.
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
  // 1. Backend creates the Razorpay order for OUR order id and returns
  //    the order_id + key. The amount is computed server-side from the DB,
  //    so the client cannot change what is charged.
  const rzOrder = await API.post('/api/razorpay/create-order', { orderId });

  // 2. Open the Standard Checkout modal bound to that server order.
  const options = {
    key: rzOrder.key,
    order_id: rzOrder.order_id,
    amount: Number(rzOrder.amount),    // paise
    currency: rzOrder.currency,
    name: 'V Shoba\'s Boutique',
    description: `Order #${orderId}`,
    prefill: {
      email: ((Session.user() || {}).email || ''),
    },
    // 3. Successful payment -> Razorpay invokes this with the three ids
    //    needed to verify the signature on the backend.
    handler: async (response) => {
      try {
        await API.post('/api/razorpay/verify-payment', {
          orderId,
          razorpayOrderId: response.razorpay_order_id,
          razorpayPaymentId: response.razorpay_payment_id,
          razorpaySignature: response.razorpay_signature,
        });
        showSuccess();
      } catch (err) {
        // Signature mismatch / already paid / server error: NOT marked paid.
        showError('Payment could not be confirmed: ' + err.message);
      }
    },
    modal: {
      // 4. User dismissed the modal without paying. Order stays PENDING.
      ondismiss: () => {
        showError('Payment cancelled - your order is still pending. You can try again from My Orders.');
      },
    },
  };

  const rzp = new window.Razorpay(options);
  rzp.on('payment.failed', (response) => {
    const details = response.error || {};
    showError('Payment failed: ' + (details.description || 'please try again') + '. Order remains pending.');
  });
  rzp.open();
}

function showError(text) {
  const msg = document.getElementById('msg');
  msg.className = 'msg err';
  msg.textContent = text;
}

function showSuccess() {
  document.querySelector('.panel').innerHTML = `
    <h2 style="color:#155724">Payment Successful!</h2>
    <p style="margin:12px 0">Thank you for shopping with V Shoba's Boutique.
    Your order #${orderId} has been paid and is being prepared.</p>
    <a class="btn" href="/orders.html">View My Orders</a>
    <a class="btn secondary" href="/">Continue Shopping</a>`;
}
