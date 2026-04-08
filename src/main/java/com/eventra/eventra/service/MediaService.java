package com.eventra.eventra.service;

import com.eventra.eventra.model.Media;
import com.eventra.eventra.model.Event;
import com.eventra.eventra.model.User;
import com.eventra.eventra.enums.MediaFileType;
import com.eventra.eventra.repository.MediaRepository;
import com.eventra.eventra.repository.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Transactional
public class MediaService {

    private static final Logger log = Logger.getLogger(MediaService.class.getName());

    private final MediaRepository mediaRepository;
    private final EventRepository eventRepository;

    public MediaService(MediaRepository mediaRepository, EventRepository eventRepository) {
        this.mediaRepository = mediaRepository;
        this.eventRepository = eventRepository;
    }

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Upload media file
     */
    public Media uploadMedia(Long eventId, MultipartFile file, MediaFileType fileType,
                            String description, User uploadedBy) {
        log.info(String.format("Uploading media for event: %d", eventId));

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

        try {
            String fileName = saveFile(file, fileType);

            Media media = new Media();
            media.setEvent(event);
            media.setUploadedBy(uploadedBy);
            media.setFileName(file.getOriginalFilename());
            media.setFilePath("/uploads/media/" + fileName);
            media.setFileType(fileType);
            media.setFileSize(file.getSize());
            media.setDescription(description);
            media.setIsApproved(true);

            log.info(String.format("Media uploaded successfully: %s", fileName));
            return mediaRepository.save(media);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error uploading media", e);
            throw new RuntimeException("Error uploading file");
        }
    }

    /**
     * Save file to disk
     */
    private String saveFile(MultipartFile file, MediaFileType fileType) throws IOException {
        String uploadPath = uploadDir + "/media";
        Files.createDirectories(Paths.get(uploadPath));

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadPath, fileName);

        Files.write(filePath, file.getBytes());
        return fileName;
    }

    /**
     * Get all media for event
     */
    public List<Media> getEventMedia(Long eventId) {
        return mediaRepository.findByEventEventId(eventId);
    }

    /**
     * Get approved media for event
     */
    public List<Media> getApprovedEventMedia(Long eventId) {
        return mediaRepository.findByEventEventIdAndIsApprovedTrue(eventId);
    }

    /**
     * Get media by ID
     */
    public Optional<Media> getMediaById(Long mediaId) {
        return mediaRepository.findById(mediaId);
    }

    /**
     * Approve media
     */
    public Media approveMedia(Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media not found"));

        media.setIsApproved(true);
        log.info(String.format("Media approved: %d", mediaId));
        return mediaRepository.save(media);
    }

    /**
     * Reject media
     */
    public void rejectMedia(Long mediaId) {
        mediaRepository.deleteById(mediaId);
        log.info(String.format("Media rejected and deleted: %d", mediaId));
    }

    /**
     * Get media count for event
     */
    public long getMediaCount(Long eventId) {
        return mediaRepository.countByEventEventId(eventId);
    }

    /**
     * Delete media
     */
    public void deleteMedia(Long mediaId) {
        mediaRepository.deleteById(mediaId);
        log.info(String.format("Media deleted: %d", mediaId));
    }

    /**
     * Get image gallery for event
     */
    public List<Media> getImageGallery(Long eventId) {
        return mediaRepository.findByEventEventIdAndIsApprovedTrue(eventId)
            .stream()
            .filter(Media::isImage)
            .toList();
    }

    /**
     * Get video list for event
     */
    public List<Media> getVideoList(Long eventId) {
        return mediaRepository.findByEventEventIdAndIsApprovedTrue(eventId)
            .stream()
            .filter(Media::isVideo)
            .toList();
    }
}
