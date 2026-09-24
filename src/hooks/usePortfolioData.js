import { useEffect, useState, useCallback } from 'react'
import { supabase } from '../lib/supabase'

const emptyData = {
    contacts: {
        whatsapp: '',
        instagram: '',
        facebook: '',
        email: '',
    },
    artProjects: [],
    offers: [],
}

export function usePortfolioData() {
    const [data, setDataState] = useState(emptyData)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    // ═══════════════════════════════════════
    // تحميل البيانات من Supabase
    // ═══════════════════════════════════════
    const loadData = useCallback(async() => {
        try {
            setLoading(true)
            setError(null)

            const [projectsRes, offersRes, contactsRes] = await Promise.all([
                supabase
                .from('art_projects')
                .select('*')
                .order('created_at', { ascending: false }),
                supabase
                .from('offers')
                .select('*')
                .order('created_at', { ascending: false }),
                supabase.from('contacts').select('*').limit(1).maybeSingle(),
            ])

            if (projectsRes.error) throw projectsRes.error
            if (offersRes.error) throw offersRes.error
            if (contactsRes.error) throw contactsRes.error

            setDataState({
                artProjects: projectsRes.data || [],
                offers: offersRes.data || [],
                contacts: contactsRes.data ?
                    {
                        whatsapp: contactsRes.data.whatsapp || '',
                        instagram: contactsRes.data.instagram || '',
                        facebook: contactsRes.data.facebook || '',
                        email: contactsRes.data.email || '',
                    } :
                    emptyData.contacts,
            })
        } catch (err) {
            console.error('Failed to load portfolio data:', err)
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        loadData()
    }, [loadData])

    // ═══════════════════════════════════════
    // setData — بسيط: بيحدّث الـ state المحلي فقط
    // (كل العمليات مع Supabase بتحصل في Dashboard.jsx)
    // ═══════════════════════════════════════
    const setData = useCallback(updaterOrValue => {
        setDataState(prev => {
            const nextData =
                typeof updaterOrValue === 'function' ?
                updaterOrValue(prev) :
                updaterOrValue
            return nextData
        })
    }, [])

    return [data, setData, { loading, error, reload: loadData }]
}