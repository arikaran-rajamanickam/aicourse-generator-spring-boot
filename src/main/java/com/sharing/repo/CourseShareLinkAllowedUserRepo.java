package com.sharing.repo;

import com.sharing.model.CourseShareLinkAllowedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseShareLinkAllowedUserRepo extends JpaRepository<CourseShareLinkAllowedUser, Long> {

    List<CourseShareLinkAllowedUser> findByShareLinkId(Long shareLinkId);

    boolean existsByShareLinkIdAndUserId(Long shareLinkId, Long userId);
}

