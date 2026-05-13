package com.kurashnation.mapper;

import com.kurashnation.dto.response.TrainingLibraryFileResponse;
import com.kurashnation.model.entity.TrainingLibraryFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrainingLibraryFileMapper extends BaseMapper<TrainingLibraryFile, TrainingLibraryFileResponse> {
    @Override
    @Mapping(target = "viewUrl", expression = "java(\"/training-library/files/\" + entity.getId())")
    @Mapping(target = "downloadUrl", expression = "java(\"/training-library/files/\" + entity.getId() + \"?download=1\")")
    TrainingLibraryFileResponse toDto(TrainingLibraryFile entity);
}

