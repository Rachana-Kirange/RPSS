package com.eventra.eventra.repository;

import com.eventra.eventra.model.Pass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassRepository extends JpaRepository<Pass, Long> {

    Optional<Pass> findByQrCode(String qrCode);

    Optional<Pass> findByRegistrationRegistrationId(Long registrationId);
}
