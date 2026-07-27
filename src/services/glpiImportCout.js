import { getAll, createItem, CreateObj, buildlookup, getOne, getTicketByExternalId, NODE_API } from "./api";
import { ref, computed } from 'vue'

function parseCSV(text) {
    const lines = text.trim().split('\n')
    const headers = lines[0].split(',').map(h => h.trim())
    return lines.slice(1).map(line => {
        const values = line.split(',').map(v => v.trim())
        return Object.fromEntries(headers.map((h, i) => [h, values[i] || '']))
    })
}

// seuls ces 3 types sont autorisés -> endpoint du dropdown "Model" correspondant
const MODEL_ENDPOINT = {
    Computer: 'Dropdowns/ComputerModel',
    Monitor: 'Dropdowns/MonitorModel',
    Phone: 'Dropdowns/PhoneModel',
}
const type_Map = {
  open : 2 ,
  close : 1
}

// retrouve l'id d'un dropdown par son nom (via cache), le crée s'il n'existe pas
async function ensureDropdown(endpoint, name, cache) {
    if (!name) return 0
    const key = name.toLowerCase()
    if (cache[key]) return cache[key]
    try {
        const res = await CreateObj(endpoint, { name })
        cache[key] = res.id
        return res.id
    } catch (e) {
        // existe déjà côté GLPI mais absent du cache -> on recharge la liste
        const items = await getAll(endpoint)
        for (const it of items) if (it.name) cache[it.name.toLowerCase()] = it.id
        return cache[key] || 0
    }
}

export async function importCout(csvtext) {
    const rows = parseCSV(csvtext)

    // caches des dropdowns (nom minuscule -> id), pré-remplis avec l'existant
    const stateCache = buildlookup(await getAll('Dropdowns/State'))
    const locationCache = buildlookup(await getAll('Dropdowns/Location'))
    const manufacturerCache = buildlookup(await getAll('Dropdowns/Manufacturer'))
    const userLookup = buildUserLookup(await getAll('Administration/User'))
    const modelCaches = {}   // un cache de models par type d'asset

    const assetMap = {}

    for (const row of rows) {
        // const itemtype = row.Item_Type
        // if (!MODEL_ENDPOINT[itemtype]) continue   // ignore tout sauf Computer / Monitor / Phone

        // if (!modelCaches[itemtype]) {
        //     modelCaches[itemtype] = buildlookup(await getAll(MODEL_ENDPOINT[itemtype]))
        // }

        // const data = {
        //     name: row.Name,
        //     otherserial: row.Inventory_Number,
        //     status: await ensureDropdown('Dropdowns/State', row.Status, stateCache),
        //     location: await ensureDropdown('Dropdowns/Location', row.Location, locationCache),
        //     manufacturer: await ensureDropdown('Dropdowns/Manufacturer', row.Manufacturer, manufacturerCache),
        //     model: await ensureDropdown(MODEL_ENDPOINT[itemtype], row.Model, modelCaches[itemtype]),
        //     user: await ensureUser(row.User, userLookup)
        // }

        // const result = await createItem(itemtype, data)
        // assetMap[row.Name] = { id: result.id, itemtype }
        const type  = row.Mvt

        const Ticket = await getTicketByExternalId(row.REFTicket)
        const idTicket = Ticket.id
        const valeur =  row.Valeur
        const mode = Number(row.Mode)
        if (type === 'cancel') {
            Annulation(idTicket)
        }
        else if (type === 'close') {
          ValiderCout(idTicket,valeur)
        }
        else{
          Reouvrir(idTicket,valeur,mode)
        }

    }

    return assetMap
}

  export  async function ValiderCout(idTicket , cout) {
      console.log('COUT :'+cout)
      await fetch(`${NODE_API}/api/cout`,{
        method : 'POST',
        headers : {'content-type' : 'application/json'},
        body : JSON.stringify({cout:Number(cout),idTicket : idTicket})
      })

    }

export async function Reouvrir(idTicket , pourcentage , mode) {
        console.log("MODEL : "+mode);

      const CoutRef = ref(0)
      if (mode == 1) {
          const resLastCout = await fetch(`${NODE_API}/api/cout/${idTicket}/last`)
          console.log("METYYY : "+(mode+1));

           const LastCout = await resLastCout.json()
           CoutRef.value = LastCout.cout
      }
      else if (mode == 2) {
        const resLastCout = await fetch(`${NODE_API}/api/cout/${idTicket}/first`)
        const LastCout = await resLastCout.json()
        console.log("LAST COUT  TEST : "+Number(LastCout.cout+1));

        CoutRef.value = Number(LastCout.cout)

      }
      else if (mode == 4 ) {
        const resLastCout = await fetch(`${NODE_API}/api/cout/${idTicket}`)

        const LastCout = await resLastCout.json()
        for (const c of LastCout) {
          CoutRef.value += c.cout
        }
      }
      else if (mode == 3) {
        const resLastCout = await fetch(`${NODE_API}/api/cout/${idTicket}`)
        const LastCout = await resLastCout.json()
        let sum = 0
        for (const c of LastCout) {
          sum += c.cout
        }
        CoutRef.value = sum/LastCout.length
      }

      console.log("COUT REF :"+CoutRef.value);

      const fraisReouverture = ref(0)
      const pourcentageFraisReouverture =pourcentage
      fraisReouverture.value =Number( pourcentageFraisReouverture* CoutRef.value /100)
      console.log("FRAIS  REOUVERTURE : "+fraisReouverture.value);

      await fetch(`${NODE_API}/api/frais`,{
        method : 'POST',
        headers : {'content-type' : 'application/json'},
        body : JSON.stringify({cout:Number(fraisReouverture.value),idTicket : idTicket})
      })

    }

async function Annulation(idTicket) {

      const resLastCout = await fetch(`${NODE_API}/api/cout/${idTicket}/last`)
      const LastCout = await resLastCout.json()
      console.log("LAST COUT : "+LastCout.id);

      await fetch(`${NODE_API}/api/cout/${LastCout.id}`,{
        method : 'DELETE',

      })
    }
// construit le cache des users existants, indexé par "realname firstname"
function buildUserLookup(users) {
    const map = {}
    for (const u of users) {
        const key = `${u.realname || ''} ${u.firstname || ''}`.trim().toLocaleLowerCase()
        if (key) {
            map[key] = u.id
        }
    }
    return map
}


// construit le cache des users existants, indexé par "realname firstname"
async function ensureUser(fullname, cache) {
    if (!fullname) return 0
    const key = fullname.toLocaleLowerCase()
    if (cache[key]) {
        return cache[key]
    }
    const parts = fullname.split(' ')
    const realname = parts[0] || fullname
    const firstname = parts.slice(1).join(' ')
    const username = fullname.toLowerCase().replace(/\s+/g, '.') //rakoto.jean
    try {
        const res = await CreateObj('Administration/User', { username, realname, firstname })
        cache[key] = res.id;
        return res.id
    } catch (e) {
        //existe deja -> on rechargera la liste et on reindexe
        const users = await getAll('Administration/User')
        for (const u of users) {
            const k = `${username || ''} ${u.firstname || ''} `.trim().toLocaleLowerCase()
            if (k) {
                cache[k] = u.id
            }
        }
        return cache[key] || 0
    }
}
