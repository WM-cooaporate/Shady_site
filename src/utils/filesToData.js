import imageCompression from 'browser-image-compression'

const compressOptions = {
    maxSizeMB: 0.5, // 500 KB كحد أقصى لكل صورة
    maxWidthOrHeight: 1200, // أقصى عرض/طول 1200px
    useWebWorker: true,
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