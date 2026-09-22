import { useState } from 'react'
import { SESSION, ADMIN_EMAIL, ADMIN_PASSWORD } from '../data/seed'

export function useAuth() {
    const [signed, setSigned] = useState(
        () => sessionStorage.getItem(SESSION) === 'true'
    )
    const [error, setError] = useState('')

    const login = (email, password) => {
        if (email === ADMIN_EMAIL && password === ADMIN_PASSWORD) {
            sessionStorage.setItem(SESSION, 'true')
            setSigned(true)
            setError('')
            return true
        }
        setError('Incorrect email or password.')
        return false
    }

    const logout = () => {
        sessionStorage.removeItem(SESSION)
        setSigned(false)
    }

    return { signed, error, login, logout }
}