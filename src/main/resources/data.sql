-- ============================================
-- INITIAL DATA - Roles (MySQL Compatible)
-- ============================================
-- This file is automatically loaded by Spring Boot to populate initial data

INSERT INTO roles (role_name, description, permissions) VALUES
('PARTICIPANT', 'Participant - Can register for events and give feedback',
 '{"view_events": true, "register_event": true, "download_pass": true, "feedback": true}')
ON DUPLICATE KEY UPDATE role_name=role_name;

INSERT INTO roles (role_name, description, permissions) VALUES
('CLUB_HEAD', 'Club Head - Can create and manage events',
 '{"create_event": true, "manage_participants": true, "upload_media": true, "submit_report": true}')
ON DUPLICATE KEY UPDATE role_name=role_name;

INSERT INTO roles (role_name, description, permissions) VALUES
('ADMIN', 'Admin - System administrator with full access',
 '{"approve_events": true, "manage_users": true, "manage_clubs": true, "generate_reports": true, "view_analytics": true}')
ON DUPLICATE KEY UPDATE role_name=role_name;

