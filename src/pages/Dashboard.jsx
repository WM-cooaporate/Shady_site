import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { filesToData } from '../utils/filesToData'
import { usePortfolio } from '../context/PortfolioContext'

export default function Dashboard() {
  const { signed, error, login, logout } = useAuth()
  const { data, setData } = usePortfolio()

  // ───────── Login State ─────────
  const [creds, setCreds] = useState({ email: '', password: '' })

  // ───────── Notice ─────────
  const [notice, setNotice] = useState('')

  // ───────── Project Form ─────────
  const emptyProject = { title: '', description: '', images: [], _files: null }
  const [form, setForm] = useState(emptyProject)
  const [editingProjectId, setEditingProjectId] = useState(null)

  // ───────── Offer Form ─────────
  const emptyOffer = {
    title: '',
    type: 'Event',
    description: '',
    action: '',
    image: '',
    _file: null,
  }
  const [offerForm, setOfferForm] = useState(emptyOffer)
  const [editingOfferId, setEditingOfferId] = useState(null)

  // ───────── Login ─────────
  const handleLogin = e => {
    e.preventDefault()
    login(creds.email, creds.password)
  }

  // ═══════════════════════════════════════
  //             PROJECT ACTIONS
  // ═══════════════════════════════════════
  const addProject = async e => {
    e.preventDefault()

    if (!form._files || !form._files.length) {
      return setNotice('Please select at least one project image.')
    }

    setNotice('Processing images...')

    try {
      const base64Images = await filesToData(form._files)

      setData({
        ...data,
        artProjects: [
          {
            id: Date.now(),
            title: form.title,
            description: form.description,
            images: base64Images,
          },
          ...(data.artProjects || []),
        ],
      })

      setForm(emptyProject)
      setNotice('Project published on the website.')
    } catch (err) {
      console.error(err)
      setNotice('Failed to process images. Try smaller files.')
    }
  }

  const updateProject = async e => {
    e.preventDefault()

    const newImages =
      form._files && form._files.length
        ? await filesToData(form._files)
        : null

    setData({
      ...data,
      artProjects: (data.artProjects || []).map(p =>
        p.id === editingProjectId
          ? {
              ...p,
              title: form.title,
              description: form.description,
              images: newImages || p.images,
            }
          : p
      ),
    })

    setForm(emptyProject)
    setEditingProjectId(null)
    setNotice('Project updated successfully.')
  }

  const startEditProject = p => {
    setEditingProjectId(p.id)
    setForm({
      title: p.title,
      description: p.description,
      images: p.images.map((_, i) => `image ${i + 1}`),
      _files: null,
    })
    setNotice('')
    // scroll للفورم
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const cancelEditProject = () => {
    setEditingProjectId(null)
    setForm(emptyProject)
  }

  const removeProject = id => {
    setData({
      ...data,
      artProjects: (data.artProjects || []).filter(x => x.id !== id),
    })
  }

  // ═══════════════════════════════════════
  //              OFFER ACTIONS
  // ═══════════════════════════════════════
  const addOffer = async e => {
    e.preventDefault()

    if (!offerForm._file) {
      return setNotice('Please select an image for the offer.')
    }

    setNotice('Processing image...')

    try {
      const [base64Image] = await filesToData([offerForm._file])

      setData({
        ...data,
        offers: [
          {
            id: Date.now(),
            type: offerForm.type,
            title: offerForm.title,
            description: offerForm.description,
            action: offerForm.action,
            image: base64Image,
          },
          ...(data.offers || []),
        ],
      })

      setOfferForm(emptyOffer)
      setNotice('Offer published on the website.')
    } catch (err) {
      console.error(err)
      setNotice('Failed to process image. Try a smaller file.')
    }
  }

  const updateOffer = async e => {
    e.preventDefault()

    const newImage = offerForm._file
      ? (await filesToData([offerForm._file]))[0]
      : null

    setData({
      ...data,
      offers: (data.offers || []).map(o =>
        o.id === editingOfferId
          ? {
              ...o,
              type: offerForm.type,
              title: offerForm.title,
              description: offerForm.description,
              action: offerForm.action,
              image: newImage || o.image,
            }
          : o
      ),
    })

    setOfferForm(emptyOffer)
    setEditingOfferId(null)
    setNotice('Offer updated successfully.')
  }

  const startEditOffer = o => {
    setEditingOfferId(o.id)
    setOfferForm({
      title: o.title,
      type: o.type,
      description: o.description,
      action: o.action,
      image: o.image,
      _file: null,
    })
    setNotice('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const cancelEditOffer = () => {
    setEditingOfferId(null)
    setOfferForm(emptyOffer)
  }

  const removeOffer = id => {
    setData({
      ...data,
      offers: (data.offers || []).filter(x => x.id !== id),
    })
  }

  // ═══════════════════════════════════════
  //             CONTACT LINKS
  // ═══════════════════════════════════════
  const updateContact = (key, value) =>
    setData({ ...data, contacts: { ...data.contacts, [key]: value } })

  // ───────── Login Screen ─────────
  if (!signed) {
    return (
      <main className="dashboard-shell">
        <section className="login-card">
          <Link className="brand dashboard-brand" to="/">
            <span className="brand-mark">A</span>
            <span>ART VISION <i>PRIVATE DASHBOARD</i></span>
          </Link>
          <p className="eyebrow">PRIVATE ACCESS</p>
          <h1>Sign in to manage the portfolio.</h1>

          <form onSubmit={handleLogin}>
            <label>
              Email
              <input
                type="email"
                required
                value={creds.email}
                onChange={e => setCreds({ ...creds, email: e.target.value })}
              />
            </label>
            <label>
              Password
              <input
                type="password"
                required
                value={creds.password}
                onChange={e => setCreds({ ...creds, password: e.target.value })}
              />
            </label>
            {error && <p className="error">{error}</p>}
            <button className="paint-button">
              Sign in <span>→</span>
            </button>
          </form>
        </section>
      </main>
    )
  }

  // ───────── Dashboard Screen ─────────
  return (
    <main className="dashboard-shell">
      <header className="dashboard-header">
        <Link className="brand" to="/">
          <span className="brand-mark">A</span>
          <span>ART VISION <i>PRIVATE DASHBOARD</i></span>
        </Link>
        <button className="small-link" onClick={logout}>
          Sign out
        </button>
      </header>

      <section className="dashboard">
        <p className="eyebrow">PORTFOLIO MANAGER</p>
        <h1>Update your art and contact links.</h1>
        <p className="dashboard-lead">
          Projects and links are saved instantly in this browser and appear on
          the public website.
        </p>

        {notice && (
          <p className="notice">
            {notice}
            {notice.includes('published') && (
              <>
                {' '}
                <Link
                  to="/art"
                  style={{
                    color: '#D9891B',
                    textDecoration: 'underline',
                    marginLeft: 6,
                    fontWeight: 700,
                  }}
                >
                  View on gallery →
                </Link>
              </>
            )}
          </p>
        )}

        <div className="dashboard-grid">
          {/* ═════════ Add/Edit Art Project ═════════ */}
          <form
            className="dashboard-card"
            onSubmit={editingProjectId ? updateProject : addProject}
          >
            <h2>
              {editingProjectId ? 'Edit art project' : 'Add an art project'}
            </h2>

            <label>
              Project title
              <input
                required
                value={form.title}
                onChange={e => setForm({ ...form, title: e.target.value })}
                placeholder="e.g. Little Explorers Nursery"
              />
            </label>

            <label>
              Description
              <textarea
                required
                value={form.description}
                onChange={e =>
                  setForm({ ...form, description: e.target.value })
                }
                placeholder="Describe the artwork and the space."
              />
            </label>

            <label>
              Project photos {editingProjectId && '(leave empty to keep current)'}
              <input
                type="file"
                accept="image/*"
                multiple
                required={!editingProjectId}
                onChange={e => {
                  const files = e.target.files
                  if (!files || !files.length) return
                  setForm(prev => ({
                    ...prev,
                    images: Array.from(files).map(f => f.name),
                    _files: files,
                  }))
                }}
              />
              <small>Select multiple photos for one project.</small>
            </label>

            {form.images.length > 0 && (
              <p className="image-count">
                {form.images.length} image(s) ready
              </p>
            )}

            <div style={{ display: 'flex', gap: 8 }}>
              <button className="paint-button">
                {editingProjectId ? (
                  <>Save changes <span>✓</span></>
                ) : (
                  <>Publish project <span>✦</span></>
                )}
              </button>

              {editingProjectId && (
                <button
                  type="button"
                  className="instagram-button"
                  onClick={cancelEditProject}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>

          {/* ═════════ Add/Edit Offer ═════════ */}
          <form
            className="dashboard-card"
            onSubmit={editingOfferId ? updateOffer : addOffer}
          >
            <h2>
              {editingOfferId
                ? 'Edit event / product'
                : 'Add an event or product'}
            </h2>

            <label>
              Title
              <input
                required
                value={offerForm.title}
                onChange={e =>
                  setOfferForm({ ...offerForm, title: e.target.value })
                }
                placeholder="e.g. Open Painting Day"
              />
            </label>

            <label>
              Type
              <select
                value={offerForm.type}
                onChange={e =>
                  setOfferForm({ ...offerForm, type: e.target.value })
                }
              >
                <option value="Event">Event</option>
                <option value="Product">Product</option>
              </select>
            </label>

            <label>
              Description
              <textarea
                required
                value={offerForm.description}
                onChange={e =>
                  setOfferForm({ ...offerForm, description: e.target.value })
                }
                placeholder="Describe the event or product."
              />
            </label>

            <label>
              Button text (action)
              <input
                required
                value={offerForm.action}
                onChange={e =>
                  setOfferForm({ ...offerForm, action: e.target.value })
                }
                placeholder="e.g. Reserve a place / Order now"
              />
            </label>

            <label>
              Image {editingOfferId && '(leave empty to keep current)'}
              <input
                type="file"
                accept="image/*"
                required={!editingOfferId}
                onChange={e => {
                  const file = e.target.files?.[0]
                  if (!file) return
                  setOfferForm({ ...offerForm, _file: file })
                }}
              />
              <small>One image for each offer.</small>
            </label>

            {offerForm._file && (
              <p className="image-count">New image selected</p>
            )}

            <div style={{ display: 'flex', gap: 8 }}>
              <button className="paint-button">
                {editingOfferId ? (
                  <>Save changes <span>✓</span></>
                ) : (
                  <>Publish offer <span>✦</span></>
                )}
              </button>

              {editingOfferId && (
                <button
                  type="button"
                  className="instagram-button"
                  onClick={cancelEditOffer}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>

        {/* ═════════ Contact Links ═════════ */}
        <section
          className="dashboard-card"
          style={{ marginTop: 18 }}
        >
          <h2>Contact links</h2>
          {[
            ['whatsapp', 'WhatsApp URL', 'https://wa.me/201...', 'url'],
            ['instagram', 'Instagram', 'https://instagram.com/art__vision.eg', 'url'],
            ['facebook', 'Facebook', 'https://facebook.com/...', 'url'],
            ['email', 'Business email', 'hello@example.com', 'email'],
          ].map(([key, label, placeholder, type]) => (
            <label key={key}>
              {label}
              <input
                type={type}
                value={data.contacts[key] || ''}
                onChange={e => updateContact(key, e.target.value)}
                placeholder={placeholder}
              />
            </label>
          ))}
          <p className="image-count">Links save automatically.</p>
        </section>

        {/* ═════════ Published Projects ═════════ */}
        <section className="dashboard-card projects-list">
          <h2>Published art projects</h2>
          {(data.artProjects || []).length === 0 && (
            <p className="image-count">No projects yet.</p>
          )}
          {(data.artProjects || []).map(p => (
            <article key={p.id}>
              <img src={p.images[0]} alt="" />
              <div>
                <b>{p.title}</b>
                <span>{p.images.length} photo(s)</span>
              </div>
              <button
                className="edit-btn"
                onClick={() => startEditProject(p)}
                title="Edit"
              >
                ✏️
              </button>
              <button
                className="remove-btn"
                onClick={() => removeProject(p.id)}
                title="Remove"
              >
                Remove
              </button>
            </article>
          ))}
        </section>

        {/* ═════════ Published Offers ═════════ */}
        <section className="dashboard-card projects-list">
          <h2>Published events & products</h2>
          {(data.offers || []).length === 0 && (
            <p className="image-count">No offers yet.</p>
          )}
          {(data.offers || []).map(o => (
            <article key={o.id}>
              <img src={o.image} alt="" />
              <div>
                <small>{o.type}</small>
                <b>{o.title}</b>
                <span>{o.action}</span>
              </div>
              <button
                className="edit-btn"
                onClick={() => startEditOffer(o)}
                title="Edit"
              >
                ✏️
              </button>
              <button
                className="remove-btn"
                onClick={() => removeOffer(o.id)}
                title="Remove"
              >
                Remove
              </button>
            </article>
          ))}
        </section>
      </section>
    </main>
  )
}