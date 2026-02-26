# 🚀 Quick Start Guide - LIB Education Website

## How to View the Website

### Method 1: Double-click to Open (Easiest)
1. Double-click `launch.bat` 
   OR
2. Double-click `index.html`

The website will open in your default web browser!

### Method 2: Using a Local Server (Recommended for Development)
```bash
# Navigate to the project folder
cd "d:\Tuition class website"

# Start a simple server (choose one):

# Option A - Python (if installed)
python -m http.server 8000

# Option B - Node.js (if installed)
npx serve

# Option C - PHP (if installed)
php -S localhost:8000
```

Then open: http://localhost:8000

## 📝 Quick Customization Checklist

### ✅ Update Institute Information
Edit `config/courses.json` and change:
- [ ] Institute name
- [ ] Tagline
- [ ] Description
- [ ] Mission & Vision

### ✅ Update Contact Details
Edit `config/courses.json` contact section:
- [ ] Phone number
- [ ] Email address
- [ ] Physical address
- [ ] Google Maps URL

### ✅ Customize Courses
Edit `config/courses.json` courses array:
- [ ] Add/remove courses
- [ ] Update course descriptions
- [ ] Change course icons (use emojis)
- [ ] Modify course colors

### ✅ Update Statistics
Edit `config/courses.json` statistics array:
- [ ] Student count
- [ ] Success rate
- [ ] Years of experience
- [ ] Number of teachers

### ✅ Customize Colors (Optional)
Edit `css/style.css` variables:
- [ ] Primary colors
- [ ] Accent colors
- [ ] Gradients

## 🎨 Design Features

✨ **Modern Aesthetics**
- Vibrant blue gradient hero section
- Smooth hover animations on cards
- Professional typography (Inter + Poppins)
- Glassmorphism effects

📱 **Fully Responsive**
- Mobile-first design
- Hamburger menu on mobile
- Touch-friendly buttons
- Optimized for all screen sizes

⚡ **Interactive Features**
- Smooth scroll navigation
- Animated statistics counter
- Form validation with real-time feedback
- Scroll-reveal animations
- Sticky header with blur effect

## 📋 Website Sections

1. **Hero Section** - Eye-catching introduction
2. **Statistics** - Key numbers that impress
3. **About Us** - Mission, vision, methodology
4. **Courses** - All 7 courses with details
5. **Features** - Why choose your institute
6. **Contact** - Form + contact info + map
7. **Footer** - Links and social media

## 🎯 Current Courses Listed

1. 📐 Mathematics
2. 🔬 Science
3. 📚 English
4. ಕ Kannada
5. ह Hindi
6. संस् Sanskrit
7. 🇫🇷 French

## 💡 Pro Tips

### Adding More Courses
Copy this template in `config/courses.json`:
```json
{
  "id": 8,
  "title": "New Course",
  "description": "Course description here",
  "icon": "📖",
  "color": "#3B82F6",
  "highlights": [
    "Feature 1",
    "Feature 2",
    "Feature 3"
  ]
}
```

### Changing the Color Scheme
All colors are in `css/style.css` under `:root`:
- Blue theme: Already applied ✅
- Green theme: Change to #10b981
- Purple theme: Change to #8b5cf6
- Orange theme: Change to #f59e0b

### Getting Google Maps Embed
1. Go to Google Maps
2. Search your location
3. Click Share → Embed a map
4. Copy the iframe src URL
5. Paste in courses.json → contact.mapEmbedUrl

## 🔧 Testing Checklist

Before going live, test:
- [ ] All navigation links work
- [ ] Mobile menu opens/closes
- [ ] Contact form validates input
- [ ] All content displays correctly
- [ ] Website works on mobile
- [ ] Map loads properly
- [ ] All courses appear
- [ ] Statistics animate on scroll

## 📞 Need Help?

Check the full README.md for:
- Detailed customization guide
- Troubleshooting tips
- Technical documentation
- Backend integration guide

## 🎉 You're All Set!

Your modern education website is ready to go. Just customize the content in `courses.json` and you're done!

---
**Happy Teaching! 🎓**
