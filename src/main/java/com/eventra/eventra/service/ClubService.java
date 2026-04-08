package com.eventra.eventra.service;

import com.eventra.eventra.model.Club;
import com.eventra.eventra.model.User;
import com.eventra.eventra.repository.ClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class ClubService {

    private static final Logger log = Logger.getLogger(ClubService.class.getName());

    private final ClubRepository clubRepository;

    public ClubService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    /**
     * Create a new club (Admin only)
     */
    public Club createClub(String clubName, String description, User clubHead) {
        log.info(String.format("Creating new club: %s", clubName));

        if (clubRepository.existsByClubName(clubName)) {
            throw new IllegalArgumentException("Club with this name already exists");
        }

        Club club = new Club();
        club.setClubName(clubName);
        club.setDescription(description);
        club.setClubHead(clubHead);
        club.setIsActive(true);

        return clubRepository.save(club);
    }

    /**
     * Get all clubs
     */
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    /**
     * Get all active clubs
     */
    public List<Club> getAllActiveClubs() {
        return clubRepository.findByIsActiveTrue();
    }

    /**
     * Get club by ID
     */
    public Optional<Club> getClubById(Long clubId) {
        return clubRepository.findById(clubId);
    }

    /**
     * Get club by name
     */
    public Optional<Club> getClubByName(String clubName) {
        return clubRepository.findByClubName(clubName);
    }

    /**
     * Get club by club head
     */
    public Optional<Club> getClubByClubHead(Long userId) {
        return clubRepository.findByClubHeadUserId(userId);
    }

    /**
     * Update club details
     */
    public Club updateClub(Long clubId, String clubName, String description) {
        Club club = clubRepository.findById(clubId)
            .orElseThrow(() -> new RuntimeException("Club not found"));

        club.setClubName(clubName);
        club.setDescription(description);

        log.info(String.format("Club updated: %d", clubId));
        return clubRepository.save(club);
    }

    /**
     * Update club head
     */
    public Club updateClubHead(Long clubId, User newClubHead) {
        Club club = clubRepository.findById(clubId)
            .orElseThrow(() -> new RuntimeException("Club not found"));

        club.setClubHead(newClubHead);
        log.info(String.format("Club head updated for club: %d", clubId));
        return clubRepository.save(club);
    }

    /**
     * Deactivate club
     */
    public void deactivateClub(Long clubId) {
        Club club = clubRepository.findById(clubId)
            .orElseThrow(() -> new RuntimeException("Club not found"));

        club.setIsActive(false);
        clubRepository.save(club);
        log.warning(String.format("Club deactivated: %d", clubId));
    }

    /**
     * Activate club
     */
    public void activateClub(Long clubId) {
        Club club = clubRepository.findById(clubId)
            .orElseThrow(() -> new RuntimeException("Club not found"));

        club.setIsActive(true);
        clubRepository.save(club);
        log.info(String.format("Club activated: %d", clubId));
    }

    /**
     * Get all predefined clubs
     */
    public List<Club> getPredefinedClubs() {
        // These are the RPSS committee clubs specified
        String[] clubNames = {
            "Codeverse Technical Club",
            "Kalakruti Cultural Club",
            "Strikers Sports Club",
            "Innovation Club",
            "Digisphere Digital Marketing Club",
            "Samvedna Social Club",
            "Photography Club"
        };

        List<Club> clubs = clubRepository.findAll();
        return clubs.stream()
            .filter(club -> java.util.Arrays.asList(clubNames).contains(club.getClubName()))
            .toList();
    }
}
