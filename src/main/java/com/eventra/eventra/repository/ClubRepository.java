package com.eventra.eventra.repository;

import com.eventra.eventra.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

    Optional<Club> findByClubName(String clubName);

    List<Club> findByIsActiveTrue();

    List<Club> findAllByClubHeadUserId(Long userId);

    Optional<Club> findByClubHeadUserId(Long userId);

    boolean existsByClubName(String clubName);
}
