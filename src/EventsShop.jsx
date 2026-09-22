import { useState } from 'react'

const whatsapp = 'https://wa.me/201201200208'

const items = [
  { id: 1, type: 'Event', title: 'Open Painting Day', text: 'A relaxed day of colour, painting, and creative conversation with Shady.', action: 'Reserve a place', image: 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=1200&q=85' },
  { id: 2, type: 'Product', title: 'Custom Art Print', text: 'A made-to-order art print designed to bring a little colour into your space.', action: 'Order now', image: 'https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?auto=format&fit=crop&w=1200&q=85' },
]

async function copyToClipboard(text) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // fall through to legacy method
  }
  try {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.focus()
    ta.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    return ok
  } catch {
    return false
  }
}

export default function EventsShop() {
  const [filter, setFilter] = useState('All')
  const [selected, setSelected] = useState(null)
  const [details, setDetails] = useState({ name: '', phone: '', quantity: '1', email: '', place: '' })
  const [copied, setCopied] = useState(false)

  const visible = items.filter(item => filter === 'All' || item.type === filter)

  const message = selected && `New ${selected.type} request\n${selected.title}\n\nName: ${details.name}\nPhone: ${details.phone}\nEmail: ${details.email}\nQuantity / attendees: ${details.quantity}\nPlace / delivery location: ${details.place}`

  const handleInstagram = async () => {
    const ok = await copyToClipboard(message)
    setCopied(ok)
    window.open('https://ig.me/m/shady.gad33', '_blank', 'noreferrer')
  }

  return (
    <main className="events-page">
      <header className="dashboard-header">
        <a className="brand" href="/">
          <span className="brand-mark">S</span>
          <span>SHADY <i>ARTIST</i></span>
        </a>
        <a className="small-link" href="/">Back to portfolio</a>
      </header>

      <section className="events-head">
        <p className="eyebrow">EVENTS & SHOP</p>
        <h1>Bring more colour<br /><span>into your day.</span></h1>
        <p>Join an upcoming event or order a piece made by Shady.</p>
      </section>

      <section className="offer-area">
        <div className="filters">
          {['All', 'Event', 'Product'].map(type => (
            <button
              key={type}
              className={filter === type ? 'selected' : ''}
              onClick={() => setFilter(type)}
            >
              {type === 'All' ? 'Everything' : `${type}s`}
            </button>
          ))}
        </div>

        <div className="offer-grid">
          {visible.map(item => (
            <article className="shop-card" key={item.id}>
              <img src={item.image} alt={item.title} />
              <div>
                <small>{item.type}</small>
                <h2>{item.title}</h2>
                <p>{item.text}</p>
                <button
                  className="paint-button"
                  onClick={() => { setSelected(item); setCopied(false) }}
                >
                  {item.action} <span>→</span>
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      {selected && (
        <div className="overlay" onClick={() => setSelected(null)}>
          <form
            className="request-modal"
            onClick={event => event.stopPropagation()}
            onSubmit={event => event.preventDefault()}
          >
            <button className="close" type="button" onClick={() => setSelected(null)}>×</button>
            <p className="eyebrow">{selected.type} REQUEST</p>
            <h2>{selected.title}</h2>
            <p>Complete your details, then choose how to contact Shady.</p>

            {[
              ['name', 'Name', 'text'],
              ['phone', 'Phone number', 'tel'],
              ['quantity', 'Quantity / attendees', 'number'],
              ['email', 'Email', 'email'],
              ['place', 'Place / delivery location', 'text'],
            ].map(([key, label, type]) => (
              <label key={key}>
                {label}
                <input
                  required
                  min={key === 'quantity' ? 1 : undefined}
                  type={type}
                  value={details[key]}
                  onChange={event => setDetails({ ...details, [key]: event.target.value })}
                />
              </label>
            ))}

            <div className="send-actions">
              <a
                className="paint-button"
                href={`${whatsapp}?text=${encodeURIComponent(message)}`}
                target="_blank"
                rel="noreferrer"
              >
                Send on WhatsApp
              </a>
              <button className="instagram-button" type="button" onClick={handleInstagram}>
                Continue on Instagram
              </button>
            </div>

            {copied && <p className="image-count">Request copied. Paste it into the Instagram message.</p>}
          </form>
        </div>
      )}
    </main>
  )
}