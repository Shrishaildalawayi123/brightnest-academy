# 🚀 Quick Build & Run Guide - Shrishail Academy

This guide will help you set up and run the complete full-stack application in minutes.

---

## ⚡ Quick Start (5 Minutes)

### Step 1: Install Prerequisites (Skip if already installed)

1. **Java JDK 17+**: [Download](https://adoptium.net/)
2. **Maven**: [Download](https://maven.apache.org/download.cgi)
3. **MySQL**: [Download](https://dev.mysql.com/downloads/mysql/)

Verify installations:
```bash
java -version    # Should show Java 17+
mvn -version     # Should show Maven 3.6+
mysql --version  # Should show MySQL 8.0+
```

---

### Step 2: Setup Database (2 minutes)

**Method A: Command Line**
```bash
# Start MySQL
mysql -u root -p

# Create database and import schema
source "d:\Tuition class website\shrishail-academy\database\schema.sql"

# Verify
SHOW TABLES;
SELECT * FROM users;
```

**Method B: MySQL Workbench**
1. Open MySQL Workbench
2. Connect to your local MySQL server
3. File → Run SQL Script → Select `database/schema.sql`
4. Execute

---

### Step 3: Configure Database Connection (30 seconds)

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shrishail_academy
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE  <-- Change this!
```

---

### Step 4: Build Backend (1 minute)

```bash
cd "d:\Tuition class website\shrishail-academy"

# Build the project
mvn clean install

# If build fails, try:
mvn clean install -DskipTests
```

---

### Step 5: Start Backend Server (10 seconds)

```bash
mvn spring-boot:run
```

You'll see:
```
========================================
🎓 Shrishail Academy API Started!
========================================
API Base URL: http://localhost:8080/api
```

**Keep this terminal open!**

---

### Step 6: Open Frontend (5 seconds)

**Option A: Double-click**
- Navigate to `frontend/` folder
- Double-click `index.html`

**Option B: Local Server (Recommended)**
```bash
# Open new terminal

# Using Python
cd "d:\Tuition class website\shrishail-academy\frontend"
python -m http.server 3000

# Using Node.js
npx serve frontend -p 3000
```

Then open: http://localhost:3000

---

## ✅ Verify Installation

### Test Backend API
Open browser: http://localhost:8080/api/courses

You should see JSON response with 7 courses.

### Test Login
1. Open: http://localhost:3000/login.html (or file index)
2. Use credentials:
   - **Admin**: admin@academy.com / admin123
   - **Student**: student@test.com / student123
3. Click Login
4. You should see dashboard

---

## 🎯 Complete File Checklist

Before building, make sure you have these files:

### Backend Files
```
✅ pom.xml
✅ src/main/resources/application.properties
✅ src/main/java/com/shrishailacademy/ShrishailAcademyApplication.java
✅ database/schema.sql
```

### Source Code Files (Copy from SOURCE_CODE_PART1.md and PART2.md)
```
✅ config/SecurityConfig.java
✅ config/WebConfig.java
✅ security/JwtTokenProvider.java
✅ security/JwtAuthenticationFilter.java
✅ security/UserDetailsServiceImpl.java
✅ model/User.java
✅ model/Course.java
✅ model/Enrollment.java
✅ repository/UserRepository.java
✅ repository/CourseRepository.java
✅ repository/EnrollmentRepository.java
✅ dto/RegisterRequest.java
✅ dto/LoginRequest.java
✅ dto/AuthResponse.java
✅ dto/ApiResponse.java
✅ service/AuthService.java
✅ service/CourseService.java
✅ service/EnrollmentService.java
✅ service/UserService.java
✅ controller/AuthController.java
✅ controller/CourseController.java
✅ controller/EnrollmentController.java
✅ controller/UserController.java
```

###  Frontend Files
```
✅ frontend/index.html
✅ frontend/css/style.css (copy from LIB Education project)
✅ frontend/js/app.js (copy from LIB Education project)
✅ frontend/js/api.js
✅ frontend/js/auth.js
```

### Frontend Pages (Copy from FRONTEND_CODE_PART1.md & PART2.md when created)
```
✅ frontend/login.html
✅ frontend/register.html
✅ frontend/courses.html
✅ frontend/about.html
✅ frontend/contact.html
✅ frontend/student-dashboard.html
✅ frontend/admin-dashboard.html
```

---

## 📋 Step-by-Step File Creation

### If you're missing source code files:

1. **Open SOURCE_CODE_PART1.md**
2. **Copy each Java class** to the specified path
3. **Do the same with SOURCE_CODE_PART2.md**
4. **Copy frontend files from FRONTEND_CODE_PART1.md**

Example:
```
From SOURCE_CODE_PART1.md, copy:

## 1. Security Config (SecurityConfig.java)
Path: src/main/java/com/shrishailacademy/config/SecurityConfig.java

→ Create folder: src/main/java/com/shrishailacademy/config/
→ Create file: SecurityConfig.java
→ Paste the code from the document
```

---

## 🐛 Common Issues & Solutions

### Issue 1: "Port 8080 already in use"
**Solution**: Change port in `application.properties`:
```properties
server.port=8081
```

### Issue 2: "Cannot connect to database"
**Solution**: 
- Check MySQL is running: `mysql.server start`
- Verify credentials in `application.properties`
- Check database exists: `SHOW DATABASES;`

### Issue 3: "Build failed - dependencies cannot be resolved"
**Solution**:
```bash
mvn clean
mvn install -U
```

### Issue 4: "JWT token errors"
**Solution**: Clear localStorage and login again:
```javascript
// In browser console:
localStorage.clear();
```

### Issue 5: "CORS errors in frontend"
**Solution**: Use local server instead of opening HTML directly
```bash
python -m http.server 3000
```

---

## 🔄 Restart Instructions

### Stop Backend
- Press `Ctrl+C` in terminal running Spring Boot

### Restart Backend
```bash
mvn spring-boot:run
```

### Clear Database and Reset
```bash
mysql -u root -p

DROP DATABASE shrishail_academy;
source database/schema.sql;
```

---

## 📱 Test the Application

### Test Flow 1: Public User
1. Visit homepage: `index.html`
2. Click "View Courses"
3. Browse all 7 courses
4. Click "Register"
5. Create student account
6. Login and see student dashboard

### Test Flow 2: Student
1. Login as: student@test.com / student123
2. View enrolled courses
3. Enroll in a new course
4. View updated course list

### Test Flow 3: Admin
1. Login as: admin@academy.com / admin123
2. View all students
3. Add a new course
4. Edit existing course
5. View enrollment statistics
6. Delete a course

---

## 📊 Project Status Check

Run these commands to verify everything is working:

### Backend Health Check
```bash
curl http://localhost:8080/api/courses
```

### Database Check
```sql
USE shrishail_academy;
SELECT COUNT(*) FROM users;      -- Should show 2 (admin + test student)
SELECT COUNT(*) FROM courses;    -- Should show 7
SELECT COUNT(*) FROM enrollments; -- Should show 1
```

---

## 🎉 Success Checklist

- [ ] MySQL database created with schema
- [ ] Backend builds successfully (`mvn clean install`)
- [ ] Backend starts on port 8080
- [ ] Can access http://localhost:8080/api/courses
- [ ] Frontend opens in browser
- [ ] Can login with admin credentials
- [ ] Can login with student credentials
- [ ] Admin can add/edit/delete courses
- [ ] Student can enroll in courses
- [ ] Contact form works
- [ ] All pages load without errors

---

## 🚀 Next Steps

Once everything is working:

1. **Customize Content**: Edit course descriptions, contact info
2. **Add Features**: Implement additional functionality
3. **Deploy**: Follow deployment guide in README.md
4. **Secure**: Change default passwords
5. **Test**: Thoroughly test all features

---

## 💡 Pro Tips

1. **Use Two Terminals**: One for backend, one for frontend server
2. **Check Console**: Browser console shows frontend errors
3. **Check Logs**: Backend logs show API errors
4. **Test APIs First**: Use Postman to test backend before frontend
5. **Git Commit Often**: Save your progress frequently

---

## 📞 Need Help?

If you encounter issues:

1. Check this guide's "Common Issues" section
2. Check browser console for errors
3. Check backend logs in terminal
4. Verify database has data: `SELECT * FROM users;`
5. Ensure all files are in correct locations

---

**You're all set! 🎓 Happy coding!**
