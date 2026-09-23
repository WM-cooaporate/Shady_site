import { useState } from 'react'
import { Link } from 'react-router-dom'
import { usePortfolio } from '../context/PortfolioContext'
import Collection from '../components/Collection'
import ContactLinks from '../components/ContactLinks'
import { Helmet } from 'react-helmet-async'
const WHATSAPP = 'https://wa.me/201201200208'

export default function ArtGallery() {
  const { data } = usePortfolio()
  const [open, setOpen] = useState(null)

  const projects = data.artProjects || []

  return (
    <main>
      <Helmet>
  <title>Art Gallery · Murals & Wall Art | Art Vision</title>
  <meta name="description" content="Explore Art Vision's collection of murals and wall art for cafés, nurseries, and public spaces. Each project tells a story, painted with care by Shady Gad." />
  <link rel="canonical" href="https://artvision.eg/art" />
  <meta property="og:title" content="Art Gallery · Murals & Wall Art | Art Vision" />
  <meta property="og:description" content="Every wall has a story. Explore our collection of murals and wall art." />
  <meta property="og:url" content="https://artvision.eg/art" />
  <meta property="og:type" content="website" />
</Helmet>
      {/* ───────── Header ───────── */}<header className="topbar">
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
    Send a message  ↗
  </a>
</header>
      {/* ───────── Hero ───────── */}
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">OUR ART GALLERY</p>
          <h1>
            Every wall<br />
            <span>has a story.</span>
          </h1>
          <p className="lead">
            From cafés to nurseries and everything in between — here's a
            collection of the walls we've painted with love.
          </p>
          <div className="hero-actions">
            <a className="paint-button" href="#gallery">
              Explore the art <span>↓</span>
            </a>
            <Link to="/">← Back to home</Link>
          </div>
        </div>

        <div className="hero-art">
          <div className="sun" />
          <div className="canvas">
            <div className="paint-stroke stroke-1" />
            <div className="paint-stroke stroke-2" />
            <div className="paint-stroke stroke-3" />
            <span>OUR<br />ART<br />WORK</span>
          </div>
          <div className="brush">🎨</div>
          <div className="dot dot1" />
          <div className="dot dot2" />
        </div>
      </section>

      {/* ───────── Gallery ───────── */}
      <section className="portfolio" id="gallery">
        <div className="section-title reveal">
          <div>
            <p className="eyebrow">SELECTED WORK</p>
            <h2>Painted with care.</h2>
          </div>
          <p>Click any project to see the full collection.</p>
        </div>

        {projects.length > 0 ? (
          <div className="gallery-grid">
            {projects.map((p, i) => (
              <button
                className={'work work-' + (i % 4)}
                key={p.id}
                onClick={() => setOpen(p)}
              >
<img
  src={p.images[0]}
  alt={`${p.title} — mural collection by Art Vision`}
  loading={i < 4 ? 'eager' : 'lazy'}
  decoding="async"
  width="400"
  height="385"
/>                <span className="work-info">
                  <small>ART</small>
                  <strong>{p.title}</strong>
                  <i>View collection ↗</i>
                </span>
              </button>
            ))}
          </div>
        ) : (
          <p className="empty">No art projects yet. Check back soon!</p>
        )}
      </section>

      {/* ───────── Contact ───────── */}
      <section className="contact">
        <p className="eyebrow">WANT A MURAL LIKE THIS?</p>
        <h2>Send a message paint<br /><span>your space.</span></h2>

        <a
          className="paint-button"
          href={WHATSAPP}
          target="_blank"
          rel="noreferrer"
        >
          Talk on WhatsApp <span>↗</span>
        </a>

        <ContactLinks contacts={data.contacts} />

        <p style={{ marginTop: 60, fontSize: 11, color: '#ffffff88', fontFamily: "'DM Mono'" }}>
          © {new Date().getFullYear()} Art Vision · Founded by WM_Solutions
        </p>
      </section>

      {/* ───────── Collection Modal ───────── */}
      {open && (
        <Collection
          project={open}
          projects={projects}
          onClose={() => setOpen(null)}
        />
      )}
    </main>
  )
}