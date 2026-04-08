# RPSS - Testing Guide (MVC with Thymeleaf - No REST API)

## 🎯 Testing Philosophy for MVC Applications

Since you're using **Thymeleaf MVC** (NOT REST API), testing is different:

| Aspect | REST API | Thymeleaf MVC |
|--------|----------|---------------|
| Response Format | JSON | HTML (rendered template) |
| Testing Tool | RestAssured, MockMvc (status/JSON) | MockMvc (HTML content, redirects) |
| Session Management | Token/Header | Http Session |
| Form Submission | POST JSON body | Form parameters |
| Validation | @Valid annotations → JSON errors | Form errors in model |
| Testing Focus | API endpoints | User flows & templates |

---

## 📊 Testing Strategy (3-Layer)

```
┌──────────────────────────────────────────────────────────┐
│  LAYER 3: END-TO-END TESTS (10%)                         │
│  - Test complete user workflows                          │
│  - Integration with Thymeleaf templates                  │
│  - Form submissions and page renders                     │
└──────────┬──────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  LAYER 2: INTEGRATION TESTS (30%)                        │
│  - Test Controller + Service + Repository                │
│  - Verify database interactions                          │
│  - Test template rendering                              │
└──────────┬──────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  LAYER 1: UNIT TESTS (60%)                               │
│  - Test services in isolation                            │
│  - Mock repositories                                     │
│  - Pure business logic                                   │
└──────────────────────────────────────────────────────────┘
```

---

## ✅ Test Setup

### Add to pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### Create application-test.properties
```
# File: src/test/resources/application-test.properties

# Use H2 in-memory database for testing
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# Hibernate auto-creates schema for tests
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

---

## 1️⃣ LAYER 1: UNIT TESTS (60%)

### Test 1: User Service Tests

```java
// File: src/test/java/com/eventra/eventra/service/UserServiceTest.java

package com.eventra.eventra.service;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.Role;
import com.eventra.eventra.repository.UserRepository;
import com.eventra.eventra.repository.RoleRepository;
import com.eventra.eventra.enums.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role participantRole;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        participantRole = Role.builder()
            .roleId(1L)
            .roleName(RoleEnum.PARTICIPANT)
            .build();

        testUser = User.builder()
            .userId(1L)
            .name("John Doe")
            .email("john@example.com")
            .password("password123")
            .phone("9876543210")
            .role(participantRole)
            .isActive(true)
            .build();
    }

    // TEST 1: Register new user as participant
    @Test
    void testRegisterUserAsParticipant() {
        // Arrange
        when(roleRepository.findByRoleName(RoleEnum.PARTICIPANT))
            .thenReturn(participantRole);
        when(userRepository.save(any(User.class)))
            .thenReturn(testUser);

        // Act
        User registeredUser = userService.registerUser(testUser, RoleEnum.PARTICIPANT);

        // Assert
        assertNotNull(registeredUser);
        assertEquals("John Doe", registeredUser.getName());
        assertEquals(RoleEnum.PARTICIPANT, registeredUser.getRole().getRoleName());
        verify(userRepository, times(1)).save(any());
    }

    // TEST 2: Login with correct password
    @Test
    void testLoginWithCorrectPassword() {
        // Arrange
        testUser.encryptPassword("password123");
        when(userRepository.findByEmail("john@example.com"))
            .thenReturn(java.util.Optional.of(testUser));

        // Act
        User loggedInUser = userService.login("john@example.com", "password123");

        // Assert
        assertNotNull(loggedInUser);
        assertEquals("John Doe", loggedInUser.getName());
    }

    // TEST 3: Login with incorrect password
    @Test
    void testLoginWithIncorrectPassword() {
        // Arrange
        testUser.encryptPassword("correctPassword123");
        when(userRepository.findByEmail("john@example.com"))
            .thenReturn(java.util.Optional.of(testUser));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.login("john@example.com", "wrongPassword");
        });
    }

    // TEST 4: Login with non-existent email
    @Test
    void testLoginWithNonExistentEmail() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com"))
            .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userService.login("nonexistent@example.com", "password123");
        });
    }

    // TEST 5: Update user profile
    @Test
    void testUpdateUserProfile() {
        // Arrange
        User updatedData = User.builder()
            .name("Jane Doe")
            .phone("9876543211")
            .build();
        when(userRepository.save(any(User.class)))
            .thenReturn(updatedData);

        // Act
        User result = userService.updateProfile(testUser, updatedData);

        // Assert
        assertEquals("Jane Doe", result.getName());
        verify(userRepository, times(1)).save(any());
    }

    // TEST 6: Deactivate user
    @Test
    void testDeactivateUser() {
        // Arrange
        testUser.setIsActive(true);
        when(userRepository.save(any(User.class)))
            .thenReturn(testUser);

        // Act
        userService.deactivateUser(testUser);

        // Assert
        assertFalse(testUser.getIsActive());
        verify(userRepository, times(1)).save(any());
    }
}
```

### Test 2: Event Service Tests

```java
// File: src/test/java/com/eventra/eventra/service/EventServiceTest.java

