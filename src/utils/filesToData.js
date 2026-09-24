import imageCompression from 'browser-image-compression'
import { supabase, STORAGE_BUCKET } from '../lib/supabase'

const compressOptions = {
    maxSizeMB: 0.8,
    maxWidthOrHeight: 1920,
    useWebWorker: true,
    initialQuality: 0.85,
}

async function compressFile(file) {
    try {
        return await imageCompression(file, compressOptions)
    } catch {
        return file
    }
}

/**
 * يرفع الملفات على Supabase Storage ويِرجع الـ URLs
 */
export const filesToData = async files => {
    const arr = [...files]
    const compressed = await Promise.all(arr.map(compressFile))

    const uploads = compressed.map(async file => {
        const ext = file.name.split('.').pop() || 'jpg'
        const fileName = `${Date.now()}-${Math.random()
      .toString(36)
      .slice(2, 10)}.${ext}`
        const filePath = `projects/${fileName}`

        const { error } = await supabase.storage
            .from(STORAGE_BUCKET)
            .upload(filePath, file, {
                cacheControl: '31536000',
                upsert: false,
                contentType: file.type || 'image/jpeg',
            })

        if (error) throw error

        const { data } = supabase.storage
            .from(STORAGE_BUCKET)
            .getPublicUrl(filePath)

        return data.publicUrl
    })

    return Promise.all(uploads)
}