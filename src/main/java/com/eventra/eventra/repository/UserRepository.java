package com.eventra.eventra.repository;

import com.eventra.eventra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    List<User> findByIsActiveTrue();

    @Query("SELECT u FROM User u WHERE u.role.roleName = :roleName")
    List<User> findByRole(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u WHERE u.role.roleId = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
