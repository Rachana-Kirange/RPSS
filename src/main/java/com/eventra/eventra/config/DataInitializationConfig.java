package com.eventra.eventra.config;

import com.eventra.eventra.model.Role;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.repository.RoleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.logging.Logger;

/**
 * Initialize default roles in the database on application startup
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
                clubHead.setPermissions("{\"create_event\": true, \"manage_participants\": true, \"upload_media\": true, \"submit_report\": true}");
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
}
