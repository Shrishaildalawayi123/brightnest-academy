# 🎓 Shrishail Academy - Full-Stack Education Management System

A complete education institute management web application built with **Spring Boot** backend and **HTML/CSS/JavaScript** frontend.

## 📋 Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Default Credentials](#default-credentials)
- [Deployment](#deployment)

---

## ✨ Features

### Public Website
- ✅ Home page with hero section
- ✅ About page (mission, vision, methodology)
- ✅ Courses page (7 courses: Math, Science, English, Kannada, Hindi, Sanskrit, French)
- ✅ Contact page with form

### Authentication System
- ✅ Student registration
- ✅ Student/Admin login
- ✅ JWT-based authentication
- ✅ BCrypt password encryption
- ✅ Role-based access control (ADMIN/STUDENT)

### Student Dashboard
- ✅ View enrolled courses
- ✅ Enroll in new courses
- ✅ View profile
- ✅ Logout functionality

### Admin Dashboard
- ✅ Add new courses
- ✅ Edit existing courses
- ✅ Delete courses
- ✅ View all students
- ✅ View all enrollments
- ✅ Enrollment statistics

---

## 🛠️ Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.2**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA**
- **MySQL Database**
- **Maven** (Build Tool)
- **Lombok** (Reduce Boilerplate)

### Frontend
- **HTML5**
- **CSS3** (Modern Design System)
- **JavaScript (ES6+)**
- **Responsive Design**
- **Blue & White Education Theme**

---

## 📁 Project Structure

```
shrishail-academy/
├── backend/
│   ├── src/main/java/com/shrishailacademy/
│   │   ├── ShrishailAcademyApplication.java    # Main Application
│   │   ├── config/
│   │   │   ├── SecurityConfig.java             # Security Configuration
│   │   │   └── WebConfig.java                  # CORS Configuration
│   │   ├── controller/
│   │   │   ├── AuthController.java             # Authentication APIs
│   │   │   ├── CourseController.java           # Course CRUD APIs
│   │   │   ├── EnrollmentController.java       # Enrollment APIs
│   │   │   └── UserController.java             # User Management APIs
│   │   ├── dto/
│   │   │   ├── RegisterRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   └── ApiResponse.java
│   │   ├── model/
│   │   │   ├── User.java                       # User Entity
│   │   │   ├── Course.java                     # Course Entity
│   │   │   └── Enrollment.java                 # Enrollment Entity
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── CourseRepository.java
│   │   │   └── EnrollmentRepository.java
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java           # JWT Utility
│   │   │   ├── JwtAuthenticationFilter.java    # JWT Filter
│   │   │   └── UserDetailsServiceImpl.java     # User Details Service
│   │   └── service/
│   │       ├── AuthService.java
│   │       ├── CourseService.java
│   │       ├── EnrollmentService.java
│   │       └── UserService.java
│   └── src/main/resources/
│       └── application.properties              # Configuration
├── database/
│   └── schema.sql                              # MySQL Schema
├── frontend/
│   ├── index.html                              # Home Page
│   ├── about.html                              # About Page
│   ├── courses.html                            # Courses Page
│   ├── contact.html                            # Contact Page
│   ├── login.html                              # Login Page
│   ├── register.html                           # Registration Page
│   ├── student-dashboard.html                  # Student Dashboard
│   ├── admin-dashboard.html                    # Admin Dashboard
│   ├── css/
│   │   └── style.css                           # Main Stylesheet
│   └── js/
│       ├── app.js                              # Main JavaScript
│       ├── auth.js                             # Authentication Logic
│       └── api.js                              # API Service
└── pom.xml                                     # Maven Dependencies
```

---

## ⚙️ Prerequisites

Before running the application, ensure you have:

1. **Java JDK 17 or higher**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **MySQL 8.0+**
   ```bash
   mysql --version
   ```

4. **Git** (Optional, for cloning)

---

## 📥 Installation & Setup

### Step 1: Clone/Download the Project

```bash
cd "d:\Tuition class website\shrishail-academy"
```

### Step 2: Setup MySQL Database

1. **Start MySQL Server**

2. **Create Database & Tables**
   ```bash
   mysql -u root -p < database/schema.sql
   ```

   Or manually:
   ```sql
   mysql -u root -p
   CREATE DATABASE shrishail_academy;
   USE shrishail_academy;
   source database/schema.sql;
   ```

3. **Verify Database**
   ```sql
   SHOW TABLES;
   SELECT * FROM users;
   SELECT * FROM courses;
   ```

### Step 3: Configure Database Connection

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shrishail_academy
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 4: Build the Project

```bash
# Navigate to project root
cd shrishail-academy

# Clean and build
mvn clean install

# Skip tests (if needed)
mvn clean install -DskipTests
```

---

## 🚀 Running the Application

### Method 1: Using Maven

```bash
mvn spring-boot:run
```

### Method 2: Using Java JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/shrishail-academy-1.0.0.jar
```

### Method 3: Using IDE (IntelliJ IDEA / Eclipse)

1. Import project as Maven project
2. Run `ShrishailAcademyApplication.java`

---

## 🌐 Access the Application

### Backend API
```
http://localhost:8080/api
```

### Frontend (Open in Browser)
```
file:///d:/Tuition%20class%20website/shrishail-academy/frontend/index.html
```

Or use a local server:
```bash
# Using Python
cd frontend
python -m http.server 3000

# Using Node.js
npx serve frontend -p 3000

# Then access: http://localhost:3000
```

---

## 🔐 Default Credentials

### Admin Login
```
Email: admin@academy.com
Password: admin123
```

### Test Student Login
```
Email: student@test.com
Password: student123
```

---

## 📡 API Documentation

### Authentication APIs

#### 1. Register Student
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "phone": "+91 9876543210"
}
```

#### 2. Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@academy.com",
  "password": "admin123"
}

