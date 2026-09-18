package com.phcnet.blood.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class BloodCompatibilityService {

    private static final Map<String, List<String>> RECIPIENT_COMPATIBILITY_MAP = new HashMap<>();

    static {
        RECIPIENT_COMPATIBILITY_MAP.put("O-", List.of("O-"));
        RECIPIENT_COMPATIBILITY_MAP.put("O+", List.of("O+", "O-"));
        RECIPIENT_COMPATIBILITY_MAP.put("A-", List.of("A-", "O-"));
        RECIPIENT_COMPATIBILITY_MAP.put("A+", List.of("A+", "A-", "O+", "O-"));
        RECIPIENT_COMPATIBILITY_MAP.put("B-", List.of("B-", "O-"));
        RECIPIENT_COMPATIBILITY_MAP.put("B+", List.of("B+", "B-", "O+", "O-"));
        RECIPIENT_COMPATIBILITY_MAP.put("AB-", List.of("AB-", "A-", "B-", "O-"));
        RECIPIENT_COMPATIBILITY_MAP.put("AB+", List.of("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"));
    }

    public List<String> getCompatibleDonorGroups(String recipientGroup) {
        if (recipientGroup == null) return Collections.emptyList();
        String normalized = recipientGroup.trim().toUpperCase();
        return RECIPIENT_COMPATIBILITY_MAP.getOrDefault(normalized, List.of(normalized));
    }

    public boolean isCompatible(String donorGroup, String recipientGroup) {
        if (donorGroup == null || recipientGroup == null) return false;
        List<String> compatibleDonors = getCompatibleDonorGroups(recipientGroup);
        return compatibleDonors.contains(donorGroup.trim().toUpperCase());
    }

    public boolean isUniversalDonor(String donorGroup) {
        return "O-".equalsIgnoreCase(donorGroup != null ? donorGroup.trim() : "");
    }

    public boolean isUniversalRecipient(String recipientGroup) {
        return "AB+".equalsIgnoreCase(recipientGroup != null ? recipientGroup.trim() : "");
    }
}
