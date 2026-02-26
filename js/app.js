/**
 * LIB Education - Main JavaScript Application
 * Handles dynamic content loading, form validation, animations, and interactions
 */

// ============================================
// Global State and Configuration
// ============================================
let siteData = null;

// ============================================
// Utility Functions
// ============================================

/**
 * Fetch JSON data from config file
 */
async function loadSiteData() {
    try {
        const response = await fetch('config/courses.json');
        if (!response.ok) {
            throw new Error('Failed to load site data');
        }
        siteData = await response.json();
        return siteData;
    } catch (error) {
        console.error('Error loading site data:', error);
        // Return default data if JSON fails to load
        return getDefaultData();
    }
}

/**
 * Default data fallback if JSON fails to load
 */
function getDefaultData() {
    return {
        institute: {
            name: "LIB Education",
            tagline: "Empowering Minds, Shaping Futures",
            description: "Premier coaching institute dedicated to quality education",
            mission: "To provide world-class education",
            vision: "To be the most trusted educational institution",
            methodology: "Combining traditional wisdom with modern technology"
        },
        contact: {
            phone: "+91 98765 43210",
            email: "info@libeducation.com",
            address: "Bangalore, Karnataka, India"
        },
        courses: [],
        features: [],
        statistics: []
    };
}

/**
 * Scroll reveal animation observer
 */
function initScrollReveal() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in-up');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Observe all cards and sections
    document.querySelectorAll('.card, .feature-card, .section-header').forEach(el => {
        observer.observe(el);
    });
}

/**
 * Smooth scroll to section
 */
function smoothScrollTo(target) {
    const element = document.querySelector(target);
    if (element) {
        const headerHeight = document.querySelector('.header').offsetHeight;
        const elementPosition = element.getBoundingClientRect().top + window.pageYOffset;
        const offsetPosition = elementPosition - headerHeight;

        window.scrollTo({
            top: offsetPosition,
            behavior: 'smooth'
        });
    }
}

// ============================================
// Header & Navigation
// ============================================

/**
 * Initialize header scroll effect and navigation
 */
function initHeader() {
    const header = document.getElementById('header');
    const menuToggle = document.getElementById('menuToggle');
    const navLinks = document.getElementById('navLinks');
    const navLinkElements = document.querySelectorAll('.nav-link');

    // Header scroll effect
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            header.classList.add('scrolled');
        } else {
            header.classList.remove('scrolled');
        }

        // Update active nav link based on scroll position
        updateActiveNavLink();
    });

    // Mobile menu toggle
    menuToggle.addEventListener('click', () => {
        menuToggle.classList.toggle('active');
        navLinks.classList.toggle('active');
        document.body.style.overflow = navLinks.classList.contains('active') ? 'hidden' : '';
    });

    // Smooth scroll on nav link click
    navLinkElements.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const target = link.getAttribute('href');
            smoothScrollTo(target);

            // Close mobile menu if open
            menuToggle.classList.remove('active');
            navLinks.classList.remove('active');
            document.body.style.overflow = '';

            // Update active state
            navLinkElements.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
        });
    });
}

/**
 * Update active navigation link based on scroll position
 */
function updateActiveNavLink() {
    const sections = document.querySelectorAll('.section');
    const navLinks = document.querySelectorAll('.nav-link');
    const headerHeight = document.querySelector('.header').offsetHeight;

    let currentSection = '';

    sections.forEach(section => {
        const sectionTop = section.offsetTop - headerHeight - 100;
        const sectionHeight = section.offsetHeight;
        const scrollPosition = window.pageYOffset;

        if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
            currentSection = section.getAttribute('id');
        }
    });

    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === `#${currentSection}`) {
            link.classList.add('active');
        }
    });
}

// ============================================
// Dynamic Content Loading
// ============================================

/**
 * Populate institute information
 */
function populateInstituteInfo(data) {
    const { institute } = data;

    // Update text content
    document.getElementById('institute-name').textContent = institute.name;
    document.getElementById('hero-title').textContent = `Welcome to ${institute.name}`;
    document.getElementById('hero-tagline').textContent = institute.tagline;
    document.getElementById('hero-description').textContent = institute.description;
    document.getElementById('about-description').textContent = institute.description;
    document.getElementById('mission-text').textContent = institute.mission;
    document.getElementById('vision-text').textContent = institute.vision;
    document.getElementById('methodology-text').textContent = institute.methodology;
    document.getElementById('footer-institute-name').textContent = institute.name;
}

/**
 * Populate statistics section
 */
