package com.eventra.eventra.service;

import com.eventra.eventra.model.Pass;
import com.eventra.eventra.model.Registration;
import com.eventra.eventra.model.User;
import com.eventra.eventra.repository.PassRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Transactional
public class PassService {

    private static final Logger log = Logger.getLogger(PassService.class.getName());

    private final PassRepository passRepository;

    public PassService(PassRepository passRepository) {
        this.passRepository = passRepository;
    }

    @Value("${app.upload.dir}")
    private String uploadDir;

    // ===================== GENERATE PASS =====================
    public Pass generateQRPass(Registration registration) {

        log.info(() -> "Generating QR pass for registration: " + registration.getRegistrationId());

        String qrData = generateQRData(registration);
        String fileName = generateQRImage(qrData);

        Pass pass = new Pass();
        pass.setRegistration(registration);
        pass.setQrCode(qrData);
        pass.setQrImagePath(fileName); // store only filename
        pass.setExpiryDate(registration.getEvent().getEventDate().plusHours(24));
        pass.setIsScanned(false);

        return passRepository.save(pass);
    }

    // ===================== QR DATA =====================
    private String generateQRData(Registration registration) {
        return String.format("EVENT:%d|USER:%d|REG:%d|TIME:%d",
                registration.getEvent().getEventId(),
                registration.getParticipant().getUserId(),
                registration.getRegistrationId(),
                System.currentTimeMillis()
        );
    }

    // ===================== GENERATE IMAGE =====================
    private String generateQRImage(String qrData) {
        try {
            Path uploadPath = Paths.get(uploadDir, "qrcodes");
            Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID() + ".png";
            Path filePath = uploadPath.resolve(fileName);

            BitMatrix bitMatrix = new MultiFormatWriter()
                    .encode(qrData, BarcodeFormat.QR_CODE, 500, 500);

            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);

            log.info(() -> "QR generated: " + fileName);

            return fileName; // ONLY filename

        } catch (Exception e) {
            log.log(Level.SEVERE, "QR generation failed", e);
            throw new RuntimeException("Failed to generate QR pass");
        }
    }

    // ===================== GET PASS =====================
    public Optional<Pass> getPassByRegistration(Long registrationId) {
        return passRepository.findByRegistrationRegistrationId(registrationId);
    }

    // ===================== DOWNLOAD FILE =====================
    public Resource getPassFile(Long registrationId) {

        Pass pass = passRepository.findByRegistrationRegistrationId(registrationId)
                .orElseThrow(() -> new RuntimeException("Pass not found"));

        try {
            Path filePath = Paths.get(uploadDir, "qrcodes", pass.getQrImagePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            log.info(() -> "Pass file accessed for registration: " + registrationId);

            return resource;

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error loading pass file", e);
            throw new RuntimeException("Could not load pass file");
        }
    }

    // ===================== SCAN =====================
    public Pass scanPass(String qrCode, User scanner) {

        Pass pass = passRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Pass not found"));

        if (!pass.isValidForScanning()) {
            throw new IllegalArgumentException("Invalid or expired pass");
        }

        pass.markAsScanned(scanner);

        log.info(() -> "Pass scanned: " + qrCode);

        return passRepository.save(pass);
    }

    // ===================== VALIDATION =====================
    public boolean isPassValid(String qrCode) {
        return passRepository.findByQrCode(qrCode)
                .map(Pass::isValidForScanning)
                .orElse(false);
    }
}