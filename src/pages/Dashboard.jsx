import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { filesToData } from '../utils/filesToData'
import { usePortfolio } from '../context/PortfolioContext'

export default function Dashboard() {
  const { signed, error, login, logout } = useAuth()
  const { data, setData } = usePortfolio()

  const [creds, setCreds] = useState({ email: '', password: '' })
  const [notice, setNotice] = useState('')
  const [form, setForm] = useState({
    title: '',
    description: '',
    images: [],
    _files: null,
  })

  const handleLogin = e => {
    e.preventDefault()
    login(creds.email, creds.password)
  }

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

      setForm({ title: '', description: '', images: [], _files: null })
      setNotice('Project published on the website.')
    } catch (err) {
      console.error(err)
      setNotice('Failed to process images. Try smaller files.')
    }
  }

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
          {/* Add Project */}
          <form className="dashboard-card" onSubmit={addProject}>
            <h2>Add an art project</h2>

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
              Project photos
              <input
                type="file"
                accept="image/*"
                multiple
                required
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
                {form.images.length} image(s) selected — will be processed on
                publish
              </p>
            )}

            <button className="paint-button">
              Publish project <span>✦</span>
            </button>
          </form>

          {/* Contact Links */}
          <section className="dashboard-card">
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
        </div>

        {/* Published Projects */}
        <section className="dashboard-card projects-list">
          <h2>Published art projects</h2>
          {(data.artProjects || []).map(p => (
            <article key={p.id}>
              <img src={p.images[0]} alt="" />
              <div>
                <b>{p.title}</b>
                <span>{p.images.length} photo(s)</span>
              </div>
              <button
                onClick={() =>
                  setData({
                    ...data,
                    artProjects: data.artProjects.filter(x => x.id !== p.id),
                  })
                }
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