package com.kurashnation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurashnation.dto.request.SubmitTrainingRequestNoteDto;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.TrainingRequest;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.TrainingRequestStatus;
import com.kurashnation.repository.TrainingRequestRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.TrainingRequestSubmissionService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingRequestSubmissionServiceImpl implements TrainingRequestSubmissionService {

    private final UserRepository userRepository;
    private final TrainingRequestRepository trainingRequestRepository;
    private final ObjectMapper objectMapper;
    private final AthleteProfileProvisioning athleteProfileProvisioning;

    public TrainingRequestSubmissionServiceImpl(
            UserRepository userRepository,
            TrainingRequestRepository trainingRequestRepository,
            ObjectMapper objectMapper,
            AthleteProfileProvisioning athleteProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.trainingRequestRepository = trainingRequestRepository;
        this.objectMapper = objectMapper;
        this.athleteProfileProvisioning = athleteProfileProvisioning;
    }

    @Override
    @Transactional
    public long submitForAthlete(String email, SubmitTrainingRequestNoteDto dto) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);

        JsonNode proposedData = null;
        if (dto != null && dto.note() != null && !dto.note().isBlank()) {
            proposedData = objectMapper.createObjectNode().put("note", dto.note().trim());
        }

        TrainingRequest row = new TrainingRequest();
        row.setAthlete(athlete);
        row.setProposedData(proposedData);
        row.setRequestedBy(user);
        row.setStatus(TrainingRequestStatus.PENDING);

        TrainingRequest saved = trainingRequestRepository.save(row);
        LogUtil.info("Training request created id=%s athleteId=%s", saved.getId(), athlete.getId());
        return saved.getId();
    }
}
