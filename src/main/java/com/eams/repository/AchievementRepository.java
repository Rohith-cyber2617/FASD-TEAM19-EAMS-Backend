package com.eams.repository;

import com.eams.entity.Achievement;
import com.eams.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByUser(User user);
    List<Achievement> findByStatus(Achievement.Status status);
    long countByStatus(Achievement.Status status);
}
