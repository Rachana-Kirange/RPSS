package com.eventra.eventra.repository;

import com.eventra.eventra.model.User;
import com.eventra.eventra.enums.UserStatus;
import com.eventra.eventra.enums.RoleEnum;
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
    List<User> findByRole(@Param("roleName") RoleEnum roleName);

    @Query("SELECT u FROM User u WHERE u.role.roleId = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Approval Status Query Methods
    List<User> findByApprovalStatus(UserStatus approvalStatus);

    @Query("SELECT u FROM User u WHERE u.approvalStatus = :status AND u.role.roleName = :roleNameValue")
    List<User> findByApprovalStatusAndRole(@Param("status") UserStatus status, @Param("roleNameValue") RoleEnum role);

    long countByApprovalStatus(UserStatus approvalStatus);

    @Query("SELECT COUNT(u) FROM User u WHERE u.approvalStatus = :status AND u.role.roleName = :roleNameValue")
    long countByApprovalStatusAndRole(@Param("status") UserStatus status, @Param("roleNameValue") RoleEnum role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName")
    long countByRoleName(@Param("roleName") RoleEnum roleName);
}
