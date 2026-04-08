package com.eams.controller;

import com.eams.dto.RemarkRequest;
import com.eams.dto.StatusUpdateRequest;
import com.eams.entity.Achievement;
import com.eams.service.AchievementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createAchievement(
            @RequestParam("data") String achievementData,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) throws Exception {
        return ResponseEntity.ok(achievementService.create(achievementData, files, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<Achievement>> getAchievements(Authentication authentication) {
        return ResponseEntity.ok(achievementService.getAllByRole(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAchievement(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(achievementService.getById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAchievement(
            @PathVariable Long id,
            @RequestParam("data") String achievementData,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) throws Exception {
        return ResponseEntity.ok(achievementService.update(id, achievementData, files, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAchievement(@PathVariable Long id, Authentication authentication) {
        achievementService.delete(id, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Achievement deleted successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(achievementService.updateStatus(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/remarks")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> addRemark(
            @PathVariable Long id,
            @Valid @RequestBody RemarkRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(achievementService.addRemark(id, request.getContent(), authentication.getName()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> getStats(Authentication authentication) {
        List<Achievement> all = achievementService.getAllByRole(authentication.getName());
        long total = all.size();
        long pending = all.stream().filter(a -> a.getStatus() == Achievement.Status.PENDING).count();
        long approved = all.stream().filter(a -> a.getStatus() == Achievement.Status.APPROVED).count();
        long rejected = all.stream().filter(a -> a.getStatus() == Achievement.Status.REJECTED).count();
        return ResponseEntity.ok(Map.of("total", total, "pending", pending, "approved", approved, "rejected", rejected));
    }
}
