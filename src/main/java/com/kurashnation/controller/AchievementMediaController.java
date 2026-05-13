package com.kurashnation.controller;

import com.kurashnation.dto.response.AchievementMediaResponse;
import com.kurashnation.dto.response.MediaDownload;
import com.kurashnation.service.interfaces.AchievementMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/achievements")
public class AchievementMediaController {

    private final AchievementMediaService achievementMediaService;

    public AchievementMediaController(AchievementMediaService achievementMediaService) {
        this.achievementMediaService = achievementMediaService;
    }

    @Operation(summary = "List photo/video for achievement")
    @ApiResponse(responseCode = "200", description = "Media list")
    @PreAuthorize("hasAnyRole('ATHLETE','COACH','ADMIN')")
    @GetMapping("/{achievementId}/media")
    public List<AchievementMediaResponse> list(
            Authentication authentication,
            @PathVariable("achievementId") Long achievementId
    ) {
        return achievementMediaService.listForAchievement(authentication.getName(), achievementId);
    }

    @Operation(summary = "Upload photo or video for own achievement (athlete)")
    @ApiResponse(responseCode = "200", description = "Media created")
    @PreAuthorize("hasRole('ATHLETE')")
    @PostMapping(value = "/{achievementId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AchievementMediaResponse upload(
            Authentication authentication,
            @PathVariable("achievementId") Long achievementId,
            @RequestPart("file") MultipartFile file
    ) {
        return achievementMediaService.upload(authentication.getName(), achievementId, file);
    }

    @Operation(summary = "Delete media (athlete owner or admin)")
    @ApiResponse(responseCode = "200", description = "Deleted")
    @PreAuthorize("hasAnyRole('ATHLETE','ADMIN')")
    @DeleteMapping("/{achievementId}/media/{mediaId}")
    public void delete(
            Authentication authentication,
            @PathVariable("achievementId") Long achievementId,
            @PathVariable("mediaId") Long mediaId
    ) {
        achievementMediaService.delete(authentication.getName(), achievementId, mediaId);
    }

    @Operation(summary = "Download or inline-view file (auth required)")
    @ApiResponse(responseCode = "200", description = "Binary content")
    @PreAuthorize("hasAnyRole('ATHLETE','COACH','ADMIN')")
    @GetMapping("/{achievementId}/media/{mediaId}/file")
    public ResponseEntity<Resource> file(
            Authentication authentication,
            @PathVariable("achievementId") Long achievementId,
            @PathVariable("mediaId") Long mediaId
    ) {
        MediaDownload d = achievementMediaService.prepareDownload(authentication.getName(), achievementId, mediaId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(d.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(d.contentType()))
                .body(d.resource());
    }
}
