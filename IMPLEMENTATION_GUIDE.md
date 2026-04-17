# RPSS Event Management System - Complete Implementation Guide

## 📋 Project Overview

This is a comprehensive **Role-Based Event Management System** for RPSS Committee with three main roles:
- **Student**: Register for events, give feedback, download passes
- **Club Head**: Create events, manage students, upload media
- **Admin**: Approve/reject events, manage users/clubs, generate reports

---

## 🏗 System Architecture

### Technology Stack
```
Framework:       Spring Boot 4.0.5
Language:        Java 21
ORM:             JPA/Hibernate
Database:        MySQL 8.0+
Template Engine: Thymeleaf
Build Tool:      Maven
Security:        Spring Security (Session-based)
```

### Module Breakdown

```
1. Authentication    → User Login/Registration
2. Event Mgmt        → Event Lifecycle (PENDING → APPROVED → COMPLETED)
3. Registration      → Event Registration & Attendance
4. Pass Generation   → QR Code passes for entry
5. Feedback          → Rating & Comments (1-5)
6. Media Upload      → Photos & Videos
7. Reporting         → Analytics & Reports
8. Admin Panel       → User/Club/Event Management
```

---

## 📊 Database Schema

### Entity Relationships

```
USER (1:1) ← ROLE
EVENT (N:1) → CLUB
EVENT (N:1) → USER (creator, approver)
REGISTRATION (N:1) → EVENT  
REGISTRATION (N:1) → USER (student)
REGISTRATION (1:1) → PASS
FEEDBACK (N:1) → EVENT
FEEDBACK (N:1) → USER 
MEDIA (N:1) → EVENT
MEDIA (N:1) → USER (uploader)
REPORT (1:1) → EVENT
```

### Key Tables
- **users**: User accounts with roles
- **roles**: STUDENT, CLUB_HEAD, ADMIN
- **clubs**: RPSS clubs (7 pre-configured)
- **events**: Event lifecycle tracking
- **registrations**: Event registrations with payment tracking
- **passes**: QR code passes
- **feedback**: Ratings and comments
- **media**: Event photos/videos
- **reports**: Event analytics

---

## 🚀 Setup Instructions

### 1. Create Database
```sql
CREATE DATABASE rpss_db;
USE rpss_db;
-- Tables auto-created by Hibernate
```

### 2. Configure Properties
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/rpss_db
spring.datasource.username=root
spring.datasource.password=root
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### 3. Build & Run
```bash
cd /Users/user/Desktop/RPSS/eventra
mvn clean install
mvn spring-boot:run
# Opens at http://localhost:8080
```

---

## 🧪 TESTING GUIDE

### Test Scenario 1: Complete Event Lifecycle

**Step 1: Participant Registration**
- Go to: http://localhost:8080/auth/register
- Select Role: PARTICIPANT
- Fill form and submit
- Login with registered email

**Step 2: Club Head Creates Event**
- Register: Role = CLUB_HEAD
- Login to Dashboard
- Click "Create Event"
- Fill: Title, Description, Date, Venue, Capacity
- Submit → Event shows as PENDING

**Step 3: Admin Approves Event**
- Register/Login as ADMIN
- Go to: /admin/events/pending
- Click "Approve"
- Event becomes APPROVED and visible to students

**Step 4: Student Registers**
- Go to: /events
- Click event → Click "Register"
- QR pass auto-generated
- Download pass

**Step 5: Submit Feedback**
- After event → /feedback/event/{eventId}
- Rate (1-5) and comment
- Submit

**Step 6: Admin Generates Report**
- /admin/reports/{eventId}/generate
- View: Attendance %, Average Rating, Revenue

---

### Test Scenario 2: Access Control

**Verify Role-Based Access:**

| Page | Student | Club Head | Admin | Anonymous |
|------|-------------|-----------|-------|-----------|
| /dashboard | ✅ | ✅ | ✅ | ❌ |
| /events | ✅ | ✅ | ✅ | ✅ |
| /events/create | ❌ | ✅ | ❌ | ❌ |
| /admin/users | ❌ | ❌ | ✅ | ❌ |
| /feedback | ✅ | ✅ | ✅ | ❌ |

---

### Test Scenario 3: Event Registration Business Logic

**Test Cases:**

1. **Registration Success**
   - Participate registers for event with available seats
   - Expected: Registration CONFIRMED, Pass GENERATED

2. **Duplicate Registration**
   - Same student registers twice
   - Expected: Error "already registered"

3. **Full Event**
   - Register after max capacity reached
   - Expected: Error "no seats available"

4. **Paid Events**
   - Register for event with payment required
   - Expected: Payment pending status

5. **Registration Cancellation**
   - Click cancel registration
   - Expected: Status = CANCELLED

---

### Manual Testing Checklist

