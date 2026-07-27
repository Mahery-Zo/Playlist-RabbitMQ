import { CreateObj } from "./api";

function parseCSVLine(line) {
    const values = []
    let current = ''
    let inQuotes = false
    for (let i = 0; i < line.length; i++) {
        const ch = line[i]
        if (ch === '"') {
            if (inQuotes && line[i + 1] === '"') { current += '"'; i++ }
            else inQuotes = !inQuotes
        } else if (ch === ',' && !inQuotes) {
            values.push(current.trim())
            current = ''
        } else {
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
        return Object.fromEntries(headers.map((h, i) => [h, values[i] || '']))
    })
}

// "8,7" -> 8.7
function toNumber(v) {
    return parseFloat((v || '0').replace(',', '.')) || 0
}

export async function importCosts(csvtext, ticketMap) {
    const rows = parseCSV(csvtext)

    for (const row of rows) {

        const ticketId = ticketMap?.[row.Num_Ticket]
        if (!ticketId) continue
        console.log("row :" + row.Num_Ticket);

        const data = {
            ticket: {
                id: ticketId
            },
            duration: parseInt(row.Duration_second) || 0,
            cost_time: toNumber(row.Time_Cost),
            cost_fixed: toNumber(row.Fixed_Cost),
        }
        await CreateObj(`Assistance/Ticket/${ticketId}/Cost`, data)
    }
}