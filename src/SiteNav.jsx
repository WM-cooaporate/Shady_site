export default function SiteNav() {
  const current = window.location.pathname.replace(/\/$/, '')
  const style = { position: 'absolute', zIndex: 20, top: 25, left: '50%', transform: 'translateX(-50%)', display: 'flex', gap: 3, padding: 5, background: '#fffdf9', borderRadius: 30, boxShadow: '0 7px 25px #4b311112', whiteSpace: 'nowrap' }
  const link = active => ({ padding: '9px 15px', borderRadius: 22, fontSize: 12, textDecoration: 'none', background: active ? '#25211e' : 'transparent', color: active ? '#fff' : '#241f1c' })
  return <nav style={style} aria-label="Main navigation"><a style={link(current === '')} href="/">Home</a><a style={link(current === '/events-products')} href="/events-products">Events & Products</a></nav>
}
