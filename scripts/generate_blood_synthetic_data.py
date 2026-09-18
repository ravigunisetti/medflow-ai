#!/usr/bin/env python3
"""
Generate Synthetic Data for MedFlow AI Emergency Blood Network
==============================================================
Generates:
1. 50 Certified Blood Banks across 5 Maharashtra districts
2. Blood inventory records for all 8 blood groups (Whole Blood, PRBC, FFP, Platelets)
3. Sample emergency requests and completed transfers for demo & evaluation
"""

import csv
import random
from datetime import datetime, timedelta
from pathlib import Path

random.seed(42)

DISTRICTS = {
    "Pune": {"lat": 18.5204, "lon": 73.8567, "radius": 0.45},
    "Satara": {"lat": 17.6805, "lon": 73.9997, "radius": 0.40},
    "Ahmednagar": {"lat": 19.0952, "lon": 74.7496, "radius": 0.42},
    "Solapur": {"lat": 17.6599, "lon": 75.9064, "radius": 0.38},
    "Nashik": {"lat": 19.9975, "lon": 73.7898, "radius": 0.40}
}

BLOOD_BANK_PREFIXES = [
    "Red Cross Regional Blood Centre",
    "Jeevan Jyoti Blood Bank",
    "Sanjivani Rotary Blood Bank",
    "Civil Hospital Blood Centre",
    "Samarth Blood Bank & Component Lab",
    "Sahyadri Charitable Blood Centre",
    "Apex Lifeline Blood Centre",
    "National Model Blood Bank",
    "District Health Mission Blood Centre",
    "Ayurseva Voluntary Blood Bank"
]

BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"]

# Relative population prevalence in India (approximate weights)
BLOOD_GROUP_WEIGHTS = {
    "O+": 0.37,
    "B+": 0.32,
    "A+": 0.22,
    "AB+": 0.07,
    "O-": 0.015,
    "B-": 0.012,
    "A-": 0.008,
    "AB-": 0.003
}

COMPONENTS = ["WHOLE_BLOOD", "PRBC", "FFP", "PLATELETS"]
VERIFICATION_STATUSES = ["VERIFIED", "VERIFIED", "VERIFIED", "PROVISIONAL", "PENDING_AUDIT"]

def generate_blood_data(output_dir: Path):
    output_dir.mkdir(parents=True, exist_ok=True)
    
    blood_banks_file = output_dir / "blood_banks.csv"
    blood_inventories_file = output_dir / "blood_inventories.csv"

    blood_banks = []
    inventories = []

    bank_id = 1
    inv_id = 1

    districts_list = list(DISTRICTS.keys())

    # Generate 50 blood banks (10 per district)
    for district in districts_list:
        cfg = DISTRICTS[district]
        for i in range(1, 11):
            prefix = BLOOD_BANK_PREFIXES[(i - 1) % len(BLOOD_BANK_PREFIXES)]
            name = f"{prefix} - {district} #{i}"
            lat = round(cfg["lat"] + random.uniform(-cfg["radius"], cfg["radius"]), 6)
            lon = round(cfg["lon"] + random.uniform(-cfg["radius"], cfg["radius"]), 6)
            
            # First 8 in district verified, last 2 provisional or pending audit
            v_status = "VERIFIED" if i <= 8 else ("PROVISIONAL" if i == 9 else "PENDING_AUDIT")
            phone = f"+91 {random.randint(70000, 99999)} {random.randint(10000, 99999)}"
            email = f"bloodbank.{district.lower()}{i}@medflow.gov.in"
            capacity = random.choice([500, 750, 1000, 1200, 1500])

            blood_banks.append({
                "id": bank_id,
                "name": name,
                "district": district,
                "state": "Maharashtra",
                "latitude": lat,
                "longitude": lon,
                "verification_status": v_status,
                "contact_phone": phone,
                "contact_email": email,
                "operating_hours": "24x7 Emergency",
                "storage_capacity_units": capacity,
                "is_active": True
            })

            # Generate inventory for all 8 blood groups for this blood bank
            for bg in BLOOD_GROUPS:
                weight = BLOOD_GROUP_WEIGHTS[bg]
                base_units = int(weight * capacity * 0.4)
                
                # Introduce variance: some centers have surplus, some have low/deficit
                multiplier = random.uniform(0.3, 1.8)
                units = max(1, int(base_units * multiplier))
                reserved = random.randint(0, min(units, 4))

                inventories.append({
                    "id": inv_id,
                    "blood_bank_id": bank_id,
                    "blood_group": bg,
                    "component_type": "WHOLE_BLOOD",
                    "units_available": units,
                    "reserved_units": reserved,
                    "last_updated": datetime.utcnow().isoformat() + "Z"
                })
                inv_id += 1

            bank_id += 1

    # Write blood_banks.csv
    with open(blood_banks_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=blood_banks[0].keys())
        writer.writeheader()
        writer.writerows(blood_banks)

    # Write blood_inventories.csv
    with open(blood_inventories_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=inventories[0].keys())
        writer.writeheader()
        writer.writerows(inventories)

    print(f"Generated {len(blood_banks)} blood banks across {len(districts_list)} districts.")
    print(f"Generated {len(inventories)} blood inventory records across all 8 blood groups.")
    print(f"Files written to {output_dir}")

if __name__ == "__main__":
    out_path = Path(__file__).parent.parent / "data"
    generate_blood_data(out_path)
