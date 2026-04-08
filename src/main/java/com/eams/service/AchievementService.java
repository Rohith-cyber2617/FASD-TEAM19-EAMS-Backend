package com.eams.service;

import com.eams.dto.StatusUpdateRequest;
import com.eams.entity.Achievement;
import com.eams.entity.Attachment;
import com.eams.entity.Remark;
import com.eams.entity.User;
import com.eams.repository.AchievementRepository;
import com.eams.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public AchievementService(
            AchievementRepository achievementRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper) {
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public Achievement create(String achievementData, MultipartFile[] files, String email) throws Exception {
        Achievement achievement = objectMapper.readValue(achievementData, Achievement.class);
        User user = getUserByEmail(email);

        if (user.getRole() != User.Role.STUDENT) {
            throw new IllegalArgumentException("Only students can submit achievements");
        }

        achievement.setUser(user);
        achievement.setStatus(Achievement.Status.PENDING);
        calculateDuration(achievement);

        if (files != null) {
            for (MultipartFile file : files) {
                String storedFile = fileStorageService.storeFile(file);
                Attachment attachment = new Attachment();
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFilePath(storedFile);
                attachment.setFileType(file.getContentType());
                attachment.setAchievement(achievement);
                achievement.getAttachments().add(attachment);
            }
        }

        return achievementRepository.save(achievement);
    }

    public List<Achievement> getAllByRole(String email) {
        User user = getUserByEmail(email);
        if (user.getRole() == User.Role.STUDENT) {
            return achievementRepository.findByUser(user);
        }
        return achievementRepository.findAll();
    }

    public Achievement getById(Long id, String email) {
        Achievement achievement = getAchievementById(id);
        User user = getUserByEmail(email);

        if (user.getRole() == User.Role.STUDENT && !achievement.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized access to another student's data");
        }

        return achievement;
    }

    public Achievement update(Long id, String achievementData, MultipartFile[] files, String email) throws Exception {
        Achievement existing = getAchievementById(id);
        User actor = getUserByEmail(email);

        boolean isOwner = existing.getUser().getId().equals(actor.getId());
        boolean canEdit = actor.getRole() == User.Role.ADMIN || actor.getRole() == User.Role.FACULTY ||
                (actor.getRole() == User.Role.STUDENT && isOwner && existing.getStatus() == Achievement.Status.PENDING);

        if (!canEdit) {
            throw new IllegalArgumentException("You are not allowed to edit this achievement");
        }

        Achievement incoming = objectMapper.readValue(achievementData, Achievement.class);

        existing.setStudentName(incoming.getStudentName());
        existing.setStudentId(incoming.getStudentId());
        existing.setCollegeName(incoming.getCollegeName());
        existing.setTitle(incoming.getTitle());
        existing.setCategory(incoming.getCategory());
        existing.setType(incoming.getType());
        existing.setAwardType(incoming.getAwardType());
        existing.setStartDate(incoming.getStartDate());
        existing.setEndDate(incoming.getEndDate());
        existing.setLocation(incoming.getLocation());
        existing.setEventLevel(incoming.getEventLevel());
        existing.setOrganizedBy(incoming.getOrganizedBy());
        existing.setDescription(incoming.getDescription());

        calculateDuration(existing);

        if (files != null) {
            for (MultipartFile file : files) {
                String storedFile = fileStorageService.storeFile(file);
                Attachment attachment = new Attachment();
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFilePath(storedFile);
                attachment.setFileType(file.getContentType());
                attachment.setAchievement(existing);
                existing.getAttachments().add(attachment);
            }
        }

        if (actor.getRole() == User.Role.FACULTY || actor.getRole() == User.Role.ADMIN) {
            existing.setStatus(Achievement.Status.PENDING);
        }

        return achievementRepository.save(existing);
    }

    public void delete(Long id, String email) {
        Achievement achievement = getAchievementById(id);
        User actor = getUserByEmail(email);

        boolean isOwner = achievement.getUser().getId().equals(actor.getId());
        boolean canDelete = actor.getRole() == User.Role.ADMIN ||
                (actor.getRole() == User.Role.STUDENT && isOwner && achievement.getStatus() == Achievement.Status.PENDING);

        if (!canDelete) {
            throw new IllegalArgumentException("You are not allowed to delete this achievement");
        }

        achievementRepository.delete(achievement);
    }

    public Achievement updateStatus(Long id, StatusUpdateRequest request, String email) {
        Achievement achievement = getAchievementById(id);
        User reviewer = getUserByEmail(email);

        if (reviewer.getRole() != User.Role.FACULTY && reviewer.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only faculty/admin can update status");
        }

        Achievement.Status newStatus = Achievement.Status.valueOf(request.getStatus());
        achievement.setStatus(newStatus);

        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            addRemarkInternal(achievement, reviewer, request.getRemarks());
        }

        return achievementRepository.save(achievement);
    }

    public Achievement addRemark(Long id, String content, String email) {
        Achievement achievement = getAchievementById(id);
        User reviewer = getUserByEmail(email);

        if (reviewer.getRole() != User.Role.FACULTY && reviewer.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only faculty/admin can add remarks");
        }

        addRemarkInternal(achievement, reviewer, content);
        return achievementRepository.save(achievement);
    }

    private void addRemarkInternal(Achievement achievement, User reviewer, String content) {
        Remark remark = new Remark();
        remark.setContent(content);
        remark.setCreatedAt(LocalDateTime.now());
        remark.setFaculty(reviewer);
        remark.setAchievement(achievement);
        achievement.getRemarks().add(remark);
    }

    private void calculateDuration(Achievement achievement) {
        if (achievement.getStartDate() != null && achievement.getEndDate() != null) {
            if (achievement.getEndDate().isBefore(achievement.getStartDate())) {
                throw new IllegalArgumentException("Event end date cannot be before start date");
            }
            achievement.setDuration(ChronoUnit.DAYS.between(achievement.getStartDate(), achievement.getEndDate()) + 1);
        }
    }

    private Achievement getAchievementById(Long id) {
        return achievementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
