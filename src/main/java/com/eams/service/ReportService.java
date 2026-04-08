package com.eams.service;

import com.eams.entity.Achievement;
import com.eams.repository.AchievementRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final AchievementRepository achievementRepository;

    public ReportService(AchievementRepository achievementRepository) {
        this.achievementRepository = achievementRepository;
    }

    public Map<String, Object> getSummaryReport() {
        long total = achievementRepository.count();
        long pending = achievementRepository.countByStatus(Achievement.Status.PENDING);
        long approved = achievementRepository.countByStatus(Achievement.Status.APPROVED);
        long rejected = achievementRepository.countByStatus(Achievement.Status.REJECTED);

        return Map.of(
                "total", total,
                "pending", pending,
                "approved", approved,
                "rejected", rejected
        );
    }

    public List<Map<String, Object>> getCategoryWiseReport() {
        return achievementRepository.findAll().stream()
                .collect(Collectors.groupingBy(Achievement::getCategory, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> Map.of(
                        "category", entry.getKey(),
                        "count", (Object) entry.getValue()
                ))
                .toList();
    }

    public List<Map<String, Object>> getStudentWiseReport() {
        return achievementRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        achievement -> achievement.getStudentId() + " - " + achievement.getStudentName(),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> Map.of(
                        "student", entry.getKey(),
                        "count", (Object) entry.getValue()
                ))
                .toList();
    }
}
