package com.kurashnation.controller;

import com.kurashnation.dto.request.TrainingLibraryUpdateRequest;
import com.kurashnation.dto.request.TrainingLibraryUploadRequest;
import com.kurashnation.dto.response.TrainingLibraryFileResponse;
import com.kurashnation.service.interfaces.TrainingLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/training-library")
@PreAuthorize("hasRole('ADMIN')")
public class TrainingLibraryAdminController {

    private final TrainingLibraryService trainingLibraryService;

    public TrainingLibraryAdminController(TrainingLibraryService trainingLibraryService) {
        this.trainingLibraryService = trainingLibraryService;
    }

    @Operation(summary = "List training library files (admin)")
    @ApiResponse(responseCode = "200", description = "Files list")
    @GetMapping
    public List<TrainingLibraryFileResponse> list() {
        return trainingLibraryService.listForAdmin();
    }

    @Operation(summary = "Upload a book/document to training library (admin)")
    @ApiResponse(responseCode = "200", description = "Created")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TrainingLibraryFileResponse upload(
            Authentication authentication,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("file") MultipartFile file
    ) {
        return trainingLibraryService.upload(
                authentication.getName(),
                new TrainingLibraryUploadRequest(title, description),
                file
        );
    }

    @Operation(summary = "Update title/description (admin)")
    @ApiResponse(responseCode = "200", description = "Updated")
    @PutMapping("/{id}")
    public TrainingLibraryFileResponse update(
            Authentication authentication,
            @PathVariable("id") Long id,
            @RequestBody @Valid TrainingLibraryUpdateRequest request
    ) {
        return trainingLibraryService.updateMeta(authentication.getName(), id, request);
    }

    @Operation(summary = "Delete file from training library (admin)")
    @ApiResponse(responseCode = "200", description = "Deleted")
    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable("id") Long id) {
        trainingLibraryService.delete(authentication.getName(), id);
    }
}

