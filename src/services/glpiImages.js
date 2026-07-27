import JSZip from 'jszip'
import { uploadDocument } from './api'

// détecte le vrai type d'après les premiers octets (magic bytes)
function detectType(bytes) {
    if (bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF)
        return { ext: 'jpg', mime: 'image/jpeg' }
    if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E && bytes[3] === 0x47)
        return { ext: 'png', mime: 'image/png' }
    if (bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46)
        return { ext: 'gif', mime: 'image/gif' }
    return null
}

export async function importImages(zipFile, assetMap) {
    const buffer = await zipFile.arrayBuffer()
    const zip = await JSZip.loadAsync(zipFile)

    for (const path of Object.keys(zip.files)) {
        const entry = zip.files[path]
        if (entry.dir) continue
        if (path.includes('__MACOSX')) continue          // ignore les fichiers macOS

        const filename = path.split('/').pop()
        const baseName = filename.replace(/\.[^.]+$/, '') // "PC-ADM-001"

        const asset = assetMap?.[baseName]
        if (!asset) continue                              // pas d'asset correspondant

        const bytes = await entry.async('uint8array')
        const real = detectType(bytes)
        if (!real) continue                               // type non reconnu

        // rectifie l'extension selon le vrai type détecté
        const correctedName = `${baseName}.${real.ext}`
        const blob = new Blob([bytes], { type: real.mime })
        await uploadDocument(correctedName, blob, asset.itemtype, asset.id)
        // const doc = await uploadDocument(correctedName, blob)
        // await linkDocumentItem(doc.id, asset.itemtype, asset.id)

        console.log(`Image ${filename} -> ${correctedName} (asset ${asset.itemtype}#${asset.id})`)
    }
}