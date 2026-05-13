package com.kurashnation.mapper;

import com.kurashnation.dto.response.CoachResponse;
import com.kurashnation.model.entity.Coach;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMappingHelper.class})
public interface CoachMapper extends BaseMapper<Coach, CoachResponse> {
    @Override
    @Mapping(target = "user", source = "user", qualifiedByName = "userToDto")
    CoachResponse toDto(Coach coach);
}