Response:
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "name": "Admin",
  "email": "admin@academy.com",
  "role": "ADMIN"
}
```

### Course APIs

#### 3. Get All Courses (Public)
```http
GET /api/courses
```

#### 4. Get Course by ID
```http
GET /api/courses/{id}
```

#### 5. Create Course (Admin Only)
```http
POST /api/courses
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Mathematics",
  "description": "Advanced mathematics course",
  "duration": "12 months",
  "icon": "📐",
  "color": "#3B82F6"
}
```

#### 6. Update Course (Admin Only)
```http
PUT /api/courses/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Updated Course Title",
  "description": "Updated description"
}
```

#### 7. Delete Course (Admin Only)
```http
DELETE /api/courses/{id}
Authorization: Bearer {token}
```

### Enrollment APIs

#### 8. Enroll in Course (Student)
```http
POST /api/enrollments/{courseId}
Authorization: Bearer {token}
```

#### 9. Get My Enrollments (Student)
```http
GET /api/enrollments/my-courses
Authorization: Bearer {token}
```

#### 10. Get All Enrollments (Admin)
```http
GET /api/enrollments
Authorization: Bearer {token}
```

### User APIs

#### 11. Get All Students (Admin)
```http
GET /api/users/students
Authorization: Bearer {token}
```

#### 12. Get Current User Profile
```http
GET /api/users/me
Authorization: Bearer {token}
```

---

## 📦 Deployment

### Deploy to Railway

1. **Install Railway CLI**
   ```bash
   npm install -g @railway/cli
   ```

2. **Login to Railway**
   ```bash
   railway login
   ```

3. **Initialize Project**
   ```bash
   railway init
   ```

4. **Add MySQL Database**
   - Go to Railway Dashboard
   - Add MySQL plugin
   - Copy database credentials

5. **Update application.properties**
   ```properties
   spring.datasource.url=${DATABASE_URL}
   ```

6. **Deploy**
   ```bash
   railway up
   ```

### Deploy to Render

1. **Create account on Render.com**

2. **Create Web Service**
   - Choose "Build and deploy from a Git repository"
   - Connect your GitHub repo

3. **Configuration**
   ```
   Build Command: mvn clean package
   Start Command: java -jar target/shrishail-academy-1.0.0.jar
   ```

4. **Add MySQL Database**
   - Create PostgreSQL or MySQL database on Render
   - Add connection string to environment variables

5. **Deploy**

### Deploy to AWS / DigitalOcean VPS

1. **Build JAR**
   ```bash
   mvn clean package
   ```

2. **Upload to Server**
   ```bash
   scp target/shrishail-academy-1.0.0.jar user@server:/opt/app/
   ```

3. **Run as Service**
   ```bash
   sudo systemctl start shrishail-academy
   ```

---

## 🧪 Testing

### Test Backend APIs with cURL

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@test.com","password":"test123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@academy.com","password":"admin123"}'

# Get Courses
curl http://localhost:8080/api/courses
```

---

## 🐛 Troubleshooting

### MySQL Connection Error
```
Error: Could not create connection to database server
```
**Solution**: Check MySQL is running and credentials are correct in `application.properties`

### Port Already in Use
```
Error: Port 8080 is already in use
```
**Solution**: Change port in `application.properties`:
```properties
server.port=8081
```

### Build Fails
```
Error: Failed to execute goal
```
**Solution**: 
```bash
mvn clean
mvn install -U
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [JWT Introduction](https://jwt.io/introduction)
- [MySQL Documentation](https://dev.mysql.com/doc/)

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

## 📄 License

This project is open source and available for educational purposes.

---

## 💬 Support

For issues or questions:
- Email: admin@shrishailacademy.com
- GitHub Issues: [Create an issue](https://github.com/your-repo/issues)

---

**Built with ❤️ for Shrishail Academy**

**Learning for a Better Future** 🎓
