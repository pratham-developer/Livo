package com.pratham.livo.repository;

import com.pratham.livo.entity.RoleRequest;
import com.pratham.livo.enums.Role;
import com.pratham.livo.enums.RoleRequestStatus;
import com.pratham.livo.projection.RoleRequestWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {

    Optional<RoleRequest> findByUserIdAndRole(Long userId, Role role);

    @Query("""
    select new com.pratham.livo.projection.RoleRequestWrapper(
    r.id, u.id, u.name, u.email, r.role, r.status)
    from RoleRequest r join r.user u
    where r.status = :status""")
    Page<RoleRequestWrapper> findRequestsWithStatus(
            @Param("status") RoleRequestStatus status, Pageable pageable);
}
