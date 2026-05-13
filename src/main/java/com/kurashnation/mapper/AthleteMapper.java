package com.kurashnation.mapper;

import com.kurashnation.dto.response.AthleteResponse;
import com.kurashnation.model.entity.Athlete;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMappingHelper.class})
public interface AthleteMapper extends BaseMapper<Athlete, AthleteResponse> {
    @Override
    @Mapping(target = "user", source = "user", qualifiedByName = "userToDto")
    @Mapping(target = "coachId", expression = "java(athlete.getCoach() == null ? null : athlete.getCoach().getId())")
    AthleteResponse toDto(Athlete athlete);
}

