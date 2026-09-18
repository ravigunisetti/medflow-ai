package com.phcnet.blood.model;

public enum BloodGroup {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    private final String display;

    BloodGroup(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public static BloodGroup fromDisplay(String str) {
        if (str == null) return null;
        String clean = str.trim().toUpperCase().replace(" ", "");
        for (BloodGroup bg : values()) {
            if (bg.display.equalsIgnoreCase(clean) || bg.name().equalsIgnoreCase(clean)) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Unknown blood group: " + str);
    }
}
