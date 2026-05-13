package com.kurashnation.service.interfaces;

import com.kurashnation.dto.response.TrainingRequestRowResponse;

import java.util.List;

public interface TrainingRequestListService {

    List<TrainingRequestRowResponse> listMine(String email);

    List<TrainingRequestRowResponse> listForCoach(String email);

    List<TrainingRequestRowResponse> listForAdmin(String email);
}
