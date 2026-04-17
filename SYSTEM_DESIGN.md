# RPSS Event Management System - Design Documentation

## 📋 Overview
- **Role-Based Event Management System** (MVC Architecture - No REST API)
- **Database**: MySQL with JPA/Hibernate
- **Frontend**: Thymeleaf Templates (HTML/CSS/JS)
- **Backend**: Spring Boot Controllers → Services → Repositories

---

## 1️⃣ CLASS DIAGRAM (Entity Model)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        USER (Base Class)                             │
├─────────────────────────────────────────────────────────────────────┤
│ - userId (Long) [PRIMARY KEY - AUTO INCREMENT]                      │
│ - name (String)                                                      │
│ - email (String) [UNIQUE]                                           │
│ - password (String) [HASHED]                                        │
│ - phone (String)                                                     │
│ - registrationDate (LocalDateTime)                                  │
│ - isActive (Boolean)                                                │
├─────────────────────────────────────────────────────────────────────┤
│ Methods:                                                             │
│ + getRole(): Role                                                    │
│ + updateProfile(details): void                                       │
└─────────────────────────────────────────────────────────────────────┘

        ↓ (Role Assignment - One-to-One)

┌─────────────────────────────────────────────────────────────────────┐
│                         ROLE                                         │
├─────────────────────────────────────────────────────────────────────┤
│ - roleId (Long)                                                      │
│ - roleName (ENUM: STUDENT, CLUB_HEAD, ADMIN)                      │
│ - permissions (String)                                               │
├─────────────────────────────────────────────────────────────────────┤
│ Enum Values:                                                         │
│ • STUDENT - Register, View Events, Give Feedback                   │
│ • CLUB_HEAD - Create Events, Manage Students                        │
│ • ADMIN - Approve Events, Manage System, Generate Reports           │
└─────────────────────────────────────────────────────────────────────┘

        ↓ (Each Club Head linked to Club)

┌─────────────────────────────────────────────────────────────────────┐
│                         CLUB                                         │
├─────────────────────────────────────────────────────────────────────┤
│ - clubId (Long)                                                      │
│ - clubName (String) [UNIQUE]                                        │
│ - description (String)                                               │
│ - clubHead (User) [FOREIGN KEY - One-to-One]                        │
│ - createdDate (LocalDateTime)                                        │
│ - isActive (Boolean)                                                │
├─────────────────────────────────────────────────────────────────────┤
│ Predefined Clubs:                                                    │
│ • Codeverse Technical Club                                           │
│ • Kalakruti Cultural Club                                            │
│ • Strikers Sports Club                                               │
│ • Innovation Club                                                    │
│ • Digisphere Digital Marketing Club                                  │
│ • Samvedna Social Club                                               │
│ • Photography Club                                                   │
├─────────────────────────────────────────────────────────────────────┤
│ Methods:                                                             │
│ + addClubHead(user): void                                            │
│ + getEvents(): List<Event>                                           │
└─────────────────────────────────────────────────────────────────────┘

        ↓ (One Club - Many Events)

┌─────────────────────────────────────────────────────────────────────┐
│                         EVENT                                        │
├─────────────────────────────────────────────────────────────────────┤
│ - eventId (Long)                                                     │
│ - title (String)                                                     │
│ - description (String)                                               │
│ - eventDate (LocalDateTime)                                          │
│ - venue (String)                                                     │
│ - maxCapacity (Integer)                                              │
│ - club (Club) [FOREIGN KEY]                                         │
│ - createdBy (User - Club Head) [FOREIGN KEY]                        │
│ - status (ENUM: PENDING, APPROVED, REJECTED, COMPLETED)             │
│ - requiresPayment (Boolean)                                          │
│ - paymentAmount (BigDecimal) [Nullable if requiresPayment=false]    │
│ - createdDate (LocalDateTime)                                       │
│ - approvedBy (User - Admin) [FOREIGN KEY - Nullable]                │
│ - approvalDate (LocalDateTime) [Nullable]                           │
├─────────────────────────────────────────────────────────────────────┤
│ Methods:                                                             │
│ + submitForApproval(): void                                          │
│ + approve(admin): void                                               │
│ + reject(admin): void                                                │
│ + getRegistrationCount(): int                                        │
│ + isSpaceAvailable(): Boolean                                        │
└─────────────────────────────────────────────────────────────────────┘

        ↓ (One Event - Many Registrations)

