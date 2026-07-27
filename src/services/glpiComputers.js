import { headers } from "./api";

const GLPI_URL = '/glpi-api'
export async function importComputer(data) {
    const token = localStorage.getItem('access_token')

    const res = await fetch(`${GLPI_URL}/Assets/Computer`, {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify({ data }),
    })
    if (!res.ok) throw new Error(`Erreur pour ${data.name}`);
    return res.json()
}