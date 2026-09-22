import { useEffect, useState } from 'react'
import { STORE, seed } from '../data/seed'

function loadData() {
    try {
        const saved = JSON.parse(localStorage.getItem(STORE))
        if (!saved) return seed

        return {
            contacts: {...seed.contacts, ...(saved.contacts || {}) },
            artProjects: Array.isArray(saved.artProjects) ? saved.artProjects : seed.artProjects,
            offers: Array.isArray(saved.offers) ? saved.offers : seed.offers,
        }
    } catch {
        return seed
    }
}

export function usePortfolioData() {
    const [data, setData] = useState(loadData)

    useEffect(() => {
        const timer = setTimeout(() => {
            try {
                localStorage.setItem(STORE, JSON.stringify(data))
            } catch (err) {
                console.error('Failed to save portfolio data:', err)
            }
        }, 400)
        return () => clearTimeout(timer)
    }, [data])

    return [data, setData]
}