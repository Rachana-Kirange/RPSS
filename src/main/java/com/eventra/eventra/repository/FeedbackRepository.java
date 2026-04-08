package com.eventra.eventra.repository;

import com.eventra.eventra.model.Feedback;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByEventAndParticipant(Event event, User participant);

    List<Feedback> findByEventEventId(Long eventId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.event.eventId = :eventId")
    Double getAverageRatingByEvent(@Param("eventId") Long eventId);

    long countByEventEventId(Long eventId);
}
