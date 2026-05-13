package com.kurashnation.service.interfaces;

import com.kurashnation.dto.response.AchievementMediaResponse;
import com.kurashnation.dto.response.MediaDownload;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AchievementMediaService {

    /** Без проверки прав — только для сборки {@code AchievementResponse} внутри сервиса. */
    List<AchievementMediaResponse> listDtosForAchievement(Long achievementId);

    List<AchievementMediaResponse> listForAchievement(String email, Long achievementId);

    AchievementMediaResponse upload(String email, Long achievementId, MultipartFile file);

    void delete(String email, Long achievementId, Long mediaId);

    MediaDownload prepareDownload(String email, Long achievementId, Long mediaId);

    /** Удалить файлы с диска и записи БД перед удалением достижения */
    void deleteAllForAchievement(Long achievementId);
}
