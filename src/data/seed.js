export const STORE = 'art-vision-portfolio-v1'
export const SESSION = 'art-vision-dashboard-session'

export const ADMIN_EMAIL =
    import.meta.env.VITE_ADMIN_EMAIL || 'shady@example.com'
export const ADMIN_PASSWORD =
    import.meta.env.VITE_ADMIN_PASSWORD || 'ChangeMe123!'

export const seed = {
    contacts: {
        whatsapp: 'https://wa.me/201201200208',
        instagram: 'https://www.instagram.com/art__vision.eg/',
        facebook: 'https://web.facebook.com/profile.php?id=61563374294991',
        email: '',
    },

    // كل مشاريع الرسم
    artProjects: [{
            id: 1,
            title: 'Luna Café Mural',
            description: 'A warm mural inspired by coffee, conversation, and the small stories shared around a table.',
            images: ['https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=1200&q=85'],
        },
        {
            id: 2,
            title: 'World of Colour',
            description: 'Playful, child-safe illustrations that turn a wall into a place for imagination and learning.',
            images: ['https://images.unsplash.com/photo-1580136579312-94651dfd596d?auto=format&fit=crop&w=1200&q=85'],
        },
        {
            id: 3,
            title: 'Coffee & Art',
            description: 'Soft colours and handmade details that make the space feel warmer and more memorable.',
            images: ['https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?auto=format&fit=crop&w=1200&q=85'],
        },
        {
            id: 4,
            title: 'Dream City',
            description: 'Characters and illustrated stories that invite children to discover their world with a smile.',
            images: ['https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=85'],
        },
    ],

    // الإيفنتات والمنتجات
    offers: [{
            id: 101,
            type: 'Event',
            title: 'Open Painting Day',
            description: 'A relaxed day of colour, painting, and creative conversation with our team.',
            action: 'Reserve a place',
            image: 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=1200&q=85',
            price: 250,
        },
        {
            id: 102,
            type: 'Product',
            title: 'Custom Art Print',
            description: 'A made-to-order art print designed to bring a little colour into your space.',
            action: 'Order now',
            image: 'https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?auto=format&fit=crop&w=1200&q=85',
            price: 100,
        },
    ],
}