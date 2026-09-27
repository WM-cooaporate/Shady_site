import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom'
import { HelmetProvider } from 'react-helmet-async'
import { PortfolioProvider } from './context/PortfolioContext'
import { useScrollReveal } from './hooks/useScrollReveal'

import Home from './pages/Home'
import ArtGallery from './pages/ArtGallery'
import Dashboard from './pages/Dashboard'
import SiteNav from './components/SiteNav'
import NotFound from './components/NotFound'

function PageTracker() {
  const location = useLocation()

  useEffect(() => {
    if (typeof window.gtag === 'function') {
      window.gtag('config', 'G-XXXXXXXXXX', {
        page_path: location.pathname + location.search,
      })
    }
  }, [location])

  return null
}

function PublicLayout({ children }) {
  useScrollReveal()
  return (
    <>
      <SiteNav />
      {children}
    </>
  )
}

export default function App() {
  return (
    <HelmetProvider>
      <BrowserRouter>
        <PageTracker />
        <PortfolioProvider>
          <Routes>
            <Route path="/shady-dashboard" element={<Dashboard />} />
            <Route
              path="/art"
              element={
                <PublicLayout>
                  <ArtGallery />
                </PublicLayout>
              }
            />
            <Route
              path="/"
              element={
                <PublicLayout>
                  <Home />
                </PublicLayout>
              }
            />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </PortfolioProvider>
      </BrowserRouter>
    </HelmetProvider>
  )
}