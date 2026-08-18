package com.sharing.repo;

import com.sharing.model.CourseEnrollment;
import com.sharing.model.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepo extends JpaRepository<CourseEnrollment, Long> {
    Optional<CourseEnrollment> findByCourseIdAndUserId(Long courseId, Long userId);

    List<CourseEnrollment> findByUserId(Long userId);

    List<CourseEnrollment> findByCourseId(Long courseId);

    Page<CourseEnrollment> findByCourseId(Long courseId, Pageable pageable);

    List<CourseEnrollment> findByUserIdAndStatus(Long userId, EnrollmentStatus status);

    List<CourseEnrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    int countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    int countByUserId(Long userId);

    int countByCourseIdAndUserId(Long courseId, Long userId);

    int countByCourseId(Long courseId);

    @Query(value = "select * from course_enrollments where course_id = :courseId order by enrolled_at desc limit :limit offset :offset", nativeQuery = true)
    List<CourseEnrollment> findByCourseIdPaged(@Param("courseId") Long courseId,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    // --- Sharing / invite specific queries ---

    // Incoming invites (shared with me)
    List<CourseEnrollment> findByUserIdAndInviteType(Long userId, String inviteType);

    List<CourseEnrollment> findByUserIdAndInviteTypeAndInviteStatus(Long userId, String inviteType, String inviteStatus);

    int countByUserIdAndInviteTypeAndInviteStatus(Long userId, String inviteType, String inviteStatus);

    // Outgoing invites (shared by me)
    List<CourseEnrollment> findByInvitedByAndInviteType(Long invitedBy, String inviteType);

    @Query("select count(ce) from CourseEnrollment ce where ce.userId = :userId and ce.inviteType = :inviteType and ce.inviteStatus = :inviteStatus and (ce.isRead is null or ce.isRead = false)")
    int countUnreadInvites(@Param("userId") Long userId,
                           @Param("inviteType") String inviteType,
                           @Param("inviteStatus") String inviteStatus);

    @Query("select distinct ce.courseId from CourseEnrollment ce where ce.invitedBy = :invitedBy and ce.inviteType = :inviteType")
    List<Long> findDistinctCourseIdsByInvitedByAndInviteType(@Param("invitedBy") Long invitedBy,
                                                             @Param("inviteType") String inviteType);
}