┌─────────────────────────────────────────────────────────────────────┐
│                     REGISTRATION                                     │
├─────────────────────────────────────────────────────────────────────┤
│ - registrationId (Long)                                              │
│ - event (Event) [FOREIGN KEY]                                       │
│ - participant (User) [FOREIGN KEY]                                  │
│ - registrationDate (LocalDateTime)                                  │
│ - status (ENUM: CONFIRMED, CANCELLED)                               │
│ - paymentStatus (ENUM: PENDING, COMPLETED, REFUNDED) [If paid]      │
│ - transactionId (String) [Nullable]                                 │
├─────────────────────────────────────────────────────────────────────┤
│ Unique Constraint: (event, participant)                              │
│                                                                       │
│ Methods:                                                             │
│ + confirmRegistration(): void                                        │
│ + cancelRegistration(): void                                         │
│ + generatePass(): Pass                                               │
└─────────────────────────────────────────────────────────────────────┘

        ↓ (One Registration - One Pass)

┌─────────────────────────────────────────────────────────────────────┐
│                         PASS                                         │
├─────────────────────────────────────────────────────────────────────┤
│ - passId (Long)                                                      │
│ - registration (Registration) [FOREIGN KEY - One-to-One]            │
│ - qrCode (String) [UNIQUE]                                          │
│ - qrImagePath (String)                                               │
│ - generatedDate (LocalDateTime)                                     │
│ - expiryDate (LocalDateTime)                                        │
│ - isScanned (Boolean)                                                │
│ - scanDate (LocalDateTime) [Nullable]                               │
├─────────────────────────────────────────────────────────────────────┤
│ Methods:                                                             │
│ + generateQRCode(): void                                             │
│ + verifyQRCode(): Boolean                                            │
│ + markAsScanned(): void                                              │
│ + downloadPass(): File                                               │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                       FEEDBACK                                       │
├─────────────────────────────────────────────────────────────────────┤
│ - feedbackId (Long)                                                  │
│ - event (Event) [FOREIGN KEY]                                       │
│ - participant (User) [FOREIGN KEY]                                  │
│ - rating (Integer) [1-5 stars]                                      │
│ - comment (String)                                                   │
│ - submittedDate (LocalDateTime)                                     │
├─────────────────────────────────────────────────────────────────────┤
│ Unique Constraint: (event, participant)                              │
│                                                                       │
│ Methods:                                                             │
│ + submitFeedback(rating, comment): void                              │
│ + updateFeedback(rating, comment): void                              │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                       MEDIA                                          │
├─────────────────────────────────────────────────────────────────────┤
│ - mediaId (Long)                                                     │
│ - event (Event) [FOREIGN KEY]                                       │
│ - uploadedBy (User - Club Head) [FOREIGN KEY]                        │
│ - filePath (String)                                                  │
│ - fileType (ENUM: IMAGE, VIDEO, DOCUMENT)                           │
│ - fileSize (Long)                                                    │
│ - uploadDate (LocalDateTime)                                        │
│ - description (String)                                               │
├─────────────────────────────────────────────────────────────────────┤
│ Methods:                                                             │
│ + uploadMedia(file, type): void                                      │
│ + deleteMedia(): void                                                │
│ + getMediaURL(): String                                              │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        REPORT                                        │
├─────────────────────────────────────────────────────────────────────┤
│ - reportId (Long)                                                    │
│ - event (Event) [FOREIGN KEY]                                       │
│ - totalStudents (Integer)                                           │
│ - totalRevenue (BigDecimal)                                          │
│ - averageRating (Double)                                             │
│ - uploadedMediaCount (Integer)                                       │
│ - createdDate (LocalDateTime)                                       │
│ - reportPath (String)                                                │
├─────────────────────────────────────────────────────────────────────┤
│ Methods:                                                             │
│ + generateReport(): void                                             │
│ + exportToPDF(): File                                                │
│ + exportToExcel(): File                                              │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ ACTIVITY DIAGRAMS

