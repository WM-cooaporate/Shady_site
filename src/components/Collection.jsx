import { useState, useEffect, useRef, useCallback } from 'react'

const MIN_SCALE = 1
const MAX_SCALE = 5
const SCALE_STEP = 0.3

export default function Collection({ project, projects, onClose }) {
  const [active, setActive] = useState(project)
  const [imageIndex, setImageIndex] = useState(0)
  const [scale, setScale] = useState(1)
  const [position, setPosition] = useState({ x: 0, y: 0 })
  const dragging = useRef(false)
  const dragStart = useRef({ x: 0, y: 0 })
  const touchStart = useRef(null)
  const pinchStart = useRef(null)

  const images = active.images || []
  const total = images.length
  const currentImage = images[imageIndex]
  const isZoomed = scale > 1

  // ───────── Reset on project change ─────────
  useEffect(() => {
    setImageIndex(0)
    setScale(1)
    setPosition({ x: 0, y: 0 })
  }, [active])

  // ───────── Navigation ─────────
  const next = useCallback(() => {
    if (total < 2) return
    setImageIndex(i => (i + 1) % total)
    setScale(1)
    setPosition({ x: 0, y: 0 })
  }, [total])

  const prev = useCallback(() => {
    if (total < 2) return
    setImageIndex(i => (i - 1 + total) % total)
    setScale(1)
    setPosition({ x: 0, y: 0 })
  }, [total])

  const goTo = i => {
    setImageIndex(i)
    setScale(1)
    setPosition({ x: 0, y: 0 })
  }

  const resetZoom = () => {
    setScale(1)
    setPosition({ x: 0, y: 0 })
  }

  // ───────── Keyboard ─────────
  useEffect(() => {
    const onKey = e => {
      if (e.key === 'Escape') {
        if (isZoomed) resetZoom()
        else onClose()
      }
      if (e.key === 'ArrowRight') next()
      if (e.key === 'ArrowLeft') prev()
      if (e.key === '+' || e.key === '=') {
        setScale(s => Math.min(MAX_SCALE, s + SCALE_STEP))
      }
      if (e.key === '-') {
        setScale(s => {
          const nextScale = Math.max(MIN_SCALE, s - SCALE_STEP)
          if (nextScale === MIN_SCALE) setPosition({ x: 0, y: 0 })
          return nextScale
        })
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [next, prev, onClose, isZoomed])

  // ───────── Wheel zoom (desktop) ─────────
  const onWheel = e => {
    e.preventDefault()
    if (e.deltaY < 0) {
      // scroll up = zoom in
      setScale(s => Math.min(MAX_SCALE, s + SCALE_STEP))
    } else {
      // scroll down = zoom out
      setScale(s => {
        const nextScale = Math.max(MIN_SCALE, s - SCALE_STEP)
        if (nextScale === MIN_SCALE) setPosition({ x: 0, y: 0 })
        return nextScale
      })
    }
  }

  // ───────── Drag to pan ─────────
  const onMouseDown = e => {
    if (!isZoomed) return
    dragging.current = true
    dragStart.current = {
      x: e.clientX - position.x,
      y: e.clientY - position.y,
    }
  }

  const onMouseMove = e => {
    if (!dragging.current || !isZoomed) return
    setPosition({
      x: e.clientX - dragStart.current.x,
      y: e.clientY - dragStart.current.y,
    })
  }

  const onMouseUp = () => {
    dragging.current = false
  }

  // ───────── Touch: swipe + pinch ─────────
  const onTouchStart = e => {
    if (e.touches.length === 2) {
      // start pinch
      const dx = e.touches[0].clientX - e.touches[1].clientX
      const dy = e.touches[0].clientY - e.touches[1].clientY
      pinchStart.current = {
        distance: Math.hypot(dx, dy),
        scale,
      }
      touchStart.current = null
    } else if (e.touches.length === 1) {
      touchStart.current = e.touches[0].clientX
    }
  }

  const onTouchMove = e => {
    if (e.touches.length === 2 && pinchStart.current) {
      e.preventDefault()
      const dx = e.touches[0].clientX - e.touches[1].clientX
      const dy = e.touches[0].clientY - e.touches[1].clientY
      const newDistance = Math.hypot(dx, dy)
      const ratio = newDistance / pinchStart.current.distance
      const newScale = Math.min(
        MAX_SCALE,
        Math.max(MIN_SCALE, pinchStart.current.scale * ratio)
      )
      setScale(newScale)
      if (newScale === MIN_SCALE) setPosition({ x: 0, y: 0 })
    }
  }

  const onTouchEnd = e => {
    if (pinchStart.current) {
      pinchStart.current = null
      return
    }
    if (touchStart.current === null) return
    const diff = touchStart.current - e.changedTouches[0].clientX
    if (Math.abs(diff) > 50 && !isZoomed) {
      if (diff > 0) next()
      else prev()
    }
    touchStart.current = null
  }

  // ───────── Stage click: zoom in only when not zoomed ─────────
  const handleStageClick = () => {
    if (!isZoomed) {
      setScale(2)
    }
    // لا يحدث شيء لو مكبر — المستخدم يسحب، يعمل scroll، أو يضغط Reset
  }

  return (
    <div className="overlay" onClick={onClose}>
      <section
        className="collection-modal"
        onClick={e => e.stopPropagation()}
      >
        <button className="close" onClick={onClose} aria-label="Close">
          ×
        </button>

        {/* ───────── Project info ───────── */}
        <div className="collection-info">
          <div>
            <p className="eyebrow">ART PROJECT</p>
            <h2>{active.title}</h2>
          </div>
          <p className="collection-description">{active.description}</p>
        </div>

        {/* ───────── Image slider ───────── */}
        {total > 0 && (
          <div className="slider-wrap">
            <div
              className={`slider-stage ${isZoomed ? 'is-zoom' : ''}`}
              onMouseDown={onMouseDown}
              onMouseMove={onMouseMove}
              onMouseUp={onMouseUp}
              onMouseLeave={onMouseUp}
              onTouchStart={onTouchStart}
              onTouchMove={onTouchMove}
              onTouchEnd={onTouchEnd}
              onWheel={onWheel}
              onClick={handleStageClick}
            >
              <img
                src={currentImage}
                alt={`${active.title} ${imageIndex + 1}`}
                draggable={false}
                style={{
                  transform: `translate(${position.x}px, ${position.y}px) scale(${scale})`,
                  cursor: isZoomed
                    ? dragging.current
                      ? 'grabbing'
                      : 'grab'
                    : 'zoom-in',
                }}
              />

              {/* Prev / Next — تختفي عند الـ zoom */}
              {total > 1 && !isZoomed && (
                <>
                  <button
                    className="slider-nav prev"
                    onClick={e => {
                      e.stopPropagation()
                      prev()
                    }}
                    aria-label="Previous image"
                  >
                    ←
                  </button>
                  <button
                    className="slider-nav next"
                    onClick={e => {
                      e.stopPropagation()
                      next()
                    }}
                    aria-label="Next image"
                  >
                    →
                  </button>
                </>
              )}

              {/* Counter */}
              <div className="slider-counter">
                {imageIndex + 1} / {total}
                {isZoomed && ` · ${Math.round(scale * 100)}%`}
              </div>

              {/* Zoom controls */}
              <div
                className="slider-zoom-controls"
                onClick={e => e.stopPropagation()}
              >
                {isZoomed && (
                  <>
                    <button
                      type="button"
                      className="zoom-btn"
                      onClick={() =>
                        setScale(s => {
                          const ns = Math.max(MIN_SCALE, s - SCALE_STEP)
                          if (ns === MIN_SCALE) setPosition({ x: 0, y: 0 })
                          return ns
                        })
                      }
                      aria-label="Zoom out"
                    >
                      −
                    </button>
                    <button
                      type="button"
                      className="zoom-btn"
                      onClick={() =>
                        setScale(s => Math.min(MAX_SCALE, s + SCALE_STEP))
                      }
                      aria-label="Zoom in"
                    >
                      +
                    </button>
                    <button
                      type="button"
                      className="zoom-btn reset"
                      onClick={resetZoom}
                      aria-label="Reset zoom"
                    >
                      Reset
                    </button>
                  </>
                )}
              </div>

              {/* Hint */}
              {!isZoomed && (
                <div className="slider-hint">
                  🔍 Click to zoom · Scroll / Pinch to adjust
                </div>
              )}
            </div>

            {/* Thumbnails */}
            {total > 1 && (
              <div className="thumbs-row">
                {images.map((img, i) => (
                  <button
                    key={i}
                    className={`thumb ${i === imageIndex ? 'active' : ''}`}
                    onClick={() => goTo(i)}
                    aria-label={`Go to image ${i + 1}`}
                  >
                    <img src={img} alt={`Thumbnail ${i + 1}`} />
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ───────── Other projects ───────── */}
        <h3>More art work</h3>
        <div className="collection-list">
          {projects.map(item => (
            <button
              key={item.id}
              className={item.id === active.id ? 'active-project' : ''}
              onClick={() => setActive(item)}
            >
              {item.title}
            </button>
          ))}
        </div>
      </section>
    </div>
  )
}