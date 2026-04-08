package com.eventra.eventra.repository;

import com.eventra.eventra.model.Event;
import com.eventra.eventra.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(EventStatus status);

    List<Event> findByClubClubId(Long clubId);

    List<Event> findByCreatedByUserId(Long userId);

    @Query("SELECT e FROM Event e WHERE e.status = 'APPROVED' " +
           "AND e.eventDate > CURRENT_TIMESTAMP ORDER BY e.eventDate ASC")
    List<Event> findUpcomingApprovedEvents();

    @Query("SELECT e FROM Event e WHERE e.status = 'PENDING' ORDER BY e.createdDate DESC")
    List<Event> findPendingEvents();

    long countByStatus(EventStatus status);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status AND e.club.clubId = :clubId")
    long countByStatusAndClub(@Param("status") EventStatus status, @Param("clubId") Long clubId);
}
