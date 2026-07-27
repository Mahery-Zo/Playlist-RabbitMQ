const GLPI_URL = '/glpi-api'
const APP_TOKEN = import.meta.env.VITE_APP_TOKEN
const ACCES_TOKEN = import.meta.env.VITE_ACCESS_TOKEN
// console.log('ACCESS_TOKEN:', ACCES_TOKEN)
export function headers() {
    const token = localStorage.getItem('access_token')
    // console.log('TOKEN ENVOYÉ:', token)
    return {
        'Authorization': `Bearer ${token}`,
        'App-Token': APP_TOKEN,
    }
}

export function getToken() {
    return localStorage.getItem('access_token')
}
export async function getAll(itemtype, isdeleted) {
    console.log(`REQUEST ${GLPI_URL}/${itemtype}`);

    const res = await fetch(`${GLPI_URL}/${itemtype}?start=0&limit=300`,
        {
            method: 'GET',
            headers: headers()
        }

    )


    if (!res.ok) return []
    return await res.json()
}
export async function createItem(itemtype, data) {


    const res = await fetch(`${GLPI_URL}/assets/${itemtype}`, {
        method: 'POST', // *GET, POST, PUT, DELETE, etc.
        headers: { ...headers(), 'Content-Type': 'application/json' },
        body: JSON.stringify(data) // must match 'Content-Type' header
    })
    if (!res.ok) throw new Error(`Erreur creation ${itemtype}: ${await res.text()}`)
    return res.json()

}
export async function CreateObj(type, data) {
    const res = await fetch(`${GLPI_URL}/${type}`, {
        method: 'POST',
        headers: { ...headers(), 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    if (!res.ok) throw new Error('Erreur lors de la creation avec :' + type + ' : ' + await res.text());
    return res.json()

}
export async function deleteItem(itemtype, id) {
    const res = await fetch(`${GLPI_URL}/${itemtype}/${id}?force=true`, {
        method: 'DELETE',
        headers: headers()
    })
}



export function buildlookup(items) {
    const map = {}
    for (const item of items) {
        if (item.name) map[item.name.toLowerCase()] = item.id
    }
    return map
}
export async function linkItemToTicket(ticketId, itemtype, itemsId) {
    const session = localStorage.getItem('session_token')
    const res = await fetch(`${GLPI_URL}/v1/Item_Ticket`, {
        method: 'POST',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN,
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ input: { tickets_id: ticketId, itemtype, items_id: itemsId } })
    })
    if (!res.ok) {
        throw new Error('Erreur lien Item_Ticket :' + await res.text());
    }
    return res.json()
}
export async function uploadDocument(filename, blob, itemtype, itemsId) {
    const session = localStorage.getItem('session_token')
    const manifest = JSON.stringify({
        input: { name: filename, _filename: [filename], itemtype, items_id: itemsId }
    })
    const form = new FormData()
    form.append('uploadManifest', manifest)
    form.append('filename[0]', blob, filename)

    const res = await fetch(`${GLPI_URL}/v1/Document`, {
        method: 'POST',
        headers: { 'Session-Token': session, 'App-Token': APP_TOKEN },
        body: form
    })
    if (!res.ok) throw new Error('Erreur upload Document: ' + await res.text())
    return res.json()
}
export async function getTicketItems(ticketId) {
    const session = localStorage.getItem("session_token")
    const res = await fetch(`${GLPI_URL}/v1/Ticket/${ticketId}/Item_Ticket`, {
        method: 'GET',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN
        }
    })
    if (!res.ok) return []
    return res.json()
}


export async function getTicketCosts(ticketId) {
    const session = localStorage.getItem("session_token")
    const res = await fetch(`${GLPI_URL}/Assistance/Ticket/${ticketId}/cost`, {
        method: 'GET',
        headers: headers()
    })
    if (!res.ok) return []
    return res.json()
}
export async function getOne(itemType, id) {
    const res = await fetch(`${GLPI_URL}/${itemType}/${id}`, {
        method: 'GET',
        headers: headers()
    })
    if (!res.ok) return null
    return res.json()
}

// export async function linkDocumentItem(documentId, itemType, itemsId) {
//     const session = localStorage.getItem("session_token")
//     const res = await fetch(`${GLPI_URL}/v1/Document_Item`, {
//         method: 'POST',
//         headers: {
//             'Session-Token': session,
//             'App-token': APP_TOKEN,
//             'Content-Type': 'application/json'
//         },
//         body: JSON.stringify({ input: { documents_id: documentId, itemType: itemsId } })
//     })
//     if (!res.ok) throw new Error('Erreur de lien Document_Item :' + await res.text());
//     return res.json()
// }


export async function assignTechnician(ticketId, userId) {
    console.log('ASSIGN -> ticket:', ticketId, 'user:', userId)
    const session = localStorage.getItem("session_token")
    const res = await fetch(`${GLPI_URL}/v1/Ticket_User`, {
        method: 'POST',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN,
            'Content-type': 'application/json'
        },
        body: JSON.stringify({
            input: { tickets_id: ticketId, users_id: userId, type: 2 }
        })

    })
    if (!res.ok) throw new Error('Erreur lors de assingation Technicien' + await res.text());
    return res.json()
}

export async function updateTicketStatus(ticket_id, statusid) {
    const res = await fetch(`${GLPI_URL}/Assistance/Ticket/${ticket_id}`, {
        method: 'PATCH',
        headers: { ...headers(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: { id: statusid } })
    })
    if (!res.ok) throw new Error('Erreur lors de l update de Stauts ticket :' + res.text());
    return res.json()
}
export async function getTicketAssignees(ticketId) {
    const session = localStorage.getItem("session_token")
    const res = await fetch(`${GLPI_URL}/v1/Ticket/${ticketId}/Ticket_User`, {
        method: 'GET',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN
        }
    })
    if (!res.ok) {
        return []
    }
    const data = await res.json()
    return (Array.isArray(data) ? data : []).filter(u => Number(u.type) === 2)
}
export async function getTicketSolutions(ticketId) {
    const res = await fetch(`${GLPI_URL}/Assistance/Ticket/${ticketId}/Timeline/Solution`, {
        method: 'GET',
        headers: headers()
    })
    if (!res.ok) {
        return []
    }
    const data = await res.json()
    return Array.isArray(data) ? data : []
}
export async function addSolution(ticketId, content) {
    const res = await fetch(`${GLPI_URL}/Assistance/Ticket/${ticketId}/Timeline/Solution`, {
        method: 'POST',
        headers: { ...headers(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ content })
    })
    if (!res.ok) throw new Error('Erreur lors de Ajout de Solution :' + res.json());
}
export async function getTicketByExternalId(externalID) {
  const res = await fetch (`${GLPI_URL}/Assistance/Ticket?filter=external_id==${externalID}`,{
    method : 'GET',
    headers : headers() ,

  })
  if(!res.ok) return null
  const data = await res.json()
  return (Array.isArray(data) && data[0]) ||null
}


function parseCSV(text) {
    const lines = text.trim().split('\n')
    const headers = lines[0].split(',').map(h => h.trim())
    return lines.slice(1).map(line => {
        const values = line.split(',').map(v => v.trim())
        return Object.fromEntries(headers.map((h, i) => [h, values[i] || '']))
    })
}



