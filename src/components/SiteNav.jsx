import { Link, useLocation } from 'react-router-dom'

export default function SiteNav() {
  const { pathname } = useLocation()
  const current = pathname.replace(/\/$/, '') || '/'

  const style = {
    position: 'absolute',
    zIndex: 20,
    top: 25,
    left: '50%',
    transform: 'translateX(-50%)',
    display: 'flex',
    gap: 3,
    padding: 5,
    background: '#fff',
    borderRadius: 30,
    boxShadow: '0 7px 25px #2C3E5C15',
    whiteSpace: 'nowrap',
  }

  const link = active => ({
    padding: '9px 17px',
    borderRadius: 22,
    fontSize: 12,
    textDecoration: 'none',
    fontWeight: active ? 700 : 500,
    background: active ? '#2C3E5C' : 'transparent',
    color: active ? '#fff' : '#2C3E5C',
    transition: '.2s',
  })

  return (
    <nav style={style} aria-label="Main navigation">
      <Link style={link(current === '/')} to="/">Home</Link>
      <Link style={link(current === '/art')} to="/art">Art</Link>
    </nav>
  )
}