function populateStatistics(data) {
    const statsGrid = document.getElementById('stats-grid');
    if (!data.statistics || data.statistics.length === 0) return;

    statsGrid.innerHTML = data.statistics.map(stat => `
        <div class="stat-item">
            <div class="stat-number">${stat.number}</div>
            <div class="stat-label">${stat.label}</div>
        </div>
    `).join('');

    // Animate numbers on scroll
    animateStatNumbers();
}

/**
 * Animate stat numbers when they come into view
 */
function animateStatNumbers() {
    const statNumbers = document.querySelectorAll('.stat-number');
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const target = entry.target;
                const text = target.textContent;
                const hasPercent = text.includes('%');
                const hasPlus = text.includes('+');
                const number = parseInt(text.replace(/\D/g, ''));
                
                let current = 0;
                const increment = number / 50;
                const timer = setInterval(() => {
                    current += increment;
                    if (current >= number) {
                        current = number;
                        clearInterval(timer);
                    }
                    target.textContent = Math.floor(current) + (hasPercent ? '%' : '') + (hasPlus ? '+' : '');
                }, 30);
                
                observer.unobserve(target);
            }
        });
    }, { threshold: 0.5 });

    statNumbers.forEach(num => observer.observe(num));
}

/**
 * Populate courses section
 */
function populateCourses(data) {
    const coursesGrid = document.getElementById('courses-grid');
    const footerCourses = document.getElementById('footer-courses');
    
    if (!data.courses || data.courses.length === 0) return;

    // Main courses grid
    coursesGrid.innerHTML = data.courses.map(course => `
        <div class="card">
            <div class="card-icon" style="color: ${course.color || 'var(--primary-600)'}">${course.icon}</div>
            <h3 class="card-title">${course.title}</h3>
            <p class="card-description">${course.description}</p>
            ${course.highlights ? `
                <ul class="card-highlights">
                    ${course.highlights.map(h => `<li>${h}</li>`).join('')}
                </ul>
            ` : ''}
            <button class="btn btn-primary" onclick="openCourseModal('${course.title}')">Learn More</button>
        </div>
    `).join('');

    // Footer courses (first 5)
    footerCourses.innerHTML = data.courses.slice(0, 5).map(course => `
        <li><a href="#courses">${course.icon} ${course.title}</a></li>
    `).join('');
}

/**
 * Populate features section
 */
function populateFeatures(data) {
    const featuresGrid = document.getElementById('features-grid');
    
    if (!data.features || data.features.length === 0) return;

    featuresGrid.innerHTML = data.features.map(feature => `
        <div class="feature-card">
            <div class="feature-icon">${feature.icon}</div>
            <h3 class="feature-title">${feature.title}</h3>
            <p class="feature-description">${feature.description}</p>
        </div>
    `).join('');
}

/**
 * Populate contact information
 */
function populateContact(data) {
    const { contact } = data;

    document.getElementById('contact-phone').textContent = contact.phone;
    document.getElementById('contact-email').textContent = contact.email;
    document.getElementById('contact-address').textContent = contact.address;
    document.getElementById('footer-phone').textContent = `📞 ${contact.phone}`;
    document.getElementById('footer-email').textContent = `📧 ${contact.email}`;
    
    // Load Google Map
    if (contact.mapEmbedUrl) {
        document.getElementById('map-container').innerHTML = `
            <iframe 
                src="${contact.mapEmbedUrl}" 
                allowfullscreen="" 
                loading="lazy" 
                referrerpolicy="no-referrer-when-downgrade">
            </iframe>
        `;
    }
}

// ============================================
// Form Handling
// ============================================

/**
 * Initialize contact form
 */
function initContactForm() {
    const form = document.getElementById('contactForm');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // Get form data
        const formData = {
            name: document.getElementById('name').value,
            email: document.getElementById('email').value,
            phone: document.getElementById('phone').value,
            message: document.getElementById('message').value,
            timestamp: new Date().toISOString()
        };

        // Validate form
        if (!validateForm(formData)) {
            return;
        }

        // Show loading state
        const submitButton = form.querySelector('button[type="submit"]');
        const originalText = submitButton.textContent;
        submitButton.textContent = 'Sending...';
        submitButton.disabled = true;

        // Simulate form submission (replace with actual API call)
        try {
            await simulateFormSubmission(formData);
            
            // Show success message
            showNotification('Thank you! Your message has been sent successfully. We will get back to you soon.', 'success');
            
            // Reset form
            form.reset();
        } catch (error) {
            showNotification('Sorry, there was an error sending your message. Please try again.', 'error');
        } finally {
            submitButton.textContent = originalText;
            submitButton.disabled = false;
        }
    });

    // Real-time validation
    const inputs = form.querySelectorAll('input, textarea');
    inputs.forEach(input => {
        input.addEventListener('blur', () => {
            validateField(input);
        });
    });
}

