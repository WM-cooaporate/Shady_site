import { Link } from 'react-router-dom'
import { Helmet } from 'react-helmet-async'
export default function NotFound() {
  return (
    <main className="not-found">
    <Helmet>
    <title>Page Not Found (404) · Art Vision</title>
    <meta name="description" content="The page you're looking for doesn't exist — or maybe it moved. Return to Art Vision homepage to explore our murals and events." />
    <meta name="robots" content="noindex, follow" />
    <meta name="googlebot" content="noindex, follow" />
  </Helmet>
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