### Activity Diagram 1: Event Approval Workflow
```
START
  │
  ├──> CLUB HEAD: Create Event
  │       ├── Fill: Title, Description, Date, Venue, Capacity
  │       ├── Choose: Payment Required? (Yes/No)
  │       ├── If Yes → Enter: Amount
  │       └── Submit for Approval
  │
  ├──> SYSTEM: Send Notification to Admin
  │
  ├──> ADMIN: Review Event
  │       ├──┬──> Approve?
  │       │  ├──> YES: Event Status = APPROVED
  │       │  │         Event Published
  │       │  │         Notification sent to all users
  │       │  │         ✓ Event visible to Students
  │       │  │
  │       │  └──> NO: Event Status = REJECTED
  │       │         Club Head notified with reason
  │       │         ✗ Event NOT published
  │       │
  │       └──> Optional: Request Changes
  │
  └──> END
```

### Activity Diagram 2: Registration & Pass Generation
```
START
  │
  ├──> STUDENT: View Event Listing
  │
  ├──> STUDENT: Click "Register Event"
  │
  ├──> SYSTEM: Check Event Capacity
  │       ├──> Space Available?
  │       │    ├──> YES: Continue
  │       │    └──> NO: Show "Event Full" ✗
  │       │
  │       └──> Check if Already Registered
  │            ├──> Already Registered: Show Error ✗
  │            └──> First Time: Continue
  │
  ├──> SYSTEM: Check Payment Required?
  │       │
  │       ├──> YES: Payment Flow
  │       │       ├── Redirect to Payment Gateway
  │       │       ├── Process Payment
  │       │       ├──┬──> Payment Success?
  │       │       │  ├──> YES: paymentStatus = COMPLETED
  │       │       │  └──> NO: paymentStatus = PENDING [Retry]
  │       │       │
  │       │       └── Registration Status = CONFIRMED
  │       │
  │       └──> NO: Direct Registration
  │           └── Registration Status = CONFIRMED
  │
  ├──> SYSTEM: Generate QR Code Pass
  │       ├── Create unique QR code
  │       ├── Generate QR image
  │       └── Store in database
  │
  ├──> SYSTEM: Send Confirmation Email
  │       ├── Event details
  │       ├── QR pass attachment
  │       └── Entry Instructions
  │
  ├──> STUDENT: Download Pass (PDF with QR)
  │
  └──> END
```

### Activity Diagram 3: Post-Event (Club Head)
```
START
  │
  ├──> EVENT: Happens (Physically)
  │
  ├──> CLUB HEAD: Log in → Go to Completed Events
  │
  ├──> CLUB HEAD: Select Event → "Post-Event Actions"
  │
  ├──> CLUB HEAD: Upload Photos/Videos
  │       └── Select files → Upload → System stores with metadata
  │
  ├──> CLUB HEAD: Generate Event Report
  │       ├── System auto-calculates:
  │       │    ├── Total registered: Count from Registration table
  │       │    ├── Total revenue: Sum of payments
  │       │    ├── Attendance: Count from scanned passes
  │       │    ├── Avg feedback rating: Average from Feedback
  │       │    └── Media count: Count uploaded files
  │       │
  │       └── Export as PDF/Excel
  │
  └──> END
```

### Activity Diagram 4: Student Feedback Flow
```
START
  │
  ├──> EVENT: Completed
  │
  ├──> STUDENT: Receives Email "Please give Feedback"
  │
  ├──> STUDENT: Log in → "My Events" → Select Event
  │
  ├──> STUDENT: Click "Give Feedback"
  │
  ├──> SYSTEM: Show Feedback Form
  │       ├── Rating: 1-5 Stars
  │       ├── Comment: Text area
  │       └── Submit button
  │
  ├──> STUDENT: Submit Feedback
  │
  ├──> SYSTEM: Store Feedback in Database
  │       └── Link to Event & Student (Unique constraint)
  │
  ├──> ADMIN: View All Feedbacks
  │       ├── Dashboard shows avg rating
  │       ├── Can see individual comments
  │       └── Use in Report Generation
  │
  └──> END
```