/**
 * Validate individual form field
 */
function validateField(field) {
    const value = field.value.trim();
    let isValid = true;
    let errorMessage = '';

    if (field.required && !value) {
        isValid = false;
        errorMessage = 'This field is required';
    } else if (field.type === 'email' && value) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
            isValid = false;
            errorMessage = 'Please enter a valid email address';
        }
    } else if (field.type === 'tel' && value) {
        const phoneRegex = /^[\d\s\+\-\(\)]+$/;
        if (!phoneRegex.test(value)) {
            isValid = false;
            errorMessage = 'Please enter a valid phone number';
        }
    }

    // Remove previous error
    const existingError = field.parentElement.querySelector('.error-message');
    if (existingError) {
        existingError.remove();
    }

    // Show error if invalid
    if (!isValid) {
        field.style.borderColor = 'var(--accent-danger)';
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.style.color = 'var(--accent-danger)';
        errorDiv.style.fontSize = 'var(--text-sm)';
        errorDiv.style.marginTop = 'var(--space-2)';
        errorDiv.textContent = errorMessage;
        field.parentElement.appendChild(errorDiv);
    } else {
        field.style.borderColor = '';
    }

    return isValid;
}

/**
 * Validate entire form
 */
function validateForm(formData) {
    let isValid = true;

    // Validate name
    if (!formData.name || formData.name.length < 2) {
        isValid = false;
        showNotification('Please enter a valid name', 'error');
    }

    // Validate email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email || !emailRegex.test(formData.email)) {
        isValid = false;
        showNotification('Please enter a valid email address', 'error');
    }

    // Validate message
    if (!formData.message || formData.message.length < 10) {
        isValid = false;
        showNotification('Please enter a message (at least 10 characters)', 'error');
    }

    return isValid;
}

/**
 * Simulate form submission (replace with actual backend API)
 */
function simulateFormSubmission(data) {
    return new Promise((resolve) => {
        // Log to console for demonstration
        console.log('Form submitted:', data);
        
        // Simulate network delay
        setTimeout(() => {
            resolve({ success: true });
        }, 1500);
    });
}

// ============================================
// Notifications & Modals
// ============================================

/**
 * Show notification message
 */
function showNotification(message, type = 'info') {
    // Remove existing notification
    const existing = document.querySelector('.notification');
    if (existing) {
        existing.remove();
    }

    // Create notification
    const notification = document.createElement('div');
    notification.className = 'notification';
    notification.style.cssText = `
        position: fixed;
        top: 100px;
        right: 20px;
        background: ${type === 'success' ? 'var(--accent-primary)' : 'var(--accent-danger)'};
        color: white;
        padding: var(--space-4) var(--space-6);
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-xl);
        z-index: 10000;
        animation: slideInRight 0.3s ease-out;
        max-width: 400px;
    `;
    notification.textContent = message;

    document.body.appendChild(notification);

    // Auto remove after 5 seconds
    setTimeout(() => {
        notification.style.animation = 'slideInRight 0.3s ease-out reverse';
        setTimeout(() => notification.remove(), 300);
    }, 5000);
}

/**
 * Open course details modal
 */
function openCourseModal(courseTitle) {
    const course = siteData.courses.find(c => c.title === courseTitle);
    if (!course) return;

    showNotification(`Learn more about ${course.title}. Contact us to enroll!`, 'info');
    smoothScrollTo('#contact');
}

// ============================================
// Initialization
// ============================================

/**
 * Initialize the application
 */
async function init() {
    // Set current year in footer
    document.getElementById('current-year').textContent = new Date().getFullYear();

    // Load site data from JSON
    const data = await loadSiteData();

    // Populate all sections with data
    populateInstituteInfo(data);
    populateStatistics(data);
    populateCourses(data);
    populateFeatures(data);
    populateContact(data);

    // Initialize interactive features
    initHeader();
    initContactForm();
    initScrollReveal();

    // Add smooth scroll to all anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = this.getAttribute('href');
            if (target && target !== '#') {
                smoothScrollTo(target);
            }
        });
    });

    // Add animation delay to cards
    document.querySelectorAll('.card, .feature-card').forEach((card, index) => {
        card.style.animationDelay = `${index * 0.1}s`;
    });

    console.log('LIB Education website initialized successfully! 🎓');
}

// ============================================
// Start Application
// ============================================

// Wait for DOM to be fully loaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}

// Export functions for potential external use
window.LIBEducation = {
    openCourseModal,
    showNotification,
    smoothScrollTo
};
