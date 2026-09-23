import imageCompression from 'browser-image-compression'
const compressOptions = {
    maxSizeMB: 0.8, // ← رفعناها من 0.5 لـ 0.8 (جودة أعلى)
    maxWidthOrHeight: 1920, // ← رفعناها من 1200 لـ 1920
    useWebWorker: true,
    initialQuality: 0.85, // ← ضمان جودة عالية
}
async function compressFile(file) {
    try {
        return await imageCompression(file, compressOptions)
    } catch {
        return file // لو فشل الضغط، ارجع الأصلية
    }
}

function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = reject
        reader.readAsDataURL(file)
    })
}

export const filesToData = async files => {
    const arr = [...files]
    const compressed = await Promise.all(arr.map(compressFile))
    return Promise.all(compressed.map(fileToBase64))
}