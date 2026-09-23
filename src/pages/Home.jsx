import { useState } from 'react'
import { Link } from 'react-router-dom'
import { usePortfolio } from '../context/PortfolioContext'
import ContactLinks from '../components/ContactLinks'
import { Helmet } from 'react-helmet-async'
const WHATSAPP = 'https://wa.me/201201200208'

export default function Home() {
  const { data } = usePortfolio()
  const [selected, setSelected] = useState(null)
  const [filter, setFilter] = useState('All')
  const [details, setDetails] = useState({
    name: '',
    phone: '',
    quantity: '',
    place: '',
  })
  const [errors, setErrors] = useState({})
  const [touched, setTouched] = useState({})
  const [copied, setCopied] = useState(false)

  const offers = data.offers || []
  const visibleOffers =
    filter === 'All' ? offers : offers.filter(o => o.type === filter)

  // ───────── Validation ─────────
  const validateField = (key, value) => {
    const v = String(value || '').trim()
    switch (key) {
      case 'name':
        if (!v) return 'Please enter your name.'
        if (v.length < 2) return 'Name must be at least 2 characters.'
        return ''
      case 'phone':
        if (!v) return 'Please enter your phone number.'
        if (!/^[\d\s+\-()]{10,}$/.test(v))
          return 'Please enter a valid phone number (at least 10 digits).'
        return ''
      case 'quantity':
        if (!v) return 'Please enter a quantity.'
        if (Number(v) < 1) return 'Quantity must be at least 1.'
        return ''
      case 'place':
        if (!v) return 'Please enter a place or delivery location.'
        if (v.length < 3) return 'Place must be at least 3 characters.'
        return ''
      default:
        return ''
    }
  }

  const validateAll = () => {
    const newErrors = {}
    const fields = ['name', 'phone', 'quantity', 'place']
    fields.forEach(key => {
      const err = validateField(key, details[key])
      if (err) newErrors[key] = err
    })
    setErrors(newErrors)
    setTouched(fields.reduce((acc, k) => ({ ...acc, [k]: true }), {}))
    return Object.keys(newErrors).length === 0
  }

  const isValid = ['name', 'phone', 'quantity', 'place'].every(
    key => !validateField(key, details[key])
  )

  const handleChange = (key, value) => {
    setDetails(prev => ({ ...prev, [key]: value }))
    if (touched[key]) {
      setErrors(prev => ({ ...prev, [key]: validateField(key, value) }))
    }
  }

  const handleBlur = key => {
    setTouched(prev => ({ ...prev, [key]: true }))
    setErrors(prev => ({ ...prev, [key]: validateField(key, details[key]) }))
  }

  const handleOpenModal = item => {
    setSelected(item)
    setDetails({ name: '', phone: '', quantity: '', place: '' })
    setErrors({})
    setTouched({})
    setCopied(false)
  }

  // ───────── Price Calculation ─────────
  const unitPrice = selected?.price || 0
  const quantity = Number(details.quantity) || 0
  const totalPrice = unitPrice * quantity

  // ───────── Message ─────────
   // ───────── Message ─────────
    // ───────── Message ─────────
  const message =
    selected && isValid
      ? `New ${selected.type} request
${selected.title}

Name: ${details.name}
Phone: ${details.phone}
Quantity / attendees: ${details.quantity}${unitPrice ? `
Total: ${totalPrice} EGP` : ''}
Place / delivery location: ${details.place}`
      : ''
  const handleWhatsApp = e => {
    if (!validateAll()) {
      e.preventDefault()
    }
  }

  const handleInstagram = async e => {
    e.preventDefault()
    if (!validateAll()) return

    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(message)
      } else {
        const ta = document.createElement('textarea')
        ta.value = message
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
      }
      setCopied(true)
    } catch {
      setCopied(false)
    }
    window.open('https://ig.me/m/art__vision.eg', '_blank', 'noreferrer')
  }

  return (
    <main>
      <Helmet>
        <title>Art Vision · Murals & Events | Shady Gad</title>
        <meta name="description" content="Creative studio painting cafés, nurseries, and public spaces with colourful murals. Founded by Shady Gad in Egypt. Explore events, products, and custom art." />
        <link rel="canonical" href="https://artvision.eg/" />
        <meta property="og:title" content="Art Vision · Murals & Events" />
        <meta property="og:description" content="Colourful walls, made with heart. Murals, events, and art for cafés, nurseries, and public spaces." />
        <meta property="og:url" content="https://artvision.eg/" />
        <meta property="og:type" content="website" />
      </Helmet>

      {/* Floating paint drops background */}
      <div className="paint-drops-bg" aria-hidden="true">
        <span className="paint-drop pd1" />
        <span className="paint-drop pd2" />
        <span className="paint-drop pd3" />
        <span className="paint-drop pd4" />
        <span className="paint-drop pd5" />
      </div>

      {/* ───────── Header ───────── */}
      <header className="topbar">
        <Link className="brand" to="/">
          <span className="brand-mark">A</span>
          <span>ART <i>VISION</i></span>
        </Link>
        <a
          className="small-link"
          href={WHATSAPP}
          target="_blank"
          rel="noreferrer"
        >
          Send a message ↗
        </a>
      </header>

      {/* ───────── Hero ───────── */}
      <section className="hero">
        <span className="splatter" style={{
          top: '15%', left: '5%',
          width: 60, height: 60,
          background: '#F5A623', opacity: 0.15
        }} />
        <span className="splatter" style={{
          bottom: '20%', right: '8%',
          width: 90, height: 90,
          background: '#2C3E5C', opacity: 0.12
        }} />
        <span className="splatter" style={{
          top: '60%', left: '45%',
          width: 40, height: 40,
          background: '#F5A623', opacity: 0.2
        }} />

        <div className="hero-copy">
          <p className="eyebrow">WHERE ART MEETS VISION</p>
          <h1>
            Colourful walls,<br />
            <span>made with heart.</span>
          </h1>
          <p className="lead">
            Art Vision is a creative studio painting cafés, nurseries, and
            public spaces with stories worth remembering.
          </p>
          <p style={{ fontSize: 13, color: '#5A6377', marginTop: 10 }}>
            Founded by <strong style={{ color: '#1F2A44' }}>Shady Gad</strong>
          </p>
          <div className="hero-actions">
            <a className="paint-button" href="#events">
              Explore events <span>↓</span>
            </a>
            <Link to="/art">See our art</Link>
          </div>
        </div>

        <div className="hero-art">
          <div className="logo-glow" />
          <div className="logo-showcase">
            <img
              src="/Logo.png"
              alt="Art Vision Logo"
              className="hero-logo"
              draggable={false}
            />
          </div>
          <div className="dot dot1" />
          <div className="dot dot2" />
        </div>
      </section>

      {/* ───────── Intro ───────── */}
      <section className="intro reveal">
        <div>
          <h2>Events, products,<br />and art for everyone.</h2>
        </div>
        <p>
          From open painting days to custom prints, we bring art closer to
          people. Every project is made to feel right for its space and story.
        </p>
        <div className="chips">
          <span>Events</span>
          <span>Products</span>
          <span>Murals</span>
        </div>
      </section>

      {/* ───────── Events & Products ───────── */}
      <section className="portfolio" id="events">
        <div className="section-title reveal">
          <div>
            <p className="eyebrow">EVENTS & PRODUCTS</p>
            <h2>Join us or take art home.</h2>
          </div>
          <p>Filter by type to find what suits you.</p>
        </div>

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

        <div className="offer-grid" style={{ padding: '0 3vw' }}>
          {visibleOffers.map(item => (
            <article className="shop-card" key={item.id}>
              <img src={item.image} alt={item.title} />
              <div>
                <small>{item.type}</small>
                <h2>{item.title}</h2>
                <p>{item.description}</p>
                {item.price > 0 && (
                  <p className="offer-price">{item.price} EGP</p>
                )}
                <button
                  className="paint-button"
                  onClick={() => handleOpenModal(item)}
                >
                  {item.action} <span>→</span>
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      {/* ───────── Art Preview ───────── */}
      <section className="portfolio" id="art">
        <div className="section-title reveal">
          <div>
            <p className="eyebrow">RECENT ART</p>
            <h2>A glimpse of our walls.</h2>
          </div>
          <Link to="/art" className="small-link">
            See all art →
          </Link>
        </div>

        <div className="gallery-grid">
          {(data.artProjects || []).slice(0, 4).map((p, i) => (
            <Link
              className={'work work-' + (i % 4)}
              key={p.id}
              to="/art"
              style={{ textDecoration: 'none', display: 'block' }}
            >
              <img
                src={p.images[0]}
                alt={`${p.title} — mural by Art Vision`}
                loading={i === 0 ? 'eager' : 'lazy'}
                decoding="async"
                width="400"
                height="385"
              />
              <span className="work-info">
                <small>ART</small>
                <strong>{p.title}</strong>
                <i>View in gallery ↗</i>
              </span>
            </Link>
          ))}
        </div>
      </section>

      {/* ───────── Contact ───────── */}
      <section className="contact" id="contact">
        <p className="eyebrow">READY TO COLOUR YOUR SPACE?</p>
        <h2>Send a message create<br /><span>something together.</span></h2>

        <a
          className="paint-button"
          href={WHATSAPP}
          target="_blank"
          rel="noreferrer"
        >
          Talk on WhatsApp <span>↗</span>
        </a>

        <ContactLinks contacts={data.contacts} />

        <p
          style={{
            marginTop: 60,
            fontSize: 11,
            color: '#ffffff88',
            fontFamily: "'DM Mono'",
          }}
        >
          © {new Date().getFullYear()} Art Vision · Founded by WM_Solutions
        </p>
      </section>

      {/* ───────── Request Modal ───────── */}
      {selected && (
        <div className="overlay" onClick={() => setSelected(null)}>
          <form
            className="request-modal"
            onClick={e => e.stopPropagation()}
            onSubmit={e => e.preventDefault()}
            noValidate
          >
            <button
              className="close"
              type="button"
              onClick={() => setSelected(null)}
            >
              ×
            </button>

            <p className="eyebrow">{selected.type.toUpperCase()} REQUEST</p>
            <h2>{selected.title}</h2>
            <p>Fill in your details, then choose how to contact us.</p>

            {[
              ['name', 'Name', 'text', 'Your full name'],
              ['phone', 'Phone number', 'tel', 'e.g. 01201200208'],
              ['quantity', 'Quantity / attendees', 'number', '1'],
              ['place', 'Place / delivery location', 'text', 'e.g. Cairo, Maadi'],
            ].map(([key, label, type, placeholder]) => (
              <label key={key}>
                {label}
                <input
                  type={type}
                  min={key === 'quantity' ? 1 : undefined}
                  value={details[key]}
                  placeholder={placeholder}
                  onChange={e => handleChange(key, e.target.value)}
                  onBlur={() => handleBlur(key)}
                  className={touched[key] && errors[key] ? 'input-error' : ''}
                  aria-invalid={touched[key] && errors[key] ? 'true' : 'false'}
                />
                {touched[key] && errors[key] && (
                  <span className="field-error">{errors[key]}</span>
                )}
              </label>
            ))}

            {selected.price > 0 && (
              <div className="price-summary">
                <div className="price-row">
                  <span>Unit price</span>
                  <b>{selected.price} EGP</b>
                </div>
                <div className="price-row">
                  <span>Quantity</span>
                  <b>× {quantity || 0}</b>
                </div>
                <div className="price-row total">
                  <span>Total</span>
                  <b>{totalPrice} EGP</b>
                </div>
              </div>
            )}

            <div className="send-actions">
              <a
                className={`paint-button ${!isValid ? 'is-disabled' : ''}`}
                href={
                  isValid
                    ? `${WHATSAPP}?text=${encodeURIComponent(message)}`
                    : '#'
                }
                target={isValid ? '_blank' : undefined}
                rel="noreferrer"
                onClick={handleWhatsApp}
                aria-disabled={!isValid}
              >
                Send on WhatsApp
              </a>

              <button
                className={`instagram-button ${!isValid ? 'is-disabled' : ''}`}
                type="button"
                onClick={handleInstagram}
                disabled={!isValid}
              >
                Continue on Instagram
              </button>
            </div>

            {!isValid && (
              <p className="form-hint">
                Please fill all fields correctly to enable the send buttons.
              </p>
            )}

            {copied && (
              <p className="image-count">
                Request copied. Paste it into the Instagram message.
              </p>
            )}
          </form>
        </div>
      )}
    </main>
  )
}