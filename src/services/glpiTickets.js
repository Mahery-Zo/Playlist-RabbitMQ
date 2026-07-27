
import { CreateObj, linkItemToTicket } from "./api";
function parseCSVLine(line) {
    const values = []
    let current = ''
    let inQuotes = false
    for (let i = 0; i < line.length; i++) {
        const ch = line[i];
        if (ch === '"') {
            if (inQuotes && line[i + 1] === '"') {
                current += '"'; i++
            }
            else inQuotes = !inQuotes
        }
        else if (ch === ',' && !inQuotes) {
            values.push(current.trim())
            current = ''
        }
        else {
            current += ch
        }
    }
    values.push(current.trim())
    return values


}
function parseCSV(text) {
    const lines = text.trim().split('\n')
    const headers = lines[0].split(',').map(h => h.trim())
    return lines.slice(1).map(line => {
        const values = parseCSVLine(line)
        return Object.fromEntries(headers.map((h, i) => [h, values[i]]) || '')
    }
    )
}
export const STATUS_MAP = { 'new': 1, 'assigned': 2, 'planned': 3, 'pending': 4, 'solved': 5, 'closed': 6 }
export const PRIORITY_MAP = { 'very low': 1, 'low': 2, 'medium': 3, 'high': 4, 'very high': 5 }
export const TYPE_MAP = { 'incident': 1, 'request': 2 }

export async function importTickets(csvtext, assetMap) {
    const rows = parseCSV(csvtext)
    console.log("ROW : " + rows.length);

    const ticketMap = {}
    for (const row of rows) {

        console.log("DATE TEST :", row.Date);

        const [day, month, year] = row.Date.split('/')
        const date = `${year}-${month}-${day}T${row.Heure}:00`
        const data = {
            external_id : row.Ref_Ticket ,
            name: row.Titre,
            content: row.Description,
            date,
            status: { id: STATUS_MAP[row.Status?.toLowerCase()] || 1 },
            priority: PRIORITY_MAP[row.Priority?.toLowerCase()] || 3,
            type: TYPE_MAP[row.Type?.toLowerCase()] || 1,
        }
        const result = await CreateObj('Assistance/Ticket', data)
        console.log('TICKET RESULT:', result)
        console.log('ASSET TROUVÉ:', assetMap)
        ticketMap[row.Ref_Ticket] = result.id
        let items = []
        try {
            items = JSON.parse(row.Items || '[]')
        } catch (e) {

        }
        for (const assetName of items) {
            const asset = assetMap?.[assetName]
            if (asset) {
                await linkItemToTicket(result.id, asset.itemtype, asset.id)
            }
        }
    }
    return ticketMap

}
