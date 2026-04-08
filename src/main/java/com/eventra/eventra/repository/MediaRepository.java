package com.eventra.eventra.repository;

import com.eventra.eventra.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByEventEventId(Long eventId);

    List<Media> findByEventEventIdAndIsApprovedTrue(Long eventId);

    long countByEventEventId(Long eventId);
}
