package com.kurashnation.service.impl;

import com.kurashnation.config.StorageProperties;
import com.kurashnation.dto.request.TrainingLibraryUpdateRequest;
import com.kurashnation.dto.request.TrainingLibraryUploadRequest;
import com.kurashnation.dto.response.MediaDownload;
import com.kurashnation.dto.response.TrainingLibraryFileResponse;
import com.kurashnation.dto.response.TrainingLibraryResolvedFile;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.mapper.TrainingLibraryFileMapper;
import com.kurashnation.model.entity.TrainingLibraryFile;
import com.kurashnation.model.entity.User;
import com.kurashnation.repository.TrainingLibraryFileRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.TrainingLibraryService;
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
public class TrainingLibraryServiceImpl implements TrainingLibraryService {

    private static final long MAX_BYTES = 80L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final StorageProperties storageProperties;
    private final UserRepository userRepository;
    private final TrainingLibraryFileRepository trainingLibraryFileRepository;
    private final TrainingLibraryFileMapper trainingLibraryFileMapper;

    private Path resolvedRoot;

    public TrainingLibraryServiceImpl(
            StorageProperties storageProperties,
            UserRepository userRepository,
            TrainingLibraryFileRepository trainingLibraryFileRepository,
            TrainingLibraryFileMapper trainingLibraryFileMapper
    ) {
        this.storageProperties = storageProperties;
        this.userRepository = userRepository;
        this.trainingLibraryFileRepository = trainingLibraryFileRepository;
        this.trainingLibraryFileMapper = trainingLibraryFileMapper;
    }

    @PostConstruct
    void initRoot() {
        String p = storageProperties.getRootPath();
        if (p == null || p.isBlank()) {
            resolvedRoot = Path.of(System.getProperty("user.dir"), "data", "uploads").normalize();
        } else {
            resolvedRoot = Path.of(p).normalize();
        }
        resolvedRoot = resolvedRoot.resolve("training-library").normalize();
        try {
            Files.createDirectories(resolvedRoot);
        } catch (IOException e) {
            LogUtil.error("Cannot create training library root %s", e, resolvedRoot);
            throw new IllegalStateException("Storage root not writable", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingLibraryFileResponse> listForCoach() {
        return trainingLibraryFileRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(trainingLibraryFileMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingLibraryFileResponse> listForAdmin() {
        return listForCoach();
    }

    @Override
    @Transactional
    public TrainingLibraryFileResponse upload(String adminEmail, TrainingLibraryUploadRequest request, MultipartFile file) {
        if (request == null) throw new ValidationException("Request is required");
        if (file == null || file.isEmpty()) throw new ValidationException("File is required");

        User admin = userRepository.findByEmail(adminEmail).orElseThrow(() -> new NotFoundException("User not found"));

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new ValidationException("Content-Type is required");
        }
        contentType = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ValidationException("Unsupported file type: " + contentType);
        }

        long size = file.getSize();
        if (size <= 0 || size > MAX_BYTES) {
            throw new ValidationException("File size not allowed");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = "document";
        }

        String storageKey = UUID.randomUUID().toString();
        Path target = resolvedRoot.resolve(storageKey);
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            LogUtil.error("Training library upload failed", e);
            throw new ValidationException("Failed to store file");
        }

        TrainingLibraryFile row = new TrainingLibraryFile();
        row.setTitle(request.title().trim());
        row.setDescription(request.description() == null ? null : request.description().trim());
        row.setStorageKey(storageKey);
        row.setOriginalFilename(original);
        row.setContentType(contentType);
        row.setSizeBytes(size);
        row.setUploadedBy(admin);
        row = trainingLibraryFileRepository.save(row);
        LogUtil.info("Training library uploaded id=%s type=%s", row.getId(), row.getContentType());
        return trainingLibraryFileMapper.toDto(row);
    }

    @Override
    @Transactional
    public TrainingLibraryFileResponse updateMeta(String adminEmail, Long id, TrainingLibraryUpdateRequest request) {
        if (request == null) throw new ValidationException("Request is required");
        userRepository.findByEmail(adminEmail).orElseThrow(() -> new NotFoundException("User not found"));

        TrainingLibraryFile row = trainingLibraryFileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
        row.setTitle(request.title().trim());
        row.setDescription(request.description() == null ? null : request.description().trim());
        row = trainingLibraryFileRepository.save(row);
        LogUtil.info("Training library meta updated id=%s", id);
        return trainingLibraryFileMapper.toDto(row);
    }

    @Override
    @Transactional
    public void delete(String adminEmail, Long id) {
        userRepository.findByEmail(adminEmail).orElseThrow(() -> new NotFoundException("User not found"));
        TrainingLibraryFile row = trainingLibraryFileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
        Path path = resolvedRoot.resolve(row.getStorageKey());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LogUtil.warn("Could not delete training library file %s: %s", path, e.getMessage());
        }
        trainingLibraryFileRepository.delete(row);
        LogUtil.info("Training library deleted id=%s", id);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaDownload prepareDownload(Long id) {
        TrainingLibraryFile row = trainingLibraryFileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
        Path path = resolvedRoot.resolve(row.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("File missing on server");
        }
        Resource resource = new FileSystemResource(path);
        return new MediaDownload(resource, row.getContentType(), row.getOriginalFilename());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingLibraryResolvedFile resolveStoredFile(Long id) {
        TrainingLibraryFile row = trainingLibraryFileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("File not found"));
        Path path = resolvedRoot.resolve(row.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("File missing on server");
        }
        return new TrainingLibraryResolvedFile(path, row.getContentType(), row.getOriginalFilename());
    }
}

