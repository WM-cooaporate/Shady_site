import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <main className="not-found">
      <div className="not-found-content">
        <p className="eyebrow">404 · PAGE NOT FOUND</p>
        <h1>This wall is<br /><span>still blank.</span></h1>
        <p className="lead">
          The page you're looking for doesn't exist — or maybe it moved.
        </p>
        <Link className="paint-button" to="/">
          Back to home <span>→</span>
        </Link>
      </div>
      <div className="not-found-art">
        <div className="paint-stroke stroke-1" />
        <div className="paint-stroke stroke-2" />
        <div className="paint-stroke stroke-3" />
        <span>?</span>
      </div>
    </main>
  )
}