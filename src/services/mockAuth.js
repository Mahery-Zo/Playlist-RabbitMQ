export async function login(username, password) {
  console.log(`[MOCK AUTH] login: ${username}`);
  localStorage.setItem('access_token', 'demo-access-token');
  localStorage.setItem('session_token', 'demo-session-token');
  localStorage.setItem('isLoged', 'true');
  return 'demo-access-token';
}

export function logout() {
  console.log(`[MOCK AUTH] logout`);
  localStorage.removeItem('access_token');
  localStorage.removeItem('session_token');
  localStorage.removeItem('isLoged');
}

export function isAuthenticated() {
  return !!localStorage.getItem('isLoged');
}
