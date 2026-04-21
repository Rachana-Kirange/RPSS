package com.eventra.eventra.config;

import com.eventra.eventra.model.Role;
import com.eventra.eventra.model.Club;
import com.eventra.eventra.model.User;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.enums.UserStatus;
import com.eventra.eventra.repository.RoleRepository;
import com.eventra.eventra.repository.ClubRepository;
import com.eventra.eventra.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.logging.Logger;

/**
 * Initialize default roles and clubs in the database on application startup
 */
@Configuration
public class DataInitializationConfig implements WebMvcConfigurer {

    private static final Logger log = Logger.getLogger(DataInitializationConfig.class.getName());

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToRoleEnumConverter());
    }

    @Bean
    public ApplicationRunner initializeRoles(RoleRepository roleRepository) {
        return args -> {
            log.info("Checking and initializing default roles...");

            // Check and insert PARTICIPANT role
            if (roleRepository.findByRoleName(RoleEnum.PARTICIPANT).isEmpty()) {
                Role participant = new Role();
                participant.setRoleName(RoleEnum.PARTICIPANT);
                participant.setDescription("Participant - Can register for events and give feedback");
                participant.setPermissions("{\"view_events\": true, \"register_event\": true, \"download_pass\": true, \"feedback\": true}");
                roleRepository.save(participant);
                log.info("Created PARTICIPANT role");
            }

            // Check and insert CLUB_HEAD role
            if (roleRepository.findByRoleName(RoleEnum.CLUB_HEAD).isEmpty()) {
                Role clubHead = new Role();
                clubHead.setRoleName(RoleEnum.CLUB_HEAD);
                clubHead.setDescription("Club Head - Can create and manage events");
                clubHead.setPermissions("{\"create_event\": true, \"manage_students\": true, \"upload_media\": true, \"submit_report\": true}");
                roleRepository.save(clubHead);
                log.info("Created CLUB_HEAD role");
            }

            // Check and insert ADMIN role
            if (roleRepository.findByRoleName(RoleEnum.ADMIN).isEmpty()) {
                Role admin = new Role();
                admin.setRoleName(RoleEnum.ADMIN);
                admin.setDescription("Admin - System administrator with full access");
                admin.setPermissions("{\"approve_events\": true, \"manage_users\": true, \"manage_clubs\": true, \"generate_reports\": true, \"view_analytics\": true}");
                roleRepository.save(admin);
                log.info("Created ADMIN role");
            }

            log.info("Role initialization completed");
        };
    }

    @Bean
    public ApplicationRunner initializeClubs(ClubRepository clubRepository) {
        return args -> {
            log.info("Checking and initializing predefined clubs...");

            String[] predefinedClubNames = {
                "Codeverse Technical Club",
                "Kalakruti Cultural Club",
                "Strikers Sports Club",
                "Innovation Club",
                "Digisphere Digital Marketing Club",
                "Samvedna Social Club",
                "Photography Club"
            };

            String[] clubDescriptions = {
                "Technical club focused on programming, web development, and software engineering",
                "Cultural club dedicated to arts, music, dance, and cultural activities",
                "Sports club for athletic activities and sports events",
                "Club for innovation projects and entrepreneurship",
                "Digital marketing club focused on social media, content marketing, and digital strategies",
                "Social club focused on community service and social awareness",
                "Photography club for photography enthusiasts and visual artists"
            };

            for (int i = 0; i < predefinedClubNames.length; i++) {
                String clubName = predefinedClubNames[i];
                if (clubRepository.findByClubName(clubName).isEmpty()) {
                    Club club = new Club();
                    club.setClubName(clubName);
                    club.setDescription(clubDescriptions[i]);
                    club.setIsActive(true);
                    clubRepository.save(club);
                    log.info(String.format("Created club: %s", clubName));
                }
            }

            log.info("Club initialization completed");
        };
    }

    @Bean
    public ApplicationRunner initializeDefaultAdmin(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            final String adminEmail = "mca25.rachanakirange@asmedu.org";

            if (userRepository.findByEmail(adminEmail).isPresent()) {
                log.info("Default admin already exists");
                return;
            }

            Role adminRole = roleRepository.findByRoleName(RoleEnum.ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName(RoleEnum.ADMIN);
                    role.setDescription("Admin - System administrator with full access");
                    role.setPermissions("{\"approve_events\": true, \"manage_users\": true, \"manage_clubs\": true, \"generate_reports\": true, \"view_analytics\": true}");
                    log.info("ADMIN role was missing during admin initialization, creating it now");
                    return roleRepository.save(role);
                });

            User adminUser = new User();
            adminUser.setName("Rachana Kirange");
            adminUser.setEmail(adminEmail);
            adminUser.setRole(adminRole);
            adminUser.setIsActive(true);
            adminUser.setApprovalStatus(UserStatus.APPROVED);
            adminUser.encryptPassword("Rachana@2907k");

            User savedAdmin = userRepository.save(adminUser);
            savedAdmin.setApprovedBy(savedAdmin);
            savedAdmin.setLastRoleChangedBy(savedAdmin);
            userRepository.save(savedAdmin);

            log.info("Created default admin user: " + adminEmail);
        };
    }
}