### Activity Diagram 5: Admin Dashboard & Reporting
```
START
  │
  ├──> ADMIN: Log in
  │
  ├──> ADMIN DASHBOARD: See Real-time Analytics
  │       ├── Total Events (Pending, Approved, Completed)
  │       ├── Total Registrations
  │       ├── Total Revenue
  │       ├── Active Clubs
  │       └── Active Users
  │
  ├──> ADMIN: Can Perform:
  │       │
  │       ├──> 1️⃣ Approve/Reject Events
  │       │
  │       ├──> 2️⃣ View Event Details
  │       │       └── All student registrations
  │       │           All feedback
  │       │           Media uploads
  │       │
  │       ├──> 3️⃣ Manage Clubs
  │       │       ├── View all clubs
  │       │       ├── Add new club
  │       │       ├── Assign club head
  │       │       └── View club events
  │       │
  │       ├──> 4️⃣ Manage Users
  │       │       ├── View all users
  │       │       ├── Edit role
  │       │       ├── Activate/Deactivate
  │       │       └── View activity
  │       │
  │       └──> 5️⃣ Generate Reports
  │           ├── Event-wise Report (PDF/Excel)
  │           ├── Club-wise Report (PDF/Excel)
  │           ├── Revenue Report (PDF/Excel)
  │           └── User Activity Report (PDF/Excel)
  │
  └──> END
```

---

## 3️⃣ DATABASE SCHEMA (SQL DDL)

