# 🎨 Frontend Code - All HTML Pages

This file contains all the HTML pages for the Shrishail Academy frontend.
Copy each section to create the corresponding HTML file.

---

## 1. Login Page (login.html)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Shrishail Academy</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Poppins:wght@400;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <header class="header">
        <nav class="nav container">
            <a href="index.html" class="logo">
                <span class="logo-icon">🎓</span>
                <span>Shrishail Academy</span>
            </a>
            <a href="index.html" class="btn btn-secondary">← Back to Home</a>
        </nav>
    </header>

    <section class="section" style="padding-top: calc(var(--header-height) + 3rem);">
        <div class="container">
            <div style="max-width: 500px; margin: 0 auto;">
                <div class="card">
                    <div class="text-center" style="margin-bottom: 2rem;">
                        <h2>Welcome Back</h2>
                        <p style="color: var(--neutral-500)">Login to your account</p>
                    </div>

                    <form id="loginForm">
                        <div class="form-group">
                            <label class="form-label">Email Address *</label>
                            <input type="email" id="email" class="form-input" required placeholder="your@email.com">
                        </div>

                        <div class="form-group">
                            <label class="form-label">Password *</label>
                            <input type="password" id="password" class="form-input" required placeholder="Enter your password">
                        </div>

                        <button type="submit" class="btn btn-primary btn-lg" style="width: 100%;">
                            Login
                        </button>
                    </form>

                    <div class="text-center" style="margin-top: 1.5rem;">
                        <p style="color: var(--neutral-600)">
                            Don't have an account? 
                            <a href="register.html" style="color: var(--primary-600); font-weight: 600;">Register here</a>
                        </p>
                    </div>

                    <div class="text-center" style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--neutral-200);">
                        <p style="font-size: 0.875rem; color: var(--neutral-500);">
                            <strong>Demo Credentials:</strong><br>
                            Admin: admin@academy.com / admin123<br>
                            Student: student@test.com / student123
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <script src="js/api.js"></script>
    <script src="js/auth.js"></script>
    <script>
        // Redirect if already logged in
        if (Auth.isLoggedIn()) {
            Auth.redirectToDashboard();
        }

        document.getElementById('loginForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const button = e.target.querySelector('button');
            
            button.disabled = true;
            button.textContent = 'Logging in...';
            
            try {
                await Auth.login(email, password);
                alert('Login successful!');
                Auth.redirectToDashboard();
            } catch (error) {
                alert('Login failed: ' + error.message);
                button.disabled = false;
                button.textContent = 'Login';
            }
        });
    </script>
</body>
</html>
```

---

## 2. Register Page (register.html)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register - Shrishail Academy</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Poppins:wght@400;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <header class="header">
        <nav class="nav container">
            <a href="index.html" class="logo">
                <span class="logo-icon">🎓</span>
                <span>Shrishail Academy</span>
            </a>
            <a href="index.html" class="btn btn-secondary">← Back to Home</a>
        </nav>
    </header>

    <section class="section" style="padding-top: calc(var(--header-height) + 3rem);">
        <div class="container">
            <div style="max-width: 500px; margin: 0 auto;">
                <div class="card">
                    <div class="text-center" style="margin-bottom: 2rem;">
                        <h2>Create Account</h2>
                        <p style="color: var(--neutral-500)">Register as a student</p>
                    </div>

                    <form id="registerForm">
                        <div class="form-group">
                            <label class="form-label">Full Name *</label>
                            <input type="text" id="name" class="form-input" required placeholder="Enter your full name">
                        </div>

                        <div class="form-group">
                            <label class="form-label">Email Address *</label>
                            <input type="email" id="email" class="form-input" required placeholder="your@email.com">
                        </div>

                        <div class="form-group">
                            <label class="form-label">Phone Number</label>
                            <input type="tel" id="phone" class="form-input" placeholder="+91 98765 43210">
                        </div>

                        <div class="form-group">
                            <label class="form-label">Password *</label>
                            <input type="password" id="password" class="form-input" required placeholder="At least 6 characters">
                        </div>

                        <div class="form-group">
                            <label class="form-label">Confirm Password *</label>
                            <input type="password" id="confirmPassword" class="form-input" required placeholder="Re-enter password">
                        </div>

                        <button type="submit" class="btn btn-primary btn-lg" style="width: 100%;">
                            Register
                        </button>
                    </form>

                    <div class="text-center" style="margin-top: 1.5rem;">
                        <p style="color: var(--neutral-600)">
                            Already have an account? 
                            <a href="login.html" style="color: var(--primary-600); font-weight: 600;">Login here</a>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <script src="js/api.js"></script>
    <script src="js/auth.js"></script>
    <script>
        // Redirect if already logged in
        if (Auth.isLoggedIn()) {
            Auth.redirectToDashboard();
        }

        document.getElementById('registerForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            
            if (password !== confirmPassword) {
                alert('Passwords do not match!');
                return;
            }
            
            if (password.length < 6) {
                alert('Password must be at least 6 characters!');
                return;
            }
            
            const userData = {
                name: document.getElementById('name').value,
                email: document.getElementById('email').value,
                phone: document.getElementById('phone').value,
                password: password
            };
            
            const button = e.target.querySelector('button');
            button.disabled = true;
            button.textContent = 'Creating account...';
            
            try {
                await Auth.register(userData);
                alert('Registration successful! Redirecting to dashboard...');
                Auth.redirectToDashboard();
            } catch (error) {
                alert('Registration failed: ' + error.message);
                button.disabled = false;
                button.textContent = 'Register';
            }
        });
    </script>
</body>
</html>
```

---

Continue in FRONTEND_CODE_PART2.md for remaining pages...
