# RPSS - Database Setup & Initial Entity Setup

## 📁 File: database_schema.sql

```sql
-- ============================================
-- RPSS EVENT MANAGEMENT SYSTEM DATABASE
-- ============================================
-- MySQL Database Setup Script
-- Run this to initialize the database

-- Drop existing database if exists
DROP DATABASE IF EXISTS rpss_db;
CREATE DATABASE rpss_db;
USE rpss_db;

-- ============================================
-- 1. ROLES TABLE
-- ============================================
CREATE TABLE roles (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name ENUM('PARTICIPANT', 'CLUB_HEAD', 'ADMIN') UNIQUE NOT NULL,
    description VARCHAR(255),
    permissions JSON,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (role_name, description, permissions) VALUES
('PARTICIPANT', 'Can register for events, download pass, and give feedback',
 JSON_OBJECT('view_events', true, 'register_event', true, 'download_pass', true, 'feedback', true)),
('CLUB_HEAD', 'Can create events, manage participants, upload media',
 JSON_OBJECT('create_event', true, 'manage_participants', true, 'upload_media', true, 'submit_report', true)),
('ADMIN', 'Full system access - approve events, manage users and clubs, generate reports',
 JSON_OBJECT('approve_events', true, 'manage_users', true, 'manage_clubs', true, 'generate_reports', true, 'view_analytics', true));

-- ============================================
-- 2. USERS TABLE
-- ============================================
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15) UNIQUE,
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    last_login DATETIME,
    role_id BIGINT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES roles(role_id),
    INDEX idx_email (email),
    INDEX idx_role (role_id)
);

-- ============================================
-- 3. CLUBS TABLE
-- ============================================
CREATE TABLE clubs (
    club_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    club_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    club_head_id BIGINT UNIQUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (club_head_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_active (is_active)
);

-- Insert predefined clubs (without club heads initially)
INSERT INTO clubs (club_name, description, is_active) VALUES
('Codeverse Technical Club', 'Technical coding competitions, webinars, workshops', TRUE),
('Kalakruti Cultural Club', 'Cultural programs, performances, art exhibitions', TRUE),
('Strikers Sports Club', 'Sports tournaments, fitness activities, outdoor events', TRUE),
('Innovation Club', 'Innovation challenges, entrepreneurship talks, hackathons', TRUE),
('Digisphere Digital Marketing Club', 'Social media campaigns, digital branding, content creation', TRUE),
('Samvedna Social Club', 'Social cause initiatives, community service, awareness drives', TRUE),
('Photography Club', 'Photography workshops, exhibitions, visual arts events', TRUE);

-- ============================================
-- 4. EVENTS TABLE
-- ============================================
CREATE TABLE events (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    event_date DATETIME NOT NULL,
    venue VARCHAR(200) NOT NULL,
    max_capacity INT NOT NULL,
    club_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    requires_payment BOOLEAN DEFAULT FALSE,
    payment_amount DECIMAL(10, 2),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approval_date DATETIME,
    rejection_reason VARCHAR(500),
    FOREIGN KEY (club_id) REFERENCES clubs(club_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    FOREIGN KEY (approved_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_club (club_id),
    INDEX idx_status (status),
    INDEX idx_event_date (event_date),
    INDEX idx_created_by (created_by)
);

-- ============================================
-- 5. REGISTRATIONS TABLE
-- ============================================
CREATE TABLE registrations (
    registration_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('CONFIRMED', 'CANCELLED', 'ATTENDED') DEFAULT 'CONFIRMED',
    payment_status ENUM('NOT_REQUIRED', 'PENDING', 'COMPLETED', 'REFUNDED') DEFAULT 'NOT_REQUIRED',
    transaction_id VARCHAR(100),
    payment_date DATETIME,
    UNIQUE KEY unique_event_participant (event_id, participant_id),
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_participant (participant_id),
    INDEX idx_event (event_id),
    INDEX idx_status (status)
);

-- ============================================
-- 6. PASSES TABLE (QR Code Entry Pass)
-- ============================================
CREATE TABLE passes (
    pass_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT UNIQUE NOT NULL,
    qr_code VARCHAR(500) UNIQUE NOT NULL,
    qr_image_path VARCHAR(500),
    generated_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    expiry_date DATETIME,
    is_scanned BOOLEAN DEFAULT FALSE,
    scan_date DATETIME,
    scanned_by BIGINT,
    FOREIGN KEY (registration_id) REFERENCES registrations(registration_id) ON DELETE CASCADE,
    INDEX idx_qr_code (qr_code),
    INDEX idx_is_scanned (is_scanned)
);

-- ============================================
-- 7. FEEDBACK TABLE
-- ============================================
CREATE TABLE feedback (
    feedback_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    submitted_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_event_feedback (event_id, participant_id),
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_event (event_id),
    INDEX idx_rating (rating)
);

-- ============================================
-- 8. MEDIA TABLE (Photos/Videos/Documents)
-- ============================================
CREATE TABLE media (
    media_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type ENUM('IMAGE', 'VIDEO', 'DOCUMENT') NOT NULL,
    file_size BIGINT,
    upload_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500),
    is_approved BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id),
    INDEX idx_event (event_id),
    INDEX idx_file_type (file_type)
);

-- ============================================
-- 9. REPORTS TABLE
-- ============================================
CREATE TABLE reports (
    report_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT UNIQUE NOT NULL,
    total_participants INT DEFAULT 0,
    total_attended INT DEFAULT 0,
    total_revenue DECIMAL(12, 2) DEFAULT 0,
    average_rating DECIMAL(3, 2),
    uploaded_media_count INT DEFAULT 0,
    feedback_count INT DEFAULT 0,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    report_path VARCHAR(500),
    generated_by BIGINT,
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (generated_by) REFERENCES users(user_id),
    INDEX idx_event (event_id)
);

-- ============================================
-- 10. AUDIT LOG TABLE (Track admin actions)
-- ============================================
CREATE TABLE audit_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    module VARCHAR(50),
    record_id BIGINT,
    old_value JSON,
    new_value JSON,
    ip_address VARCHAR(50),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_created (created_date),
    INDEX idx_action (action)
);

-- ============================================
-- 11. EMAIL LOG TABLE (Track notifications)
-- ============================================
CREATE TABLE email_log (
    email_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_email VARCHAR(100) NOT NULL,
    subject VARCHAR(255),
    email_type VARCHAR(50),
    sent_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_successful BOOLEAN,
    error_message VARCHAR(500),
    FOREIGN KEY recipient_id REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_sent_date (sent_date),
    INDEX idx_recipient (recipient_email)
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX idx_event_approval ON events(status, approval_date);
CREATE INDEX idx_registration_payment ON registrations(event_id, payment_status);
CREATE INDEX idx_feedback_rating ON feedback(event_id, rating);

-- ============================================
-- VIEWS (For easier querying)
-- ============================================

-- View: Upcoming Events (Next 30 days)
CREATE VIEW upcoming_events AS
SELECT
    e.event_id,
    e.title,
    e.event_date,
    c.club_name,
    e.max_capacity,
    COUNT(r.registration_id) as registered_count,
    (e.max_capacity - COUNT(r.registration_id)) as available_seats
FROM events e
JOIN clubs c ON e.club_id = c.club_id
LEFT JOIN registrations r ON e.event_id = r.event_id AND r.status = 'CONFIRMED'
WHERE e.status = 'APPROVED'
  AND e.event_date > NOW()
  AND e.event_date <= DATE_ADD(NOW(), INTERVAL 30 DAY)
GROUP BY e.event_id;

-- View: Event Statistics
CREATE VIEW event_statistics AS
SELECT
    e.event_id,
    e.title,
    COUNT(DISTINCT r.participant_id) as total_registrations,
    SUM(CASE WHEN r.payment_status = 'COMPLETED' THEN r.event_id ELSE 0 END) as paid_registrations,
    SUM(e.payment_amount * COUNT(r.registration_id)) as total_revenue,
    AVG(f.rating) as average_rating,
    COUNT(DISTINCT f.feedback_id) as feedback_count
FROM events e
LEFT JOIN registrations r ON e.event_id = r.event_id AND r.status IN ('CONFIRMED', 'ATTENDED')
LEFT JOIN feedback f ON e.event_id = f.event_id
GROUP BY e.event_id;

-- View: Club Performance
CREATE VIEW club_performance AS
SELECT
    c.club_id,
    c.club_name,
    COUNT(DISTINCT e.event_id) as total_events,
    COUNT(DISTINCT r.participant_id) as total_participants,
    SUM(r.event_id) as total_revenue,
    AVG(f.rating) as average_event_rating
FROM clubs c
LEFT JOIN events e ON c.club_id = e.club_id AND e.status = 'APPROVED'
LEFT JOIN registrations r ON e.event_id = r.event_id
LEFT JOIN feedback f ON e.event_id = f.event_id
GROUP BY c.club_id;

-- ============================================
-- STORED PROCEDURES (Optional - For complex operations)
-- ============================================

-- Procedure to generate event report
DELIMITER //
CREATE PROCEDURE generate_event_report(IN p_event_id BIGINT)
BEGIN
    INSERT INTO reports (event_id, total_participants, total_revenue, average_rating, feedback_count, generated_date)
    SELECT
        e.event_id,
        COUNT(DISTINCT r.participant_id),
        SUM(CASE WHEN r.payment_status = 'COMPLETED' THEN e.payment_amount ELSE 0 END),
        AVG(f.rating),
        COUNT(DISTINCT f.feedback_id),
        NOW()
    FROM events e
    LEFT JOIN registrations r ON e.event_id = r.event_id
    LEFT JOIN feedback f ON e.event_id = f.event_id
    WHERE e.event_id = p_event_id;
END //
DELIMITER ;

-- ============================================
-- DATA VERIFICATION QUERIES
-- ============================================

-- Check all tables created
SELECT table_name FROM information_schema.tables WHERE table_schema = 'rpss_db';

-- Check data in clubs
SELECT * FROM clubs;

-- Check roles
SELECT * FROM roles;
```

## 🚀 How to Execute This Script

### Option 1: Using MySQL CLI
```bash
mysql -u root -p < database_schema.sql

# Then enter your password when prompted
```

### Option 2: Using MySQL Workbench
1. Open MySQL Workbench
2. File → Open SQL Script
3. Select `database_schema.sql`
4. Execute (Ctrl + Shift + Enter)

### Option 3: Using Spring Boot (Auto-creation)
In `application.properties`:
```properties
spring.jpa.hibernate.ddl-auto=create-drop  # Creates on startup, drops on shutdown
# OR
spring.jpa.hibernate.ddl-auto=update  # Creates if not exists, updates schema
```

---

## ✅ Verification Queries

After running the script, verify:

```sql
-- Check all tables exist
USE rpss_db;
SHOW TABLES;

-- Check roles created
SELECT * FROM roles;

-- Check clubs inserted
SELECT * FROM clubs;

-- Check structure of users table
DESC users;

-- Check sample data
SELECT COUNT(*) as total_clubs FROM clubs;
```

Expected Output:
```
7 clubs in clubs table
3 roles in roles table
0 users (will be created during registration)
```
