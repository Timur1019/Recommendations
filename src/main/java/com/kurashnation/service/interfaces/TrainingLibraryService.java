package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.TrainingLibraryUpdateRequest;
import com.kurashnation.dto.request.TrainingLibraryUploadRequest;
import com.kurashnation.dto.response.MediaDownload;
import com.kurashnation.dto.response.TrainingLibraryFileResponse;
import com.kurashnation.dto.response.TrainingLibraryResolvedFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TrainingLibraryService {
    List<TrainingLibraryFileResponse> listForCoach();

    List<TrainingLibraryFileResponse> listForAdmin();

    TrainingLibraryFileResponse upload(String adminEmail, TrainingLibraryUploadRequest request, MultipartFile file);

    TrainingLibraryFileResponse updateMeta(String adminEmail, Long id, TrainingLibraryUpdateRequest request);

    void delete(String adminEmail, Long id);

    MediaDownload prepareDownload(Long id);

    TrainingLibraryResolvedFile resolveStoredFile(Long id);
}

