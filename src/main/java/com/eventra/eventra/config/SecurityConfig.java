package com.eventra.eventra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Spring Security Configuration for Eventra - RBAC System
 * 
 * Role Hierarchy: ADMIN > CLUB_HEAD > PARTICIPANT
 * This means:
 * - ADMIN has all permissions of CLUB_HEAD and PARTICIPANT
 * - CLUB_HEAD has all permissions of PARTICIPANT (but not ADMIN)
 * - PARTICIPANT is the base role
 * 
 * Endpoint Protection:-- First, verify the ADMIN role exists
SELECT * FROM roles WHERE role_name = 'ADMIN';

-- If ADMIN role doesn't exist, create it:
INSERT INTO roles (role_name, description, created_date) 
VALUES ('ADMIN', 'Admin - System administrator with full access', NOW());

-- Now create the admin user
-- Using a default password: Admin@123
-- BCrypt hash for "Admin@123" (12 rounds): $2a$12$zLIYG5gJrTuPsScMVOwfeeA0VKV0wLvl7fBlcsV9nLVkVKV3u6cPm

INSERT INTO users (
  name, 
  email, 
  phone, 
  password, 
  role_id, 
  is_active, 
  approval_status, 
  registration_date
) VALUES (
  'Rachana Kirange',
  'mca25.rachanakirange@asmedu.org',
  NULL,
  '$2a$12$zLIYG5gJrTuPsScMVOwfeeA0VKV0wLvl7fBlcsV9nLVkVKV3u6cPm',
  (SELECT role_id FROM roles WHERE role_name = 'ADMIN'),
  true,
  'APPROVED',
  NOW()
);

-- Verify the admin was created
SELECT u.*, r.role_name FROM users u 
JOIN roles r ON u.role_id = r.role_id 
WHERE u.email = 'mca25.rachanakirange@asmedu.org';
 * - /admin/** → ADMIN only
 * - /club/** → CLUB_HEAD and ADMIN
 * - /user/** → Authenticated users (PARTICIPANT, CLUB_HEAD, ADMIN)
 * - Public endpoints: /, /about, /contact, /faq, /auth/**, /css/**, /js/**, /images/**, /uploads/**, /webjars/**
 * 
 * Note: This application uses session-based authentication with custom AuthController.
 * Spring Security roles are checked via hasAuthority() in endpoint configuration.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable default form login - application uses custom session-based authentication
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // Enable session management with security context repository for custom auth
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository())
            )
            .sessionManagement(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/",
                    "/about",
                    "/contact",
                    "/faq",
                    "/auth/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/uploads/**",
                    "/webjars/**"
                ).permitAll()
                
                // Admin endpoints - ADMIN role only
                // These are admin-only operations
                .requestMatchers(
                    "/admin/dashboard",
                    "/admin/users/**",
                    "/admin/roles/**",
                    "/admin/clubs/**",
                    "/admin/audit-logs/**",
                    "/admin/reports/**"
                ).hasAuthority("ADMIN")
                
                // Club management endpoints - CLUB_HEAD and ADMIN
                // Note: Role hierarchy is enforced in RoleEnum.isHigherOrEqual()
                .requestMatchers(
                    "/club/**",
                    "/events/create/**",
                    "/events/manage/**"
                ).hasAnyAuthority("CLUB_HEAD", "ADMIN")
                
                // User endpoints - All authenticated users
                .requestMatchers(
                    "/user/**", 
                    "/dashboard", 
                    "/events/**", 
                    "/registrations/**", 
                    "/feedback/**", 
                    "/profile/**"
                ).authenticated()
                
                // All other endpoints require authentication by default
                .anyRequest().authenticated()
            )
            .anonymous(Customizer.withDefaults());

        return http.build();
    }

    /**
     * BCrypt password encoder bean for password encryption and verification
     * BCrypt is industry standard for secure password hashing with salt
     * Strength: 12 rounds of hashing (increased from default)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Security context repository for persisting authentication in HTTP session
     * This allows custom authentication (from AuthController) to be properly managed by Spring Security
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