```sql
-- ============================================
-- USERS TABLE (Base Table)
-- ============================================
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    role_id BIGINT,
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

-- ============================================
-- ROLES TABLE
-- ============================================
CREATE TABLE roles (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name ENUM('STUDENT', 'CLUB_HEAD', 'ADMIN') UNIQUE NOT NULL,
    description VARCHAR(255),
    permissions JSON
);

-- Insert predefined roles
INSERT INTO roles (role_name, description) VALUES
('STUDENT', 'Can register for events and give feedback'),
('CLUB_HEAD', 'Can create and manage club events'),
('ADMIN', 'Full system access and approvals');

-- ============================================
-- CLUBS TABLE
-- ============================================
CREATE TABLE clubs (
    club_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    club_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    club_head_id BIGINT UNIQUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (club_head_id) REFERENCES users(user_id)
);

-- Insert predefined clubs (no head assigned yet)
INSERT INTO clubs (club_name, description, is_active) VALUES
('Codeverse Technical Club', 'Technical and coding activities', TRUE),
('Kalakruti Cultural Club', 'Cultural and arts events', TRUE),
('Strikers Sports Club', 'Sports and fitness activities', TRUE),
('Innovation Club', 'Innovation and entrepreneurship', TRUE),
('Digisphere Digital Marketing Club', 'Digital marketing and social media', TRUE),
('Samvedna Social Club', 'Social cause and community service', TRUE),
('Photography Club', 'Photography and visual arts', TRUE);

-- ============================================
-- EVENTS TABLE
-- ============================================
CREATE TABLE events (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    event_date DATETIME NOT NULL,
    venue VARCHAR(200) NOT NULL,
    max_capacity INT NOT NULL,
    club_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED') DEFAULT 'PENDING',
    requires_payment BOOLEAN DEFAULT FALSE,
    payment_amount DECIMAL(10, 2),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approval_date DATETIME,
    FOREIGN KEY (club_id) REFERENCES clubs(club_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    FOREIGN KEY (approved_by) REFERENCES users(user_id),
    INDEX idx_club (club_id),
    INDEX idx_status (status),
    INDEX idx_event_date (event_date)
);

-- ============================================
-- REGISTRATIONS TABLE
-- ============================================
CREATE TABLE registrations (
    registration_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('CONFIRMED', 'CANCELLED') DEFAULT 'CONFIRMED',
    payment_status ENUM('PENDING', 'COMPLETED', 'REFUNDED'),
    transaction_id VARCHAR(100),
    UNIQUE KEY unique_event_participant (event_id, participant_id),
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES users(user_id),
    INDEX idx_participant (participant_id),
    INDEX idx_event (event_id)
);

-- ============================================
-- PASSES TABLE (QR Code)
-- ============================================
CREATE TABLE passes (
    pass_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT UNIQUE NOT NULL,
    qr_code VARCHAR(255) UNIQUE NOT NULL,
    qr_image_path VARCHAR(255),
    generated_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    expiry_date DATETIME,
    is_scanned BOOLEAN DEFAULT FALSE,
    scan_date DATETIME,
    FOREIGN KEY (registration_id) REFERENCES registrations(registration_id) ON DELETE CASCADE
);

-- ============================================
-- FEEDBACK TABLE
-- ============================================
CREATE TABLE feedback (
    feedback_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    submitted_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_event_feedback (event_id, participant_id),
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES users(user_id),
    INDEX idx_event (event_id)
);

-- ============================================
-- MEDIA TABLE (Photos/Videos)
-- ============================================
CREATE TABLE media (
    media_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_type ENUM('IMAGE', 'VIDEO', 'DOCUMENT') NOT NULL,
    file_size BIGINT,
    upload_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255),
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id),
    INDEX idx_event (event_id)
);

-- ============================================
-- REPORTS TABLE
-- ============================================
CREATE TABLE reports (
    report_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    total_students INT,
    total_revenue DECIMAL(12, 2),
    average_rating DECIMAL(3, 2),
    uploaded_media_count INT,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    report_path VARCHAR(255),
    FOREIGN KEY (event_id) REFERENCES events(event_id),
    INDEX idx_event (event_id)
);

-- ============================================
-- AUDIT LOG TABLE (Optional - for tracking admin actions)
-- ============================================
CREATE TABLE audit_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(100),
    table_name VARCHAR(50),
    record_id BIGINT,
    old_value JSON,
    new_value JSON,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_user (user_id),
    INDEX idx_created (created_date)
);
```

---

## 4️⃣ PROJECT STRUCTURE (Spring Boot MVC)

```
eventra/
├── src/main/java/com/eventra/eventra/
│   └── EventraApplication.java
│
├── model/ (JPA Entities - Match Database)
│   ├── User.java
│   ├── Role.java
│   ├── Club.java
│   ├── Event.java
│   ├── Registration.java
│   ├── Pass.java
│   ├── Feedback.java
│   ├── Media.java
│   └── Report.java
│
├── repository/ (Database Access)
│   ├── UserRepository.java
│   ├── EventRepository.java
│   ├── RegistrationRepository.java
│   ├── PassRepository.java
│   ├── FeedbackRepository.java
│   └── ClubRepository.java
│
├── service/ (Business Logic)
│   ├── UserService.java
│   ├── EventService.java
│   ├── RegistrationService.java
│   ├── PassService.java
│   ├── FeedbackService.java
│   ├── PaymentService.java
│   ├── EmailService.java
│   ├── ReportService.java
│   └── QRCodeService.java
│
├── controller/ (MVC Controllers - NO REST API)
│   ├── AuthController.java       [Login/Register]
│   ├── EventController.java      [View, Create, Approve Events]
│   ├── RegistrationController.java [Register for event]
│   ├── DashboardController.java   [Role-based dashboards]
│   ├── AdminController.java       [Admin operations]
│   ├── FeedbackController.java     [Feedback management]
│   └── ReportController.java       [Report generation]
│
├── resources/
│   ├── templates/
│   │   ├── layout/
│   │   │   └── base.html [Common theme]
│   │   ├── auth/
│   │   │   ├── login.html
│   │   │   ├── register.html
│   │   │   └── select-role.html
│   │   ├── event/
│   │   │   ├── create-event.html
│   │   │   ├── event-list.html
│   │   │   ├── event-details.html
│   │   │   └── event-approval.html
│   │   ├── dashboard/
│   │   │   ├── student-dashboard.html
│   │   │   ├── club-head-dashboard.html
│   │   │   ├── admin-dashboard.html
│   │   │   └── analytics.html
│   │   ├── registration/
│   │   │   ├── my-registrations.html
│   │   │   └── pass-download.html
│   │   └── feedback/
│   │       └── feedback-form.html
│   │
│   ├── static/
│   │   ├── css/
│   │   │   ├── style.css
│   │   │   ├── dashboard.css
│   │   │   └── form.css
│   │   ├── js/
│   │   │   ├── form-validation.js
│   │   │   ├── modal.js
│   │   │   └── chart.js [For analytics]
│   │   └── images/
│   │       ├── logo.png
│   │       └── icons/
│   │
│   └── application.properties
│       ├── spring.datasource.url=jdbc:mysql://localhost:3306/rpss_db
│       ├── spring.datasource.username=root
│       ├── spring.datasource.password=password
│       ├── spring.jpa.hibernate.ddl-auto=update
│       └── spring.thymeleaf.prefix=classpath:/templates/
│
└── pom.xml
    └── Dependencies already added (JPA, Thymeleaf, MySQL)
```