package com.eventra.eventra.service;

import com.eventra.eventra.model.*;
import com.eventra.eventra.enums.*;
import com.eventra.eventra.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private User clubHead;
    private Club testClub;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        clubHead = User.builder()
            .userId(1L)
            .name("Club Head")
            .email("clubhead@example.com")
            .role(Role.builder().roleName(RoleEnum.CLUB_HEAD).build())
            .build();

        testClub = Club.builder()
            .clubId(1L)
            .clubName("Codeverse Technical Club")
            .clubHead(clubHead)
            .build();

        testEvent = Event.builder()
            .eventId(1L)
            .title("Tech Fest 2026")
            .description("Annual tech festival")
            .eventDate(LocalDateTime.now().plusDays(10))
            .venue("Main Hall")
            .maxCapacity(100)
            .club(testClub)
            .createdBy(clubHead)
            .requiresPayment(false)
            .status(EventStatus.PENDING)
            .build();
    }

    // TEST 1: Create free event
    @Test
    void testCreateFreeEvent() {
        // Arrange
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        Event createdEvent = eventService.createEvent(testEvent);

        // Assert
        assertNotNull(createdEvent);
        assertEquals("Tech Fest 2026", createdEvent.getTitle());
        assertFalse(createdEvent.getRequiresPayment());
        assertNull(createdEvent.getPaymentAmount());
        assertEquals(EventStatus.PENDING, createdEvent.getStatus());
        verify(eventRepository, times(1)).save(any());
    }

    // TEST 2: Create paid event
    @Test
    void testCreatePaidEvent() {
        // Arrange
        Event paidEvent = Event.builder()
            .title("Paid Workshop")
            .requiresPayment(true)
            .paymentAmount(new BigDecimal("500"))
            .maxCapacity(50)
            .club(testClub)
            .createdBy(clubHead)
            .build();

        when(eventRepository.save(any(Event.class))).thenReturn(paidEvent);

        // Act
        Event createdEvent = eventService.createEvent(paidEvent);

        // Assert
        assertTrue(createdEvent.getRequiresPayment());
        assertEquals(new BigDecimal("500"), createdEvent.getPaymentAmount());
        verify(eventRepository, times(1)).save(any());
    }

    // TEST 3: Approve event
    @Test
    void testApproveEvent() {
        // Arrange
        User admin = User.builder()
            .userId(2L)
            .name("Admin")
            .role(Role.builder().roleName(RoleEnum.ADMIN).build())
            .build();

        when(eventRepository.findById(1L)).thenReturn(java.util.Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        eventService.approveEvent(1L, admin);

        // Assert
        assertEquals(EventStatus.APPROVED, testEvent.getStatus());
        assertEquals(admin, testEvent.getApprovedBy());
        assertNotNull(testEvent.getApprovalDate());
        verify(eventRepository, times(1)).save(any());
    }

    // TEST 4: Reject event with reason
    @Test
    void testRejectEvent() {
        // Arrange
        User admin = User.builder()
            .userId(2L)
            .name("Admin")
            .role(Role.builder().roleName(RoleEnum.ADMIN).build())
            .build();
        String rejectionReason = "Insufficient details provided";

        when(eventRepository.findById(1L)).thenReturn(java.util.Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        eventService.rejectEvent(1L, admin, rejectionReason);

        // Assert
        assertEquals(EventStatus.REJECTED, testEvent.getStatus());
        assertEquals(rejectionReason, testEvent.getRejectionReason());
        verify(eventRepository, times(1)).save(any());
    }

    // TEST 5: Check event capacity
    @Test
    void testCheckEventCapacity() {
        // Assert
        assertTrue(testEvent.isSpaceAvailable());
        assertEquals(100, testEvent.getAvailableSeats());
    }

    // TEST 6: Check available seats
    @Test
    void testAvailableSeatsCalculation() {
        // Create 50 registrations
        Registration reg = Registration.builder()
            .event(testEvent)
            .status(RegistrationStatus.CONFIRMED)
            .build();

        // Mock registrations count
        assertNotNull(testEvent.getAvailableSeats());
        assertTrue(testEvent.getAvailableSeats() > 0);
    }

    // TEST 7: Find upcoming events
    @Test
    void testFindUpcomingEvents() {
        // Arrange
        when(eventRepository.findUpcomingEvents())
            .thenReturn(java.util.List.of(testEvent));

        // Act
        var events = eventService.getUpcomingEvents();

        // Assert
        assertFalse(events.isEmpty());
        assertEquals(1, events.size());
    }

    // TEST 8: Find pending approval events
    @Test
    void testFindPendingEvents() {
        // Arrange
        when(eventRepository.findPendingEvents())
            .thenReturn(java.util.List.of(testEvent));

        // Act
        var events = eventService.getPendingEvents();

        // Assert
        assertEquals(1, events.size());
        assertEquals(EventStatus.PENDING, events.get(0).getStatus());
    }
}
```

### Test 3: Registration & Payment Service Tests

```java
// File: src/test/java/com/eventra/eventra/service/RegistrationServiceTest.java

package com.eventra.eventra.service;

import com.eventra.eventra.model.*;
import com.eventra.eventra.enums.*;
import com.eventra.eventra.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PassRepository passRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private User participant;
    private Event freeEvent;
    private Event paidEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        participant = User.builder()
            .userId(3L)
            .name("Participant")
            .email("participant@example.com")
            .role(Role.builder().roleName(RoleEnum.PARTICIPANT).build())
            .build();

        freeEvent = Event.builder()
            .eventId(1L)
            .title("Free Event")
            .requiresPayment(false)
            .maxCapacity(50)
            .status(EventStatus.APPROVED)
            .build();

        paidEvent = Event.builder()
            .eventId(2L)
            .title("Paid Event")
            .requiresPayment(true)
            .paymentAmount(new BigDecimal("500"))
            .maxCapacity(50)
            .status(EventStatus.APPROVED)
            .build();
    }

    // TEST 1: Register for free event
    @Test
    void testRegisterForFreeEvent() {
        // Arrange
        Registration registration = Registration.builder()
            .event(freeEvent)
            .participant(participant)
            .status(RegistrationStatus.CONFIRMED)
            .paymentStatus(PaymentStatus.NOT_REQUIRED)
            .build();

        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // Act
        Registration result = registrationService.registerForEvent(participant, freeEvent);

        // Assert
        assertNotNull(result);
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertEquals(PaymentStatus.NOT_REQUIRED, result.getPaymentStatus());
        verify(registrationRepository, times(1)).save(any());
    }

    // TEST 2: Register for paid event
    @Test
    void testRegisterForPaidEvent() {
        // Arrange
        Registration registration = Registration.builder()
            .event(paidEvent)
            .participant(participant)
            .status(RegistrationStatus.CONFIRMED)
            .paymentStatus(PaymentStatus.PENDING)
            .build();

        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // Act
        Registration result = registrationService.registerForEvent(participant, paidEvent);

        // Assert
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        assertNull(result.getTransactionId());
    }

    // TEST 3: Complete payment
    @Test
    void testCompletePayment() {
        // Arrange
        Registration registration = Registration.builder()
            .registrationId(1L)
            .event(paidEvent)
            .participant(participant)
            .paymentStatus(PaymentStatus.PENDING)
            .build();

        String transactionId = "TXN123456";

        when(registrationRepository.findById(1L))
            .thenReturn(java.util.Optional.of(registration));
        when(registrationRepository.save(any(Registration.class)))
            .thenReturn(registration);

        // Act
        registrationService.completePayment(1L, transactionId);

        // Assert
        assertEquals(PaymentStatus.COMPLETED, registration.getPaymentStatus());
        assertEquals(transactionId, registration.getTransactionId());
        assertNotNull(registration.getPaymentDate());
    }

    // TEST 4: Prevent duplicate registration
    @Test
    void testPreventDuplicateRegistration() {
        // Arrange
        when(registrationRepository.existsByEventAndParticipant(freeEvent, participant))
            .thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            registrationService.registerForEvent(participant, freeEvent);
        });
    }

    // TEST 5: Check event capacity before registration
    @Test
    void testCheckEventCapacityBeforeRegistration() {
        // Arrange
        Event fullEvent = Event.builder()
            .eventId(3L)
            .maxCapacity(0)  // No capacity
            .status(EventStatus.APPROVED)
            .build();

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            registrationService.registerForEvent(participant, fullEvent);
        }, "Event is full");
    }

    // TEST 6: Cancel registration
    @Test
    void testCancelRegistration() {
        // Arrange
        Registration registration = Registration.builder()
            .registrationId(1L)
            .status(RegistrationStatus.CONFIRMED)
            .build();

        when(registrationRepository.findById(1L))
            .thenReturn(java.util.Optional.of(registration));
        when(registrationRepository.save(any(Registration.class)))
            .thenReturn(registration);

        // Act
        registrationService.cancelRegistration(1L);

        // Assert
        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
    }
}
```

---

## 2️⃣ LAYER 2: INTEGRATION TESTS (30%)

### Integration Test 1: User Registration & Login Flow

```java
// File: src/test/java/com/eventra/eventra/controller/UserControllerIT.java

