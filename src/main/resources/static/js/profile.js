document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('reloginForm').addEventListener('submit', doRelogin);
  document.getElementById('changePwForm').addEventListener('submit', doChangePassword);

  const user = Session.user();
  if (!user) {
    showLoginPanel();
    return;
  }

  renderProfile(user);
  verifyToken();
});

function renderProfile(user) {
  document.getElementById('profileName').textContent = 'Name: ' + user.fullName;
  document.getElementById('profileEmail').textContent = 'Email: ' + user.email;
  document.getElementById('profileInfo').style.display = '';
  document.getElementById('pwPanel').style.display = '';
}

function showLoginPanel() {
  document.getElementById('profileInfo').style.display = 'none';
  document.getElementById('pwPanel').style.display = 'none';
  document.getElementById('loginPanel').style.display = '';
}

async function verifyToken() {
  try {
    await API.get('/api/auth/me');
  } catch (err) {
    // Only the backend's real 401 means "logged out". A network/connection
    // error (free-tier cold start, offline) must NOT wipe the session.
    if (err.message === 'You are not logged in') {
      Session.clear();
      showLoginPanel();
    }
  }
}

async function doRelogin(e) {
  e.preventDefault();
  const err = document.getElementById('reloginErr');
  err.className = 'msg';
  err.textContent = '';
  try {
    const auth = await API.post('/api/auth/login', {
      email: document.getElementById('reloginEmail').value,
      password: document.getElementById('reloginPassword').value,
    });
    Session.save(auth);
    e.target.reset();
    renderProfile(auth);
    document.getElementById('loginPanel').style.display = 'none';
  } catch (ex) {
    err.className = 'msg err';
    err.textContent = ex.message;
  }
}

async function doChangePassword(e) {
  e.preventDefault();
  const msg = document.getElementById('pwMsg');
  msg.className = 'msg';
  msg.textContent = '';

  const oldPw = document.getElementById('oldPassword').value;
  const newPw = document.getElementById('newPassword').value;
  const confirmPw = document.getElementById('confirmPassword').value;

  if (newPw !== confirmPw) {
    msg.className = 'msg err';
    msg.textContent = 'New passwords do not match.';
    return;
  }

  if (newPw.length < 8) {
    msg.className = 'msg err';
    msg.textContent = 'New password must be at least 8 characters.';
    return;
  }

  try {
    await API.post('/api/auth/change-password', {
      oldPassword: oldPw,
      newPassword: newPw,
    });
    msg.className = 'msg ok';
    msg.textContent = 'Password updated successfully.';
    e.target.reset();
  } catch (err) {
    msg.className = 'msg err';
    msg.textContent = err.message;
    if (err.message === 'You are not logged in') {
      Session.clear();
      showLoginPanel();
    }
  }
}