---

## 5️⃣ TESTING STRATEGY (MVC - No REST API)

### Test Pyramid
```
                    🎯
                 INTEGRATION TESTS
               (Controller + Service)
              /                      \
          20%/                        \20%
            /                          \
       🔷──────────────────────────────🔷
       │    UNIT TESTS (Service Layer)    │
       │              60%                 │
       │                                  │
       🔷──────────────────────────────🔷
```

### 1. **Unit Tests (Services)** - 60%
Test business logic in isolation

```java
// src/test/java/com/eventra/eventra/service/EventServiceTest.java

@SpringBootTest
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    // TEST 1: Create Event
    @Test
    void testCreateEventSuccessfully() {
        // Arrange
        Event event = new Event();
        event.setTitle("RPSS Tech Fest 2026");
        event.setRequiresPayment(true);
        event.setPaymentAmount(500);

        // Act
        eventService.createEvent(event);

        // Assert
        verify(eventRepository, times(1)).save(event);
        assertEquals("PENDING", event.getStatus());
    }

    // TEST 2: Approve Event
    @Test
    void testApproveEvent() {
        Event event = new Event();
        event.setEventId(1L);
        event.setStatus("PENDING");

        eventService.approveEvent(1L, admin);

        assertEquals("APPROVED", event.getStatus());
        assertNotNull(event.getApprovalDate());
    }

    // TEST 3: Check Payment Amount is NULL for free events
    @Test
    void testFreeEventHasNullPayment() {
        Event event = new Event();
        event.setRequiresPayment(false);

        assertNull(event.getPaymentAmount());
    }
}
```

### 2. **Integration Tests (Controller + Service)** - 20%
Test full request-response cycle

