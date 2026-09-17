package com.phcnet.demand.service;

import com.phcnet.common.exception.ResourceNotFoundException;
import com.phcnet.demand.dto.DemandDTO;
import com.phcnet.demand.dto.FootfallDTO;
import com.phcnet.demand.model.Demand;
import com.phcnet.demand.model.PatientFootfall;
import com.phcnet.demand.repository.DemandRepository;
import com.phcnet.demand.repository.FootfallRepository;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DemandServiceImpl implements DemandService {

    private final DemandRepository demandRepository;
    private final FootfallRepository footfallRepository;
    private final PhcRepository phcRepository;
    private final MedicineRepository medicineRepository;

    public DemandServiceImpl(DemandRepository demandRepository,
                             FootfallRepository footfallRepository,
                             PhcRepository phcRepository,
                             MedicineRepository medicineRepository) {
        this.demandRepository = demandRepository;
        this.footfallRepository = footfallRepository;
        this.phcRepository = phcRepository;
        this.medicineRepository = medicineRepository;
    }

    @Override
    public Page<DemandDTO> getDemands(Long phcId, Long medicineId, Pageable pageable) {
        return demandRepository.findFiltered(phcId, medicineId, pageable).map(DemandDTO::from);
    }

    @Override
    public List<DemandDTO> getDemandHistory(Long phcId, Long medicineId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        return demandRepository.findHistory(phcId, medicineId, since).stream().map(DemandDTO::from).toList();
    }

    @Override
    public Double getAverageDailyDemand(Long phcId, Long medicineId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        Double avg = demandRepository.getAverageDailyDemand(phcId, medicineId, since);
        return avg != null ? avg : 0.0;
    }

    @Override
    public Page<FootfallDTO> getFootfalls(Long phcId, Pageable pageable) {
        return footfallRepository.findFiltered(phcId, pageable).map(FootfallDTO::from);
    }

    @Override
    public List<FootfallDTO> getFootfallHistory(Long phcId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        return footfallRepository.findHistory(phcId, since).stream().map(FootfallDTO::from).toList();
    }

    @Override
    @Transactional
    public DemandDTO recordDemand(Long phcId, Long medicineId, LocalDate date, Integer quantity) {
        Phc phc = phcRepository.findById(phcId)
                .orElseThrow(() -> new ResourceNotFoundException("PHC " + phcId + " not found"));
        Medicine med = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine " + medicineId + " not found"));

        Demand demand = new Demand(null, phc, med, date, quantity);
        return DemandDTO.from(demandRepository.save(demand));
    }

    @Override
    @Transactional
    public FootfallDTO recordFootfall(Long phcId, LocalDate date, Integer patients, Integer emergencies) {
        Phc phc = phcRepository.findById(phcId)
                .orElseThrow(() -> new ResourceNotFoundException("PHC " + phcId + " not found"));

        PatientFootfall pf = new PatientFootfall(null, phc, date, patients, emergencies);
        return FootfallDTO.from(footfallRepository.save(pf));
    }
}
