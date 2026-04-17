package com.eventra.eventra.repository;

import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByEventAndStudent(Event event, User student);

    List<Registration> findByEventEventId(Long eventId);

    List<Registration> findByStudentUserId(Long userId);

    long countByEventEventId(Long eventId);

    @Query("SELECT r FROM Registration r WHERE r.event.eventId = :eventId AND r.status = 'CONFIRMED'")
    List<Registration> findConfirmedRegistrationsByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.eventId = :eventId AND r.paymentStatus = 'COMPLETED'")
    long countPaidRegistrations(@Param("eventId") Long eventId);

    boolean existsByEventAndStudent(Event event, User student);

    long countByEvent(Event event);
}