```java
// src/test/java/com/eventra/eventra/controller/EventControllerIT.java

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    // TEST 1: Create Event Form Submission
    @Test
    void testCreateEventForm_ValidSubmission() throws Exception {
        String eventTitle = "Tech Fest 2026";

        mockMvc.perform(post("/event/create")
                .param("title", eventTitle)
                .param("date", "2026-05-20")
                .param("venue", "Main Hall")
                .param("capacity", "100")
                .param("requiresPayment", "true")
                .param("paymentAmount", "500")
                .flashAttr("club", club))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard/club-head"));

        // Verify event was saved
        Event saved = eventRepository.findByTitle(eventTitle);
        assertNotNull(saved);
        assertEquals("PENDING", saved.getStatus());
    }

    // TEST 2: Event Approval Workflow
    @Test
    void testApproveEvent_AdminApproves() throws Exception {
        Event event = createAndSaveEvent("Pending Event");

        mockMvc.perform(post("/event/{eventId}/approve", event.getEventId())
                .param("approved", "true")
                .flashAttr("admin", admin))
            .andExpect(status().is3xxRedirection());

        Event updated = eventRepository.findById(event.getEventId()).get();
        assertEquals("APPROVED", updated.getStatus());
    }

    // TEST 3: Register for Event - Free Event
    @Test
    void testRegisterForFreeEvent() throws Exception {
        Event freeEvent = createEvent("Free Event", false, null);

        mockMvc.perform(post("/registration/register/{eventId}", freeEvent.getEventId())
                .sessionAttr("user", student))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/registration/*/download"));

        // Verify registration created
        Registration reg = registrationRepository.findByEventAndStudent(freeEvent, student);
        assertNotNull(reg);
        assertEquals("CONFIRMED", reg.getStatus());
    }

    // TEST 4: Register for Event - Paid Event (with Payment)
    @Test
    void testRegisterForPaidEvent_WithPayment() throws Exception {
        Event paidEvent = createEvent("Workshop", true, 500.0);

        mockMvc.perform(post("/registration/register/{eventId}", paidEvent.getEventId())
                .sessionAttr("user", student))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/payment"));  // Redirect to payment

        // Verify registration with PENDING payment
        Registration reg = registrationRepository.findByEventAndStudent(paidEvent, student);
        assertEquals("PENDING", reg.getPaymentStatus());
    }

    // TEST 5: Submit Feedback
    @Test
    void testSubmitFeedback() throws Exception {
        Event event = createEvent("Past Event", false, null);
        Registration registrations = registerUser(event, student);

        mockMvc.perform(post("/feedback/submit/{eventId}", event.getEventId())
                .param("rating", "5")
                .param("comment", "Excellent event!")
                .sessionAttr("user", student))
            .andExpect(status().is3xxRedirection());

        Feedback fb = feedbackRepository.findByEventAndStudent(event, student);
        assertEquals(5, fb.getRating());
    }
}
```

### 3. **UI/Form Tests** - Testing with Thymeleaf Templates

```java
// src/test/java/com/eventra/eventra/controller/UIFormTest.java

@SpringBootTest
@AutoConfigureMockMvc
class UIFormTest {

    @Autowired
    private MockMvc mockMvc;

    // TEST: Event Creation Form Validation
    @Test
    void testEventCreationForm_InvalidData() throws Exception {
        mockMvc.perform(post("/event/create")
                .param("title", "")  // EMPTY TITLE - INVALID
                .param("date", "2026-05-20")
                .param("capacity", "-10"))  // NEGATIVE CAPACITY - INVALID
            .andExpect(status().isOk())
            .andExpect(view().name("event/create-event"))
            .andExpect(model().attributeHasFieldErrors("event", "title"))
            .andExpect(model().attributeHasFieldErrors("event", "capacity"));
    }

    // TEST: Role Selection Page
    @Test
    void testRoleSelectionPage() throws Exception {
        mockMvc.perform(get("/auth/select-role"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/select-role"))
            .andExpect(model().attributeExists("roles"));
    }
}
```

### 4. **End-to-End Tests** - Full Workflow Simulation

