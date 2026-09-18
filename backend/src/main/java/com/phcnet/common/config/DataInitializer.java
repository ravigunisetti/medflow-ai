package com.phcnet.common.config;

import com.phcnet.demand.model.Demand;
import com.phcnet.demand.model.PatientFootfall;
import com.phcnet.demand.repository.DemandRepository;
import com.phcnet.demand.repository.FootfallRepository;
import com.phcnet.inventory.model.Inventory;
import com.phcnet.inventory.repository.InventoryRepository;
import com.phcnet.medicine.model.Medicine;
import com.phcnet.medicine.repository.MedicineRepository;
import com.phcnet.phc.model.Phc;
import com.phcnet.phc.repository.PhcRepository;
import com.phcnet.prediction.service.PredictionService;
import com.phcnet.security.model.Role;
import com.phcnet.security.model.User;
import com.phcnet.security.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            PhcRepository phcRepo,
            MedicineRepository medRepo,
            InventoryRepository invRepo,
            DemandRepository demandRepo,
            FootfallRepository footfallRepo,
            PredictionService predictionService,
            UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            com.phcnet.blood.repository.BloodBankRepository bloodBankRepo,
            com.phcnet.blood.repository.BloodInventoryRepository bloodInventoryRepo,
            com.phcnet.blood.repository.EmergencyBloodRequestRepository bloodReqRepo,
            com.phcnet.blood.repository.BloodTransferRepository bloodTransferRepo) {
        return args -> {
            // Seed users if empty
            if (userRepo.count() == 0) {
                log.info("Seeding default role-based user accounts...");
                userRepo.save(new User(
                        null,
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "Dr. Rajesh Verma (State Health Director)",
                        "admin@phcnet.gov.in",
                        Role.ROLE_ADMIN,
                        null,
                        null
                ));
                userRepo.save(new User(
                        null,
                        "district_pune",
                        passwordEncoder.encode("pune123"),
                        "Dr. Sunita Kulkarni (Pune District Health Officer)",
                        "dho.pune@phcnet.gov.in",
                        Role.ROLE_DISTRICT_OFFICER,
                        "Pune",
                        null
                ));
                userRepo.save(new User(
                        null,
                        "phc_shirwal",
                        passwordEncoder.encode("shirwal123"),
                        "Dr. Amit Deshmukh (Medical Officer In-Charge)",
                        "mo.shirwal@phcnet.gov.in",
                        Role.ROLE_PHC_MANAGER,
                        "Satara",
                        1L
                ));
                log.info("Seeded 3 RBAC accounts: admin, district_pune, phc_shirwal.");
            }

            Path dataPath = Paths.get("..", "data").toAbsolutePath().normalize();
            if (!dataPath.toFile().exists()) {
                dataPath = Paths.get("data").toAbsolutePath().normalize();
            }

            // Seed Blood Network if empty
            if (bloodBankRepo.count() == 0) {
                initBloodNetwork(dataPath, bloodBankRepo, bloodInventoryRepo, phcRepo, bloodReqRepo, bloodTransferRepo);
            }

            if (phcRepo.count() > 0) {
                log.info("Database already contains {} PHCs. Skipping automatic medicine data bootstrap.", phcRepo.count());
                return;
            }

            log.info("Database is empty. Ingesting primary healthcare datasets from {} ...", dataPath);

            File phcsFile = new File(dataPath.toFile(), "phcs.csv");
            File medsFile = new File(dataPath.toFile(), "medicines.csv");
            File invFile = new File(dataPath.toFile(), "inventories.csv");
            File footfallFile = new File(dataPath.toFile(), "patient_footfalls.csv");

            if (phcsFile.exists() && medsFile.exists() && invFile.exists()) {
                log.info("Found synthetic datasets in {}. Performing fast startup ingestion...", dataPath);
                Map<Long, Phc> phcMap = new HashMap<>();
                Map<Long, Medicine> medMap = new HashMap<>();

                // 1. Ingest PHCs
                try (BufferedReader br = new BufferedReader(new FileReader(phcsFile))) {
                    String line = br.readLine(); // skip header
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length >= 7) {
                            Long id = Long.parseLong(parts[0]);
                            String name = parts[1];
                            String district = parts[2];
                            String state = parts[3];
                            Double lat = Double.parseDouble(parts[4]);
                            Double lon = Double.parseDouble(parts[5]);
                            Integer pop = Integer.parseInt(parts[6]);
                            Phc p = new Phc(null, name, district, state, lat, lon, pop, true);
                            phcMap.put(id, phcRepo.save(p));
                        }
                    }
                }
                log.info("Loaded {} PHCs.", phcMap.size());

                // 2. Ingest Medicines
                try (BufferedReader br = new BufferedReader(new FileReader(medsFile))) {
                    String line = br.readLine(); // skip header
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length >= 6) {
                            Long id = Long.parseLong(parts[0]);
                            String code = parts[1];
                            String name = parts[2];
                            String category = parts[3];
                            String unit = parts[4];
                            Integer safety = Integer.parseInt(parts[5]);
                            Medicine m = new Medicine(null, code, name, category, unit, safety, 730, false);
                            medMap.put(id, medRepo.save(m));
                        }
                    }
                }
                log.info("Loaded {} Medicines.", medMap.size());

                // 3. Ingest Inventories
                try (BufferedReader br = new BufferedReader(new FileReader(invFile))) {
                    String line = br.readLine(); // skip header
                    int count = 0;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length >= 6) {
                            Long pId = Long.parseLong(parts[1]);
                            Long mId = Long.parseLong(parts[2]);
                            Integer qty = Integer.parseInt(parts[3]);
                            Integer res = Integer.parseInt(parts[4]);
                            String batch = parts[5];
                            Phc p = phcMap.get(pId);
                            Medicine m = medMap.get(mId);
                            if (p != null && m != null) {
                                Inventory inv = new Inventory(null, p, m, qty, res, batch, LocalDate.now().plusYears(1));
                                invRepo.save(inv);
                                count++;
                            }
                        }
                    }
                    log.info("Loaded {} Inventory records.", count);
                }

                // 4. Ingest recent Footfalls (last 30 days sample for instant analytics)
                if (footfallFile.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(footfallFile))) {
                        String line = br.readLine(); // skip header
                        int ffCount = 0;
                        while ((line = br.readLine()) != null && ffCount < 3000) {
                            String[] parts = line.split(",");
                            if (parts.length >= 5) {
                                Long pId = Long.parseLong(parts[1]);
                                LocalDate date = LocalDate.parse(parts[2]);
                                Integer count = Integer.parseInt(parts[3]);
                                Integer emerg = Integer.parseInt(parts[4]);
                                Phc p = phcMap.get(pId);
                                if (p != null) {
                                    footfallRepo.save(new PatientFootfall(null, p, date, count, emerg));
                                    ffCount++;
                                }
                            }
                        }
                        log.info("Loaded recent sample of {} Footfall records.", ffCount);
                    }
                }

                // Run baseline risk evaluation across facilities
                log.info("Evaluating initial stock-out risk ratings...");
                predictionService.evaluateAllFacilities();

                // Seed initial active emergency blood requests now that PHCs are loaded
                seedSampleBloodRequests(bloodReqRepo, bloodTransferRepo, phcRepo, bloodBankRepo);

                log.info("Bootstrap complete. System is ready for live traffic!");

            } else {
                log.warn("Synthetic data CSV files not found. Creating manual sample bootstrap...");
                Phc p1 = phcRepo.save(new Phc(null, "Alandi PHC-1", "Pune", "Maharashtra", 18.6744, 73.8967, 34000, true));
                Phc p2 = phcRepo.save(new Phc(null, "Shirwal PHC-2", "Satara", "Maharashtra", 18.1345, 74.0234, 29000, true));

                Medicine m1 = medRepo.save(new Medicine(null, "MED-005", "Paracetamol 500mg", "Analgesic/Antipyretic", "Tablet", 3000, 1095, false));
                Medicine m2 = medRepo.save(new Medicine(null, "MED-001", "Amoxicillin 500mg", "Antibiotic", "Capsule", 1000, 730, false));

                invRepo.save(new Inventory(null, p1, m1, 150, 0, "BATCH-2026-001", LocalDate.now().plusMonths(12)));
                invRepo.save(new Inventory(null, p1, m2, 850, 0, "BATCH-2026-002", LocalDate.now().plusMonths(12)));
                invRepo.save(new Inventory(null, p2, m1, 4500, 0, "BATCH-2026-003", LocalDate.now().plusMonths(12)));

                predictionService.evaluateAllFacilities();
                log.info("Manual sample bootstrap complete.");
            }
        };
    }

    private void initBloodNetwork(
            Path dataPath,
            com.phcnet.blood.repository.BloodBankRepository bloodBankRepo,
            com.phcnet.blood.repository.BloodInventoryRepository bloodInventoryRepo,
            PhcRepository phcRepo,
            com.phcnet.blood.repository.EmergencyBloodRequestRepository bloodReqRepo,
            com.phcnet.blood.repository.BloodTransferRepository bloodTransferRepo) {
        log.info("Initializing Emergency Blood Network datasets...");
        File banksFile = new File(dataPath.toFile(), "blood_banks.csv");
        File invFile = new File(dataPath.toFile(), "blood_inventories.csv");

        Map<Long, com.phcnet.blood.model.BloodBank> bankMap = new HashMap<>();

        if (banksFile.exists() && invFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(banksFile))) {
                String line = br.readLine(); // skip header
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 12) {
                        Long id = Long.parseLong(parts[0]);
                        String name = parts[1];
                        String district = parts[2];
                        String state = parts[3];
                        Double lat = Double.parseDouble(parts[4]);
                        Double lon = Double.parseDouble(parts[5]);
                        com.phcnet.blood.model.VerificationStatus vStatus = com.phcnet.blood.model.VerificationStatus.valueOf(parts[6]);
                        String phone = parts[7];
                        String email = parts[8];
                        String hours = parts[9];
                        Integer cap = Integer.parseInt(parts[10]);
                        Boolean active = Boolean.parseBoolean(parts[11]);

                        com.phcnet.blood.model.BloodBank bb = new com.phcnet.blood.model.BloodBank(
                                null, name, district, state, lat, lon, vStatus, phone, email, hours, cap, active
                        );
                        bankMap.put(id, bloodBankRepo.save(bb));
                    }
                }
                log.info("Loaded {} Blood Banks into Emergency Blood Network.", bankMap.size());
            } catch (Exception e) {
                log.error("Failed to parse blood_banks.csv", e);
            }

            try (BufferedReader br = new BufferedReader(new FileReader(invFile))) {
                String line = br.readLine(); // skip header
                int invCount = 0;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        Long bankId = Long.parseLong(parts[1]);
                        String group = parts[2];
                        String compType = parts[3];
                        Integer avail = Integer.parseInt(parts[4]);
                        Integer res = Integer.parseInt(parts[5]);

                        com.phcnet.blood.model.BloodBank bb = bankMap.get(bankId);
                        if (bb != null) {
                            com.phcnet.blood.model.BloodInventory inv = new com.phcnet.blood.model.BloodInventory(
                                    null, bb, group, compType, avail, res
                            );
                            bloodInventoryRepo.save(inv);
                            invCount++;
                        }
                    }
                }
                log.info("Loaded {} Blood Inventory records into Emergency Blood Network.", invCount);
            } catch (Exception e) {
                log.error("Failed to parse blood_inventories.csv", e);
            }
        } else {
            log.warn("Blood Network CSV files not found. Creating default regional sample blood bank.");
            com.phcnet.blood.model.BloodBank bb = bloodBankRepo.save(new com.phcnet.blood.model.BloodBank(
                    null, "Pune Central Red Cross Blood Bank", "Pune", "Maharashtra", 18.5204, 73.8567,
                    com.phcnet.blood.model.VerificationStatus.VERIFIED, "+91 20 2612 0000",
                    "pune.redcross@medflow.gov.in", "24x7 Emergency", 2000, true
            ));
            bankMap.put(1L, bb);
            for (String grp : List.of("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")) {
                bloodInventoryRepo.save(new com.phcnet.blood.model.BloodInventory(null, bb, grp, "WHOLE_BLOOD", 50, 5));
            }
        }
    }

    private void seedSampleBloodRequests(
            com.phcnet.blood.repository.EmergencyBloodRequestRepository bloodReqRepo,
            com.phcnet.blood.repository.BloodTransferRepository bloodTransferRepo,
            PhcRepository phcRepo,
            com.phcnet.blood.repository.BloodBankRepository bloodBankRepo) {
        if (bloodReqRepo.count() > 0) return;
        Phc samplePhc = phcRepo.findAll().stream().findFirst().orElse(null);
        com.phcnet.blood.model.BloodBank sourceBank = bloodBankRepo.findAll().stream().findFirst().orElse(null);
        if (samplePhc != null && sourceBank != null) {
            // 1. Critical postpartum emergency (MATCH_FOUND)
            bloodReqRepo.save(new com.phcnet.blood.model.EmergencyBloodRequest(
                    null, samplePhc, "O-", "WHOLE_BLOOD", 4,
                    com.phcnet.blood.model.RequestPriority.CRITICAL,
                    java.time.Instant.now().plus(java.time.Duration.ofMinutes(45)),
                    com.phcnet.blood.model.RequestStatus.MATCH_FOUND,
                    "Emergency Postpartum Hemorrhage - Immediate universal blood required", null
            ));

            // 2. High priority polytrauma (IN_TRANSIT) with an active transfer
            com.phcnet.blood.model.EmergencyBloodRequest transitReq = bloodReqRepo.save(new com.phcnet.blood.model.EmergencyBloodRequest(
                    null, samplePhc, "A+", "WHOLE_BLOOD", 3,
                    com.phcnet.blood.model.RequestPriority.HIGH,
                    java.time.Instant.now().plus(java.time.Duration.ofMinutes(90)),
                    com.phcnet.blood.model.RequestStatus.IN_TRANSIT,
                    "Polytrauma road accident - Siren cold-chain dispatch active", null
            ));

            com.phcnet.blood.model.BloodTransfer transfer = new com.phcnet.blood.model.BloodTransfer(
                    null, transitReq, sourceBank, samplePhc, 3, 14.2, 31, "IN_TRANSIT", true
            );
            transfer.setDispatchedAt(java.time.Instant.now().minus(java.time.Duration.ofMinutes(12)));
            bloodTransferRepo.save(transfer);

            // 3. Historical fulfilled emergency
            bloodReqRepo.save(new com.phcnet.blood.model.EmergencyBloodRequest(
                    null, samplePhc, "B+", "WHOLE_BLOOD", 2,
                    com.phcnet.blood.model.RequestPriority.MEDIUM,
                    java.time.Instant.now().minus(java.time.Duration.ofHours(3)),
                    com.phcnet.blood.model.RequestStatus.FULFILLED,
                    "Emergency scheduled pediatric transfusion - Transfusion completed", null
            ));
            log.info("Seeded initial live emergency blood requests and active siren transfer.");
        }
    }
}
