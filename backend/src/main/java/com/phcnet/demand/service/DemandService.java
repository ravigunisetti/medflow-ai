package com.phcnet.demand.service;

import com.phcnet.demand.dto.DemandDTO;
import com.phcnet.demand.dto.FootfallDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DemandService {
    Page<DemandDTO> getDemands(Long phcId, Long medicineId, Pageable pageable);
    List<DemandDTO> getDemandHistory(Long phcId, Long medicineId, int days);
    Double getAverageDailyDemand(Long phcId, Long medicineId, int days);

    Page<FootfallDTO> getFootfalls(Long phcId, Pageable pageable);
    List<FootfallDTO> getFootfallHistory(Long phcId, int days);
    DemandDTO recordDemand(Long phcId, Long medicineId, LocalDate date, Integer quantity);
    FootfallDTO recordFootfall(Long phcId, LocalDate date, Integer patients, Integer emergencies);
}
