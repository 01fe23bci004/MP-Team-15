package edu.bvb.campus;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

interface UserRepo extends JpaRepository<AppUser,Long> { Optional<AppUser> findByUsername(String username); List<AppUser> findByRole(Role role); }
interface TechRepo extends JpaRepository<TechnicianProfile,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from TechnicianProfile t where t.userId=:id") Optional<TechnicianProfile> lockByUserId(@Param("id") Long id);
}
interface CategoryRepo extends JpaRepository<ServiceCategory,Long> { List<ServiceCategory> findByActiveTrue(); }
interface SubcategoryRepo extends JpaRepository<ServiceSubcategory,Long> { List<ServiceSubcategory> findByActiveTrue(); }
interface AssignmentRepo extends JpaRepository<ServiceAssignment,Long> {
 long countByTechnicianIdAndRequestStatusIn(Long id,Collection<RequestStatus> states);
 Optional<ServiceAssignment> findByRequestId(Long requestId);
}
interface EventRepo extends JpaRepository<StatusEvent,Long> { List<StatusEvent> findByRequestIdOrderByCreatedAtAsc(Long id); }
interface RequestRepo extends JpaRepository<ServiceRequest,Long> {
 List<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(Long id);
 List<ServiceRequest> findAllByOrderByCreatedAtDesc();
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ServiceRequest r where r.id=:id") Optional<ServiceRequest> lockById(@Param("id") Long id);
}
