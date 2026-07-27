const GLPI_URL = '/glpi-api'
const CLIENT_ID = import.meta.env.VITE_CLIENT_ID
const CLIENT_SECRET = import.meta.env.VITE_CLIENT_SECRET
const APP_TOKEN = import.meta.env.VITE_APP_TOKEN

export async function login(username, password) {
  const body = new URLSearchParams({
    grant_type: 'password',
    username,
    password,
    client_id: CLIENT_ID,
    client_secret: CLIENT_SECRET,
    scope: 'api status email',
  })

  const res = await fetch(`${GLPI_URL}/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })


  if (!res.ok) throw new Error('Identifiants invalides')
  const data = await res.json()
  console.log('RESPONSE LOGIN DATA :', data.access_token)  // ajoute cette ligne
  localStorage.setItem('access_token', data.access_token)
  // session legacy v1 (pour lier asset <-> ticket, non dispo en API v2)
  const params = new URLSearchParams({ login: username, password })
  const sessRes = await fetch(`${GLPI_URL}/v1/initSession?${params}`, {
    headers: { 'App-Token': APP_TOKEN },
  })
  if (sessRes.ok) {
    const sess = await sessRes.json()
    localStorage.setItem('session_token', sess.session_token)
  }
  return data.access_token
}
export function logout() {
  // localStorage.removeItem('access_token')
  localStorage.removeItem('isLoged')
}
export function isAuthenticated() {
  // return !!localStorage.getItem('access_token')
  return !!localStorage.getItem('isLoged')
}



