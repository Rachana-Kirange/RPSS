package com.eventra.eventra;

import com.eventra.eventra.enums.EventStatus;
import com.eventra.eventra.enums.PaymentStatus;
import com.eventra.eventra.enums.RoleEnum;
import com.eventra.eventra.enums.UserStatus;
import com.eventra.eventra.model.Club;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.Role;
import com.eventra.eventra.model.User;
import com.eventra.eventra.repository.ClubRepository;
import com.eventra.eventra.repository.EventRepository;
import com.eventra.eventra.repository.MediaRepository;
import com.eventra.eventra.repository.PassRepository;
import com.eventra.eventra.repository.RegistrationRepository;
import com.eventra.eventra.repository.RoleRepository;
import com.eventra.eventra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApplicationFlowIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private PassRepository passRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void registrationFormRendersForParticipant() throws Exception {
        User participant = createUser("participant-form@example.com", "Participant Form", "9000000001", RoleEnum.PARTICIPANT);
        Event event = createApprovedEvent(false);

        mockMvc.perform(get("/registrations/register/{eventId}", event.getEventId())
                .session(authenticatedSession(participant)))
            .andExpect(status().isOk())
            .andExpect(view().name("registration/register-form"));
    }

    @Test
    void paidRegistrationFlowCompletesPaymentAndGeneratesPass() throws Exception {
        User participant = createUser("participant-paid@example.com", "Participant Paid", "9000000002", RoleEnum.PARTICIPANT);
        Event event = createApprovedEvent(true);

        mockMvc.perform(post("/registrations/register/{eventId}", event.getEventId())
                .session(authenticatedSession(participant))
                .param("studentFullName", "Participant Paid")
                .param("section", "A1")
                .param("rollNumber", "42")
                .param("mobileNumber", "9000000002")
                .param("studentEmail", "participant-paid@example.com"))
            .andExpect(status().is3xxRedirection());

        var registration = registrationRepository.findByStudentUserId(participant.getUserId()).stream()
            .filter(item -> item.getEvent().getEventId().equals(event.getEventId()))
            .findFirst()
            .orElseThrow();

        mockMvc.perform(get("/payments/initiate/{registrationId}", registration.getRegistrationId())
                .session(authenticatedSession(participant)))
            .andExpect(status().isOk())
            .andExpect(view().name("payment/initiate"));

        mockMvc.perform(post("/payments/initiate/{registrationId}", registration.getRegistrationId())
                .session(authenticatedSession(participant))
                .param("transactionId", "TXN-123456"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/registrations/" + registration.getRegistrationId() + "/pass"));

        var updatedRegistration = registrationRepository.findById(registration.getRegistrationId()).orElseThrow();
        assertThat(updatedRegistration.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(passRepository.findByRegistrationRegistrationId(registration.getRegistrationId())).isPresent();
    }

    @Test
    void clubHeadDashboardRendersUsingExistingTemplate() throws Exception {
        User clubHead = createUser("clubhead-dashboard@example.com", "Club Head Dashboard", "9000000003", RoleEnum.CLUB_HEAD);
        createClub("Dashboard Club", clubHead);

        mockMvc.perform(get("/clubhead/dashboard").session(authenticatedSession(clubHead)))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard/clubhead-dashboard"));
    }

    @Test
    void eventMediaUploadPersistsFilesForClubHead() throws Exception {
        User clubHead = createUser("clubhead-media@example.com", "Club Head Media", "9000000004", RoleEnum.CLUB_HEAD);
        Event event = createApprovedEventForClubHead(clubHead, "Media Club", false);

        byte[] imageBytes = "fake-image-content".getBytes();
        org.springframework.mock.web.MockMultipartFile file =
            new org.springframework.mock.web.MockMultipartFile("files", "poster.png", "image/png", imageBytes);

        mockMvc.perform(multipart("/events/{eventId}/media/upload", event.getEventId())
                .file(file)
                .session(authenticatedSession(clubHead))
                .param("description", "Launch poster"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/events/" + event.getEventId() + "/media"));

        assertThat(mediaRepository.countByEventEventId(event.getEventId())).isEqualTo(1);
    }

    private User createUser(String email, String name, String phone, RoleEnum roleEnum) {
        Role role = roleRepository.findByRoleName(roleEnum).orElseThrow();

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setIsActive(true);
        user.setApprovalStatus(UserStatus.APPROVED);
        user.encryptPassword("Password@123");
        return userRepository.save(user);
    }

    private Club createClub(String baseName, User clubHead) {
        Club club = new Club();
        club.setClubName(baseName + " " + System.nanoTime());
        club.setDescription("Integration test club");
        club.setClubHead(clubHead);
        club.setIsActive(true);
        return clubRepository.save(club);
    }

    private Event createApprovedEvent(boolean requiresPayment) {
        User clubHead = createUser("clubhead-" + System.nanoTime() + "@example.com", "Club Head Owner", uniquePhone(), RoleEnum.CLUB_HEAD);
        return createApprovedEventForClubHead(clubHead, "Approved Event Club", requiresPayment);
    }

    private Event createApprovedEventForClubHead(User clubHead, String clubName, boolean requiresPayment) {
        Club club = createClub(clubName, clubHead);

        Event event = new Event();
        event.setTitle("Integration Event " + System.nanoTime());
        event.setDescription("Integration event description");
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setVenue("Main Hall");
        event.setMaxCapacity(100);
        event.setClub(club);
        event.setCreatedBy(clubHead);
        event.setStatus(EventStatus.APPROVED);
        event.setRequiresPayment(requiresPayment);
        event.setPaymentAmount(requiresPayment ? new BigDecimal("250.00") : null);
        event.setRequiresQR(true);
        event.setActivityProposal("Integration testing proposal");
        return eventRepository.save(event);
    }

    private MockHttpSession authenticatedSession(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getRole().getRoleName());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            List.of(new SimpleGrantedAuthority(user.getRole().getRoleName().name()))
        ));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    private String uniquePhone() {
        long suffix = System.nanoTime() % 1_000_000_0000L;
        return String.format("%010d", suffix);
    }
}
