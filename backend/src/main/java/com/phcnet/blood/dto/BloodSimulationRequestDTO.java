package com.phcnet.blood.dto;

import com.phcnet.blood.model.RequestPriority;
import java.util.List;

public record BloodSimulationRequestDTO(
        Long hospitalId,
        String bloodGroup,
        Integer unitsRequired,
        RequestPriority priority,
        Integer deadlineMinutes,
        String scenarioType // e.g. MASS_CASUALTY_ACCIDENT, POSTPARTUM_HEMORRHAGE, URGENT_SURGERY
) {}
