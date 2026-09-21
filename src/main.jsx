import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import EventsShop from './EventsShop.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {window.location.pathname.replace(/\/$/, '') === '/events-products' ? <EventsShop /> : <App />}
  </StrictMode>,
)