```
AUTHENTICATION
✓ Register with valid data
✓ Register with duplicate email (fails)
✓ Login with correct password
✓ Login with wrong password (fails)
✓ Logout
✓ Access protected page without login (redirects)
✓ Session timeout after 30 min

EVENT MANAGEMENT
✓ Club Head creates event
✓ Event starts as PENDING
✓ Admin approves event
✓ Event becomes APPROVED
✓ Approved events visible to students
✓ Admin rejects with reason
✓ Event can be cancelled

REGISTRATION & PASSES
✓ Student registers for event
✓ QR code generated automatically
✓ Pass can be scanned
✓ Cannot register twice
✓ Capacity prevents over-enrollment
✓ Available seats decrease correctly
✓ Registration can be cancelled

FEEDBACK
✓ Can submit 1-5 rating
✓ Comment is optional
✓ Average rating calculated
✓ Cannot submit twice
✓ Feedback visible in event details
✓ Can edit feedback
✓ Can delete feedback

ADMIN PANEL
✓ Add new user
✓ Create new club
✓ Assign club head
✓ Approve pending events
✓ Reject events with reason
✓ Generate event report
✓ View attendance stats
✓ View revenue stats
```

---

## 🔍 Key API Endpoints

### Authentication
```
GET  /auth/login                    Show login
POST /auth/login                    Process login
GET  /auth/register                 Show registration
POST /auth/register                 Process registration
GET  /auth/logout                   Logout
```

### Events
```
GET  /events                        All approved events
GET  /events/{id}                   Event details
GET  /events/create                 Create form (Club Head)
POST /events/create                 Create event
GET  /admin/events/pending          Pending (Admin)
POST /events/{id}/approve           Approve (Admin)
POST /events/{id}/reject            Reject (Admin)
```

### Registrations
```
POST /registrations/register/{id}   Register for event
GET  /registrations/my-registrations See my registrations
GET  /registrations/{id}/pass       View QR pass
POST /registrations/{id}/cancel     Cancel registration
```

### Feedback
```
GET  /feedback/event/{id}           Feedback form
POST /feedback/event/{id}           Submit feedback
GET  /feedback/event/{id}/all       View all feedback
POST /feedback/{id}/update          Update feedback
POST /feedback/{id}/delete          Delete feedback
```

### Admin
```
GET  /admin/users                   Manage users
GET  /admin/clubs                   Manage clubs
GET  /admin/reports                 View reports
POST /admin/reports/{id}/generate   Generate report
```

---

## 🌟 Features Included

✅ Role-based dashboards (Student, Club Head, Admin)
✅ Event lifecycle (PENDING → APPROVED → COMPLETED)
✅ Automatic QR code generation for passes
✅ Event capacity management
✅ Registration tracking
✅ Feedback & ratings system
✅ Media upload (photos/videos)
✅ Event reports with analytics
✅ User & club management
✅ Email notifications ready
✅ Payment integration ready
✅ Clean MVC architecture
✅ Database indexing
✅ Input validation
✅ Exception handling

---

## 🛠 Troubleshooting

### MySQL Connection Error
```
Solution:
1. Start MySQL: mysql.server start
2. Create DB: CREATE DATABASE rpss_db;
3. Verify in terminal: mysql rpss_db -u root
```

### Compilation Errors
```
Solution:
mvn clean compile
(Always use Maven, not direct javac)
```

### Port 8080 In Use
```
Change in application.properties:
server.port=8081
```

### Template Not Found
```
Check:
1. File exists in src/main/resources/templates/
2. Filename matches controller return value
3. File has .html extension
```

---

## 📱 Pre-configured Clubs

1. Codeverse Technical Club
2. Kalakruti Cultural Club
3. Strikers Sports Club
4. Innovation Club
5. Digisphere Digital Marketing Club
6. Samvedna Social Club
7. Photography Club

(Admin can add new clubs anytime)

---

## 🎯 Quick Start Test Workflow

1. **Start App**: `mvn spring-boot:run`
2. **Register Admin**: /auth/register → Role: ADMIN
3. **Create Club**: /admin/clubs/add
4. **Add Club Head**: /admin/users/add → Role: CLUB_HEAD
5. **Assign Club Head to Club**: /admin/clubs/edit
6. **Register Student**: /auth/register → Role: STUDENT
7. **Create Event**: Login as Club Head → /events/create
8. **Approve Event**: Login as Admin → /admin/events/pending → Approve
9. **Register Student**: Login as Student → /events → Register
10. **Submit Feedback**: /feedback/event/{eventId}
11. **Generate Report**: /admin/reports/{eventId}/generate

---

## 📝 File Structure

```
eventra/
├── src/main/java/com/eventra/eventra/
│   ├── enums/          (RoleEnum, EventStatus, etc.)
│   ├── model/          (Entity classes)
│   ├── repository/     (CRUD repositories)
│   ├── service/        (Business logic)
│   ├── controller/     (Web controllers)
│   └── dto/           (Data transfer objects)
├── src/main/resources/
│   ├── templates/      (Thymeleaf HTML)
│   │   ├── auth/      (Login/Register)
│   │   ├── dashboard/ (Dashboards)
│   │   ├── event/     (Event pages)
│   │   ├── admin/     (Admin panel)
│   │   └── ...
│   ├── static/        (CSS, JS, images)
│   └── application.properties
├── pom.xml           (Dependencies)
└── IMPLEMENTATION_GUIDE.md
```

---

**System is production-ready! 🚀**
