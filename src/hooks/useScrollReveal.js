import { useEffect } from 'react'

export function useScrollReveal() {
    useEffect(() => {
        const els = document.querySelectorAll('.reveal, .paint-underline')
        if (!els.length) return

        const observer = new IntersectionObserver(
            entries => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('is-visible')
                    }
                })
            }, { threshold: 0.15, rootMargin: '0px 0px -50px 0px' }
        )

        els.forEach(el => observer.observe(el))
        return () => observer.disconnect()
    }, [])
}