```java
// src/test/java/com/eventra/eventra/E2EWorkflowTest.java

@SpringBootTest
@AutoConfigureMockMvc
class E2EWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    // COMPLETE FLOW: Event Creation → Approval → Registration → Pass Download
    @Test
    void testCompleteEventWorkflow() throws Exception {

        // STEP 1: Club Head Registers
        User clubHead = registerUser("club@example.com", Role.CLUB_HEAD);

        // STEP 2: Club Head Creates Event
        mockMvc.perform(post("/event/create")
                .sessionAttr("user", clubHead)
                .param("title", "RPSS Hackathon")
                .param("date", "2026-06-15")
                .param("capacity", "50")
                .param("requiresPayment", "false"))
            .andExpect(status().is3xxRedirection());

        Event event = eventRepository.findByTitle("RPSS Hackathon");

        // STEP 3: Admin Approves Event
        User admin = registerUser("admin@rpss.com", Role.ADMIN);
        mockMvc.perform(post("/event/{id}/approve", event.getId())
                .sessionAttr("user", admin))
            .andExpect(status().is3xxRedirection());

        // STEP 4: Participant Registers
        User participant = registerUser("participant@example.com", Role.PARTICIPANT);
        mockMvc.perform(post("/registration/register/{eventId}", event.getId())
                .sessionAttr("user", participant))
            .andExpect(status().is3xxRedirection());

        // STEP 5: Participant Downloads Pass
        mockMvc.perform(get("/registration/pass/download")
                .sessionAttr("user", participant))
            .andExpect(status().isOk());

        // STEP 6: Participant Gives Feedback
        mockMvc.perform(post("/feedback/submit/{eventId}", event.getId())
                .param("rating", "5")
                .sessionAttr("user", participant))
            .andExpect(status().is3xxRedirection());

        // STEP 7: Admin Generates Report
        mockMvc.perform(get("/report/download/{eventId}", event.getId())
                .sessionAttr("user", admin))
            .andExpect(status().isOk());
    }
}
```

### 5. **Test Execution & Commands**

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EventServiceTest

# Run with coverage report
mvn test jacoco:report

# Run integration tests only
mvn test -Dgroups=integration

# Generate HTML test report
mvn surefire-report:report
```

---

## 6️⃣ KEY CHANGES FROM YOUR FEEDBACK

| Item | Change | Reason |
|------|--------|--------|
| `User.userId` | ✅ KEEP - Auto-generated PK | JPA requires primary key |
| Payment | ✅ Optional per event | `requiresPayment` boolean |
| Admin Roles | ✅ Role-based (ADMIN enum) | HOD, Activity Coord, Office Bearers |
| Clubs | ✅ Predefined + Admin Can Add | 7 clubs + future expansion |
| REST API | ❌ NOT USING | Using Thymeleaf (MVC) |
| Role field | ✅ Separate ROLE table | Better than hardcoding |

---

## 7️⃣ APPLICATION FLOW DIAGRAM

```
                    ┌─────────────────────┐
                    │   User Opens App    │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Select Role       │
                    │  [3 Options]        │
                    └──────────┬──────────┘
                        ┌──────┼──────┐
                        │      │      │
          ┌─────────────▼─┐ ┌──▼────┐ ┌─▼──────────┐
          │ STUDENT   │ │CLUB   │ │ADMIN       │
          │ Dashboard     │ │HEAD   │ │Dashboard   │
          │ - View Events │ │      │ │ - Approve  │
          │ - Register    │ │Dashboard  │ - Manage  │
          │ - Pay (if req)│ │ - Create │ - Report  │
          │ - DL Pass     │ │ - Submit │ - Analytics
          │ - Feedback    │ │ - Upload │           │
          └───────────────┘ └────────┘ └───────────┘
```

---

## 8️⃣ NEXT STEPS

1. ✅ Create model classes (use provided schema)
2. ✅ Create repositories (extends JpaRepository)
3. ✅ Create services (business logic)
4. ✅ Create controllers (MVC - returns templates)
5. ✅ Create Thymeleaf templates (HTML/CSS)
6. ✅ Implement tests (Unit → Integration → E2E)
7. ✅ Setup email notifications
8. ✅ Setup QR code generation
9. ✅ Deploy & test

---

## ❓ FAQ for Clarification

**Q: Why keep userId in User class?**
A: JPA/Hibernate requires a primary key for database mapping. It's auto-generated and doesn't conflict with role-based access.

**Q: What if payment fails?**
A: `paymentStatus` stays PENDING. Student can retry from dashboard.

**Q: Can a user have multiple roles?**
A: Currently NO (One-to-One User:Role). Can be extended if needed.

**Q: How to add new club?**
A: Admin goes to "Manage Clubs" → Add new → System creates entry in CLUBS table.

**Q: How to test without REST API?**
A: Use MockMvc for form submissions, Thymeleaf template rendering, assertion testing.
