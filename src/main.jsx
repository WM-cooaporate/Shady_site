import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import EventsShop from './EventsShop.jsx'
import SiteNav from './SiteNav.jsx'
import NotFound from './notFound.jsx'

const path = window.location.pathname.replace(/\/$/, '')

function Router() {
  if (path === '/shady-dashboard') return <App />
  if (path === '/events-products') return <><SiteNav /><EventsShop /></>
  if (path === '' || path === '/index.html') return <><SiteNav /><App /></>
  return <NotFound />
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Router />
  </StrictMode>,
)