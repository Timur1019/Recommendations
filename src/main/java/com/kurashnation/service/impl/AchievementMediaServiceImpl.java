package com.kurashnation.service.impl;

import com.kurashnation.config.StorageProperties;
import com.kurashnation.dto.response.AchievementMediaResponse;
import com.kurashnation.dto.response.MediaDownload;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Achievement;
import com.kurashnation.model.entity.AchievementMedia;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.AchievementMediaKind;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AchievementMediaRepository;
import com.kurashnation.repository.AchievementRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.AchievementMediaService;
import com.kurashnation.util.LogUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AchievementMediaServiceImpl implements AchievementMediaService {

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime"
    );

    private final StorageProperties storageProperties;
    private final UserRepository userRepository;
    private final CoachProfileProvisioning coachProfileProvisioning;
    private final AchievementRepository achievementRepository;
    private final AchievementMediaRepository achievementMediaRepository;

    private Path resolvedRoot;

    public AchievementMediaServiceImpl(
            StorageProperties storageProperties,
            UserRepository userRepository,
            CoachProfileProvisioning coachProfileProvisioning,
            AchievementRepository achievementRepository,
            AchievementMediaRepository achievementMediaRepository
    ) {
        this.storageProperties = storageProperties;
        this.userRepository = userRepository;
        this.coachProfileProvisioning = coachProfileProvisioning;
        this.achievementRepository = achievementRepository;
        this.achievementMediaRepository = achievementMediaRepository;
    }

    @PostConstruct
    void initRoot() {
        String p = storageProperties.getRootPath();
        if (p == null || p.isBlank()) {
            resolvedRoot = Path.of(System.getProperty("user.dir"), "data", "uploads").normalize();
        } else {
            resolvedRoot = Path.of(p).normalize();
        }
        try {
            Files.createDirectories(resolvedRoot);
        } catch (IOException e) {
            LogUtil.error("Cannot create storage root %s", e, resolvedRoot);
            throw new IllegalStateException("Storage root not writable", e);
        }
    }

    @Override
    public List<AchievementMediaResponse> listDtosForAchievement(Long achievementId) {
        return achievementMediaRepository.findByAchievement_IdOrderByCreatedAtDesc(achievementId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementMediaResponse> listForAchievement(String email, Long achievementId) {
        Achievement a = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new NotFoundException("Achievement not found"));
        assertCanAccessAchievement(email, a);
        return listDtosForAchievement(achievementId);
    }

    @Override
    @Transactional
    public AchievementMediaResponse upload(String email, Long achievementId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required");
        }
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new NotFoundException("Achievement not found"));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) {
            throw new ValidationException("Only athletes can upload media");
        }
        if (!achievement.getAthlete().getUser().getId().equals(user.getId())) {
            throw new ValidationException("Access denied");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new ValidationException("Content-Type is required");
        }
        contentType = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();

        AchievementMediaKind kind;
        long maxBytes;
        if (IMAGE_TYPES.contains(contentType)) {
            kind = AchievementMediaKind.IMAGE;
            maxBytes = storageProperties.getMaxImageBytes();
        } else if (VIDEO_TYPES.contains(contentType)) {
            kind = AchievementMediaKind.VIDEO;
            maxBytes = storageProperties.getMaxVideoBytes();
        } else {
            throw new ValidationException("Unsupported file type: " + contentType);
        }

        long size = file.getSize();
        if (size <= 0 || size > maxBytes) {
            throw new ValidationException("File size not allowed for this media type");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = "upload.bin";
        }
        String ext = extension(original);
        if (!allowedExtension(ext, kind)) {
            throw new ValidationException("Invalid file extension");
        }

        String storageKey = UUID.randomUUID() + "." + ext;
        Path dir = resolvedRoot.resolve("achievement-media").resolve(String.valueOf(achievementId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storageKey);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            LogUtil.error("Upload failed achievementId=%s", e, achievementId);
            throw new ValidationException("Failed to store file");
        }

        AchievementMedia row = new AchievementMedia();
        row.setAchievement(achievement);
        row.setMediaKind(kind);
        row.setStorageKey(storageKey);
        row.setOriginalFilename(original);
        row.setContentType(contentType);
        row.setSizeBytes(size);
        row = achievementMediaRepository.save(row);
        LogUtil.info("Achievement media uploaded id=%s achievementId=%s kind=%s", row.getId(), achievementId, kind);
        return toDto(row);
    }

    @Override
    @Transactional
    public void delete(String email, Long achievementId, Long mediaId) {
        AchievementMedia media = achievementMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found"));
        if (!media.getAchievement().getId().equals(achievementId)) {
            throw new ValidationException("Access denied");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() == UserRole.ATHLETE) {
            if (!media.getAchievement().getAthlete().getUser().getId().equals(user.getId())) {
                throw new ValidationException("Access denied");
            }
        } else if (user.getRole() == UserRole.ADMIN) {
            // ok
        } else {
            throw new ValidationException("Access denied");
        }
        deleteFileOnDisk(achievementId, media.getStorageKey());
        achievementMediaRepository.delete(media);
        LogUtil.info("Achievement media deleted id=%s", mediaId);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaDownload prepareDownload(String email, Long achievementId, Long mediaId) {
        AchievementMedia media = achievementMediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found"));
        if (!media.getAchievement().getId().equals(achievementId)) {
            throw new ValidationException("Access denied");
        }
        assertCanAccessAchievement(email, media.getAchievement());
        Path path = resolvedRoot.resolve("achievement-media").resolve(String.valueOf(achievementId)).resolve(media.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("File missing on server");
        }
        Resource resource = new FileSystemResource(path);
        return new MediaDownload(resource, media.getContentType(), media.getOriginalFilename());
    }

    @Override
    @Transactional
    public void deleteAllForAchievement(Long achievementId) {
        List<AchievementMedia> rows = achievementMediaRepository.findByAchievement_Id(achievementId);
        for (AchievementMedia m : rows) {
            deleteFileOnDisk(achievementId, m.getStorageKey());
        }
        achievementMediaRepository.deleteByAchievement_Id(achievementId);
    }

    private void deleteFileOnDisk(Long achievementId, String storageKey) {
        Path path = resolvedRoot.resolve("achievement-media").resolve(String.valueOf(achievementId)).resolve(storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LogUtil.warn("Could not delete file %s: %s", path, e.getMessage());
        }
    }

    private void assertCanAccessAchievement(String email, Achievement achievement) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.ATHLETE) {
            if (achievement.getAthlete().getUser().getId().equals(user.getId())) {
                return;
            }
            throw new ValidationException("Access denied");
        }
        if (user.getRole() == UserRole.COACH) {
            Coach coach = coachProfileProvisioning.ensureCoachProfile(user);
            if (achievement.getAthlete().getCoach() != null
                    && coach.getId().equals(achievement.getAthlete().getCoach().getId())) {
                return;
            }
            throw new ValidationException("Access denied");
        }
        throw new ValidationException("Access denied");
    }

    private AchievementMediaResponse toDto(AchievementMedia m) {
        long aid = m.getAchievement().getId();
        return new AchievementMediaResponse(
                m.getId(),
                aid,
                m.getMediaKind().name(),
                m.getOriginalFilename(),
                m.getContentType(),
                m.getSizeBytes(),
                "/achievements/" + aid + "/media/" + m.getId() + "/file"
        );
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0 || i >= filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean allowedExtension(String ext, AchievementMediaKind kind) {
        if (ext.isEmpty()) {
            return false;
        }
        return switch (kind) {
            case IMAGE -> Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext);
            case VIDEO -> Set.of("mp4", "webm", "mov").contains(ext);
        };
    }
}
