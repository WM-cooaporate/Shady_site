export default function ContactLinks({ contacts }) {
  const items = [
    ['WhatsApp', contacts.whatsapp],
    ['Instagram', contacts.instagram],
    ['Facebook', contacts.facebook],
    ['Email', contacts.email && `mailto:${contacts.email}`],
  ].filter(([, url]) => url)

  if (!items.length) return null

  return (
    <div className="socials">
      {items.map(([label, url]) => (
        <a
          key={label}
          href={url}
          target={label === 'Email' ? undefined : '_blank'}
          rel="noreferrer"
        >
          {label}
        </a>
      ))}
    </div>
  )
}