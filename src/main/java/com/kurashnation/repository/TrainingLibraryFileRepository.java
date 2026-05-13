package com.kurashnation.repository;

import com.kurashnation.model.entity.TrainingLibraryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingLibraryFileRepository extends JpaRepository<TrainingLibraryFile, Long> {
    List<TrainingLibraryFile> findAllByOrderByCreatedAtDesc();
}

