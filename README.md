# 🎓 LIB Education - Modern Education Institute Website

A modern, responsive, and visually stunning website for LIB Education coaching institute. Built with HTML, CSS, and JavaScript with a focus on user experience and easy content management.

## ✨ Features

- **Modern & Premium Design**: Beautiful gradient backgrounds, smooth animations, and professional aesthetics
- **Fully Responsive**: Works perfectly on desktop, tablet, and mobile devices
- **Dynamic Content Loading**: All content loaded from a single JSON configuration file
- **SEO Optimized**: Semantic HTML, meta tags, and proper heading structure
- **Interactive Components**: Smooth scrolling, form validation, animated statistics
- **Easy to Customize**: Simple JSON-based content management
- **No Dependencies**: Pure vanilla JavaScript, HTML, and CSS

## 📁 Project Structure

```
Tuition class website/
│
├── index.html              # Main HTML file
├── README.md              # This file
│
├── config/
│   └── courses.json       # All website content (editable)
│
├── css/
│   └── style.css          # All styles and design system
│
└── js/
    └── app.js             # All JavaScript functionality
```

## 🚀 Getting Started

### Quick Start

1. **Open the website**: Simply open `index.html` in any modern web browser (Chrome, Firefox, Edge, Safari)
2. **No server required**: The website works directly from the file system
3. **For development**: Use a local server for the best experience:
   ```
   # If you have Python installed:
   python -m http.server 8000
   
   # If you have Node.js installed:
   npx serve
   
   # Or use VS Code's Live Server extension
   ```

### Editing Content

All website content can be easily modified by editing the `config/courses.json` file:

#### Change Institute Name
```json
{
  "institute": {
    "name": "Your Institute Name",
    "tagline": "Your Tagline Here",
    ...
  }
}
```

#### Add/Edit Courses
```json
{
  "courses": [
    {
      "id": 1,
      "title": "Course Name",
      "description": "Course description",
      "icon": "📚",
      "color": "#3B82F6",
      "highlights": [
        "Feature 1",
        "Feature 2",
        "Feature 3"
      ]
    }
  ]
}
```

#### Update Contact Information
```json
{
  "contact": {
    "phone": "Your Phone Number",
    "email": "your@email.com",
    "address": "Your Full Address",
    "mapEmbedUrl": "Google Maps embed URL"
  }
}
```

### Getting Google Maps Embed URL

1. Go to [Google Maps](https://www.google.com/maps)
2. Find your location
3. Click "Share" → "Embed a map"
4. Copy the URL from the iframe src attribute
5. Paste it in the `mapEmbedUrl` field in `config/courses.json`

## 🎨 Customization Guide

### Changing Colors

Edit the CSS variables in `css/style.css`:

```css
:root {
  --primary-600: #2563eb;  /* Main brand color */
  --accent-primary: #10b981;  /* Success/accent color */
  /* ... more colors */
}
```

### Changing Fonts

The website uses Google Fonts (Inter and Poppins). To change fonts:

1. Visit [Google Fonts](https://fonts.google.com)
2. Select your preferred fonts
3. Update the `<link>` tag in `index.html`
4. Update the CSS variables in `css/style.css`:

```css
:root {
  --font-primary: 'YourFont', sans-serif;
  --font-display: 'YourDisplayFont', sans-serif;
}
```

### Adding New Sections

1. Add HTML in `index.html`:
```html
<section class="section" id="new-section">
  <div class="container">
    <div class="section-header">
      <h2 class="section-title">Section Title</h2>
      <p class="section-subtitle">Subtitle here</p>
    </div>
    <!-- Your content -->
  </div>
</section>
```

2. Add navigation link in the header:
```html
<li><a href="#new-section" class="nav-link">New Section</a></li>
```

## 📱 Sections Overview

### 1. Hero Section
- Eye-catching gradient background
- Institute name and tagline
- Call-to-action buttons

### 2. Statistics
- Dynamic animated numbers
- Student count, success rate, experience, teachers

### 3. About Us
- Institute description
- Mission and vision
- Teaching methodology

### 4. Courses
- Card-based layout for all courses
- Icons, descriptions, and highlights
- "Learn More" buttons

### 5. Features
- Key benefits and features
- Icon-based cards
- Hover effects

### 6. Contact
- Contact form with validation
- Contact information
- Google Maps integration
- Social media links

### 7. Footer
- Quick links
- Popular courses
- Contact details
- Social media icons

## 🛠️ Technical Details

### Technologies Used
- **HTML5**: Semantic markup, SEO-friendly structure
- **CSS3**: Modern design system, CSS Grid, Flexbox, animations
- **JavaScript (ES6+)**: Dynamic content loading, form validation, smooth scrolling

### Browser Support
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

### Performance
- Optimized CSS with CSS variables
- Efficient JavaScript with event delegation
- Lazy loading for images
- Smooth 60fps animations

## 📝 Form Functionality

The contact form includes:
- Real-time validation
- Required field checking
- Email format validation
- Phone number validation
- Success/error notifications

**Note**: The form currently logs submissions to the console. To integrate with a backend:

1. Modify the `simulateFormSubmission` function in `js/app.js`
2. Replace with your actual API endpoint:

```javascript
async function submitFormToBackend(data) {
    const response = await fetch('https://your-api.com/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    return response.json();
}
```

## 🎯 SEO Best Practices

The website includes:
- ✅ Proper meta tags and descriptions
- ✅ Semantic HTML5 structure
- ✅ Heading hierarchy (H1, H2, H3)
- ✅ Alt text for images
- ✅ Mobile-friendly viewport
- ✅ Fast loading times
- ✅ Unique page title

## 🔧 Troubleshooting

### Content not loading?
- Make sure `config/courses.json` is in the correct location
- Check browser console for errors
- Ensure you're using a modern browser

### Styles not working?
- Verify `css/style.css` path is correct
- Clear browser cache
- Check for CSS file loading errors

### JavaScript not working?
- Check browser console for errors
- Ensure `js/app.js` is loaded correctly
- Verify JSON file is valid (use JSONLint.com)

## 📧 Support

For questions or issues:
- Email: info@libeducation.com
- Phone: +91 98765 43210

## 📄 License

This project is free to use and modify for your educational institution.

## 🙏 Credits

- Design: Modern web design principles
- Fonts: Google Fonts (Inter & Poppins)
- Icons: Emoji icons for universal compatibility

---

**Built with ❤️ for quality education**

Last Updated: February 2026
