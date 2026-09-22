import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { PortfolioProvider } from './context/PortfolioContext'
import { useScrollReveal } from './hooks/useScrollReveal'

import Home from './pages/Home'
import ArtGallery from './pages/ArtGallery'
import Dashboard from './pages/Dashboard'
import SiteNav from './components/SiteNav'
import NotFound from './components/NotFound'

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
    <BrowserRouter>
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
  )
}