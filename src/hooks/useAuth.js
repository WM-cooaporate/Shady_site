import { useState, useEffect } from 'react'
import { supabase } from '../lib/supabase'

export function useAuth() {
    const [signed, setSigned] = useState(false)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        // الجلسة عند التحميل
        supabase.auth.getSession().then(({ data: { session } }) => {
            setSigned(!!session)
            setLoading(false)
        })

        // الاستماع لتغييرات الجلسة
        const { data: { subscription } } = supabase.auth.onAuthStateChange(
            (_event, session) => {
                setSigned(!!session)
            }
        )

        return () => subscription.unsubscribe()
    }, [])

    const login = async(email, password) => {
        setError('')
        setLoading(true)

        const { error: authError } = await supabase.auth.signInWithPassword({
            email,
            password,
        })

        if (authError) {
            setError(authError.message)
            setLoading(false)
            return false
        }

        setSigned(true)
        setLoading(false)
        return true
    }

    const logout = async() => {
        await supabase.auth.signOut()
        setSigned(false)
    }

    return { signed, error, loading, login, logout }
}