package com.eventra.eventra.controller;

import com.eventra.eventra.model.User;
import com.eventra.eventra.model.Role;
import com.eventra.eventra.repository.UserRepository;
import com.eventra.eventra.repository.RoleRepository;
import com.eventra.eventra.enums.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role participantRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        participantRole = Role.builder()
            .roleName(RoleEnum.PARTICIPANT)
            .build();
        roleRepository.save(participantRole);
    }

    // TEST 1: Navigate to registration page
    @Test
    void testGetRegistrationPage() throws Exception {
        mockMvc.perform(get("/auth/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(model().attributeExists("roles"));
    }

    // TEST 2: Register user as participant
    @Test
    void testRegisterUserAsParticipant() throws Exception {
        mockMvc.perform(post("/auth/register")
                .param("name", "John Doe")
                .param("email", "john@example.com")
                .param("password", "password123")
                .param("phone", "9876543210")
                .param("role", "PARTICIPANT"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login"));

        // Verify user saved to database
        User savedUser = userRepository.findByEmail("john@example.com").orElse(null);
        assert savedUser != null;
        assert savedUser.getName().equals("John Doe");
    }

    // TEST 3: Register with duplicate email
    @Test
    void testRegisterWithDuplicateEmail() throws Exception {
        // First registration
        User existingUser = User.builder()
            .name("Existing")
            .email("existing@example.com")
            .password("password")
            .role(participantRole)
            .build();
        userRepository.save(existingUser);

        // Try to register with same email
        mockMvc.perform(post("/auth/register")
                .param("name", "Different Name")
                .param("email", "existing@example.com")
                .param("password", "password123")
                .param("role", "PARTICIPANT"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(model().attributeHasFieldErrors("user", "email"));
    }

    // TEST 4: Login with valid credentials
    @Test
    void testLoginWithValidCredentials() throws Exception {
        // Create user
        User user = User.builder()
            .name("John Doe")
            .email("john@example.com")
            .phone("9876543210")
            .role(participantRole)
            .build();
        user.encryptPassword("password123");
        userRepository.save(user);

        // Login
        mockMvc.perform(post("/auth/login")
                .param("email", "john@example.com")
                .param("password", "password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard/participant"));
    }

    // TEST 5: Login with invalid credentials
    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                .param("email", "nonexistent@example.com")
                .param("password", "wrongpassword"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            .andExpect(model().attributeExists("error"));
    }
}
```

### Integration Test 2: Event Approval Workflow

```java
// File: src/test/java/com/eventra/eventra/controller/EventControllerIT.java

package com.eventra.eventra.controller;

import com.eventra.eventra.model.*;
import com.eventra.eventra.repository.*;
import com.eventra.eventra.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User clubHead;
    private User admin;
    private Club club;
    private MockHttpSession clubHeadSession;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();
        clubRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role clubHeadRole = Role.builder()
            .roleName(RoleEnum.CLUB_HEAD)
            .build();
        Role adminRole = Role.builder()
            .roleName(RoleEnum.ADMIN)
            .build();
        roleRepository.save(clubHeadRole);
        roleRepository.save(adminRole);

        // Create users
        clubHead = User.builder()
            .name("Club Head")
            .email("clubhead@example.com")
            .phone("9876543210")
            .role(clubHeadRole)
            .build();
        clubHead.encryptPassword("password123");
        userRepository.save(clubHead);

        admin = User.builder()
            .name("Admin")
            .email("admin@example.com")
            .phone("9876543211")
            .role(adminRole)
            .build();
        admin.encryptPassword("password123");
        userRepository.save(admin);

        // Create club
        club = Club.builder()
            .clubName("Codeverse Technical Club")
            .clubHead(clubHead)
            .build();
        clubRepository.save(club);

        // Create sessions
        clubHeadSession = new MockHttpSession();
        clubHeadSession.setAttribute("user", clubHead);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("user", admin);
    }

    // TEST 1: Club head creates event
    @Test
    void testClubHeadCreatesEvent() throws Exception {
        mockMvc.perform(post("/event/create")
                .session(clubHeadSession)
                .param("title", "Tech Fest 2026")
                .param("description", "Annual tech festival")
                .param("eventDate", LocalDateTime.now().plusDays(10).toString())
                .param("venue", "Main Hall")
                .param("maxCapacity", "100")
                .param("requiresPayment", "false"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard/club-head"));

        // Verify event created with PENDING status
        Event event = eventRepository.findByTitle("Tech Fest 2026");
        assert event != null;
        assert event.getStatus() == EventStatus.PENDING;
        assert event.getCreatedBy().equals(clubHead);
    }

    // TEST 2: Admin sees pending events
    @Test
    void testAdminViewsPendingEvents() throws Exception {
        // Create pending event
        Event event = Event.builder()
            .title("Pending Event")
            .description("Test")
            .eventDate(LocalDateTime.now().plusDays(5))
            .venue("Hall")
            .maxCapacity(50)
            .club(club)
            .createdBy(clubHead)
            .status(EventStatus.PENDING)
            .build();
        eventRepository.save(event);

        mockMvc.perform(get("/admin/events/pending")
                .session(adminSession))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/pending-events"))
            .andExpect(model().attributeExists("events"));
    }

    // TEST 3: Admin approves event
    @Test
    void testAdminApprovesEvent() throws Exception {
        // Create pending event
        Event event = Event.builder()
            .title("Event to Approve")
            .description("Test")
            .eventDate(LocalDateTime.now().plusDays(5))
            .venue("Hall")
            .maxCapacity(50)
            .club(club)
            .createdBy(clubHead)
            .status(EventStatus.PENDING)
            .build();
        eventRepository.save(event);

        mockMvc.perform(post("/event/{eventId}/approve", event.getEventId())
                .session(adminSession)
                .param("approved", "true"))
            .andExpect(status().is3xxRedirection());

        // Verify event approved
        Event updated = eventRepository.findById(event.getEventId()).get();
        assert updated.getStatus() == EventStatus.APPROVED;
        assert updated.getApprovedBy().equals(admin);
    }

    // TEST 4: Admin rejects event with reason
    @Test
    void testAdminRejectsEvent() throws Exception {
        Event event = Event.builder()
            .title("Event to Reject")
            .description("Test")
            .eventDate(LocalDateTime.now().plusDays(5))
            .venue("Hall")
            .maxCapacity(50)
            .club(club)
            .createdBy(clubHead)
            .status(EventStatus.PENDING)
            .build();
        eventRepository.save(event);

        mockMvc.perform(post("/event/{eventId}/reject", event.getEventId())
                .session(adminSession)
                .param("rejectionReason", "Insufficient details"))
            .andExpect(status().is3xxRedirection());

        Event updated = eventRepository.findById(event.getEventId()).get();
        assert updated.getStatus() == EventStatus.REJECTED;
        assert updated.getRejectionReason().equals("Insufficient details");
    }

    // TEST 5: Club head creates paid event
    @Test
    void testClubHeadCreatesPaidEvent() throws Exception {
        mockMvc.perform(post("/event/create")
                .session(clubHeadSession)
                .param("title", "Paid Workshop")
                .param("description", "Workshop with fee")
                .param("eventDate", LocalDateTime.now().plusDays(15).toString())
                .param("venue", "Conference Room")
                .param("maxCapacity", "30")
                .param("requiresPayment", "true")
                .param("paymentAmount", "500"))
            .andExpect(status().is3xxRedirection());

        Event paidEvent = eventRepository.findByTitle("Paid Workshop");
        assert paidEvent != null;
        assert paidEvent.getRequiresPayment() == true;
        assert paidEvent.getPaymentAmount().doubleValue() == 500.0;
    }
}
```

---

## 3️⃣ LAYER 3: END-TO-END TESTS (10%)

### Complete Event Registration Workflow Test

```java
// File: src/test/java/com/eventra/eventra/E2EEventWorkflowTest.java

package com.eventra.eventra;

import com.eventra.eventra.model.*;
import com.eventra.eventra.repository.*;
import com.eventra.eventra.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class E2EEventWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    private User clubHead;
    private User admin;
    private User participant;
    private Club club;

    @BeforeEach
    void setUp() {
        // Clear all data
        feedbackRepository.deleteAll();
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        clubRepository.deleteAll();
        roleRepository.deleteAll();

        // Setup roles
        Role clubHeadRole = Role.builder().roleName(RoleEnum.CLUB_HEAD).build();
        Role adminRole = Role.builder().roleName(RoleEnum.ADMIN).build();
        Role participantRole = Role.builder().roleName(RoleEnum.PARTICIPANT).build();
        roleRepository.save(clubHeadRole);
        roleRepository.save(adminRole);
        roleRepository.save(participantRole);

        // Setup users
        clubHead = User.builder()
            .name("Club Head")
            .email("clubhead@example.com")
            .role(clubHeadRole)
            .build();
        clubHead.encryptPassword("password123");
        userRepository.save(clubHead);

        admin = User.builder()
            .name("Admin")
            .email("admin@example.com")
            .role(adminRole)
            .build();
        admin.encryptPassword("password123");
        userRepository.save(admin);

        participant = User.builder()
            .name("Participant")
            .email("participant@example.com")
            .role(participantRole)
            .build();
        participant.encryptPassword("password123");
        userRepository.save(participant);

        // Setup club
        club = Club.builder()
            .clubName("Codeverse Technical Club")
            .clubHead(clubHead)
            .build();
        clubRepository.save(club);
    }

    // E2E WORKFLOW: Event Creation → Approval → Registration → Feedback
    @Test
    void testCompleteEventLifecycle() throws Exception {
        MockHttpSession clubHeadSession = createSession(clubHead);
        MockHttpSession adminSession = createSession(admin);
        MockHttpSession participantSession = createSession(participant);

        // STEP 1: Club head creates event
        mockMvc.perform(post("/event/create")
                .session(clubHeadSession)
                .param("title", "RPSS Hackathon 2026")
                .param("description", "48-hour hackathon")
                .param("eventDate", LocalDateTime.now().plusDays(20).toString())
                .param("venue", "Tech Building")
                .param("maxCapacity", "100")
                .param("requiresPayment", "false"))
            .andExpect(status().is3xxRedirection());

        Event event = eventRepository.findByTitle("RPSS Hackathon 2026");
        assert event != null;
        assert event.getStatus() == EventStatus.PENDING;

        // STEP 2: Admin approves event
        mockMvc.perform(post("/event/{eventId}/approve", event.getEventId())
                .session(adminSession))
            .andExpect(status().is3xxRedirection());

        event = eventRepository.findById(event.getEventId()).get();
        assert event.getStatus() == EventStatus.APPROVED;

        // STEP 3: Participant registers
        mockMvc.perform(post("/registration/register/{eventId}", event.getEventId())
                .session(participantSession))
            .andExpect(status().is3xxRedirection());

        Registration registration = registrationRepository
            .findByEventAndParticipant(event, participant).orElse(null);
        assert registration != null;
        assert registration.getStatus() == RegistrationStatus.CONFIRMED;

        // STEP 4: Participant submits feedback (after event)
        mockMvc.perform(post("/feedback/submit/{eventId}", event.getEventId())
                .session(participantSession)
                .param("rating", "5")
                .param("comment", "Excellent hackathon!"))
            .andExpect(status().is3xxRedirection());

        Feedback feedback = feedbackRepository
            .findByEventAndParticipant(event, participant).orElse(null);
        assert feedback != null;
        assert feedback.getRating() == 5;
    }

    // Helper method to create session
    private MockHttpSession createSession(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }
}
```

---

## 🚀 Running Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#testRegisterUserAsParticipant

# Run integration tests only
mvn test -Dgroups=integration

# Generate test report
mvn surefire-report:report
```

---

## 📊 Test Report Generation

After running tests, generate HTML reports:

```bash
# Generate Surefire Report (Test Results)
mvn surefire-report:report

# Generate JaCoCo Report (Code Coverage)
mvn jacoco:report

# Open reports
open target/site/surefire-report.html
open target/site/jacoco/index.html
```

---

## ✅ Testing Checklist for Your Project

- [ ] **User Service Tests** - Registration, Login, Profile Update
- [ ] **Event Service Tests** - Event Creation, Approval, Rejection
- [ ] **Registration Service Tests** - Free/Paid Event Registration
- [ ] **Payment Service Tests** - Payment Completion, Refunds
- [ ] **Feedback Service Tests** - Loading feedback, ratings
- [ ] **Controller Integration Tests** - Form submissions, redirects
- [ ] **Template Rendering Tests** - Verify HTML content
- [ ] **Database Integration Tests** - Repository operations
- [ ] **E2E Workflow Tests** - Complete user flows
- [ ] **Form Validation Tests** - Invalid inputs, error messages

---

## 🎯 Coverage Goals

- **Unit Tests**: 80%+ code coverage for services
- **Integration Tests**: Cover all happy path workflows
- **E2E Tests**: At least 3 major user workflows

