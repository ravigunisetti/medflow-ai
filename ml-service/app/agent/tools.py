"""
Backend Tools Registry for Gemini AI Agent
==========================================
Executes authenticated REST calls to the Spring Boot backend to retrieve
ground truth facts for the Gemini reasoning agent.
"""

import httpx
from app.config import settings

def get_phc_inventory(phc_id: int):
    """Retrieves current stock levels, safety stock thresholds, and batch numbers for a PHC facility."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(f"{settings.backend_url}/api/inventory/phc/{phc_id}")
            if res.status_code == 200:
                data = res.json().get("data", [])
                return [
                    {
                        "medicine": item["medicineName"],
                        "code": item["medicineCode"],
                        "stock": item["quantity"],
                        "safetyStock": item["safetyStock"],
                        "unit": item["unit"],
                        "expiry": item["expiryDate"],
                    }
                    for item in data
                ]
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return {"error": "Facility inventory not found"}

def get_stockout_risk(phc_id: int, medicine_id: int):
    """Retrieves deterministic days-to-stockout, risk classification, and mathematical derivation."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(
                f"{settings.backend_url}/api/predictions/explain",
                params={"phcId": phc_id, "medicineId": medicine_id}
            )
            if res.status_code == 200:
                return res.json().get("data", {})
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return {"error": "Risk data unavailable"}

def find_surplus_phcs(medicine_id: int):
    """Identifies nearby facilities holding excess stock above their mandatory safety retention buffer."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(
                f"{settings.backend_url}/api/inventory",
                params={"medicineId": medicine_id, "size": 50}
            )
            if res.status_code == 200:
                items = res.json().get("data", {}).get("content", [])
                surplus = [
                    {
                        "phcId": i["phcId"],
                        "phcName": i["phcName"],
                        "district": i["district"],
                        "currentStock": i["quantity"],
                        "safetyStock": i["safetyStock"],
                        "surplusUnits": max(0, i["quantity"] - (i["safetyStock"] * 2)),
                    }
                    for i in items if i["quantity"] > (i["safetyStock"] * 1.5)
                ]
                surplus.sort(key=lambda x: x["surplusUnits"], reverse=True)
                return surplus[:5]
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return []

def calculate_transfer_recommendation(medicine_id: int, target_district: str = None):
    """Runs the optimization engine to calculate feasible inter-PHC transfers with distance and cost."""
    try:
        with httpx.Client(timeout=5.0) as client:
            res = client.post(
                f"{settings.backend_url}/api/optimization/redistribute",
                json={"medicineId": medicine_id, "district": target_district, "maxDistanceKm": 80.0}
            )
            if res.status_code == 200:
                return res.json().get("data", [])[:5]
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return []

def get_patient_footfall(phc_id: int, days: int = 14):
    """Retrieves historical daily patient OPD visits for the specified PHC."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(
                f"{settings.backend_url}/api/footfall/history",
                params={"phcId": phc_id, "days": days}
            )
            if res.status_code == 200:
                data = res.json().get("data", [])
                return [
                    {"date": item["recordDate"], "patients": item["patientCount"], "emergencies": item["emergencyCount"]}
                    for item in data[-days:]
                ]
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return []

def get_district_summary():
    """Retrieves district-level vulnerability indices, total PHCs, and network health score."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(f"{settings.backend_url}/api/dashboard/summary")
            if res.status_code == 200:
                return res.json().get("data", {})
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return {}

def get_active_alerts():
    """Retrieves all active critical and high-risk medicine shortages across the entire network."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(f"{settings.backend_url}/api/alerts")
            if res.status_code == 200:
                return res.json().get("data", [])
    except Exception as e:
        return []
    return []

# ==========================================
# Emergency Blood Network Tools
# ==========================================

def get_emergency_blood_requests():
    """Retrieves all active and historical emergency blood requisitions in the network."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(f"{settings.backend_url}/api/blood/requests")
            if res.status_code == 200:
                return res.json().get("data", [])
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return []

def get_blood_dashboard_summary():
    """Retrieves real-time blood network summary including active blood banks and inventory by blood group."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(f"{settings.backend_url}/api/blood/dashboard/summary")
            if res.status_code == 200:
                return res.json().get("data", {})
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return {}

def find_compatible_blood_resources(request_id: int):
    """Executes the deterministic matching engine for an emergency blood requisition."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.get(f"{settings.backend_url}/api/blood/requests/{request_id}/matches")
            if res.status_code == 200:
                return res.json().get("data", {})
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return {}

def simulate_blood_emergency(blood_group: str, units_required: int, deadline_minutes: int = 60, scenario_type: str = "MASS_CASUALTY_ACCIDENT"):
    """Runs the emergency blood simulation sandbox to identify optimal sources and route ETAs."""
    try:
        with httpx.Client(timeout=4.0) as client:
            res = client.post(
                f"{settings.backend_url}/api/blood/simulate",
                json={
                    "bloodGroup": blood_group,
                    "unitsRequired": units_required,
                    "deadlineMinutes": deadline_minutes,
                    "scenarioType": scenario_type,
                    "priority": "CRITICAL"
                }
            )
            if res.status_code == 200:
                return res.json().get("data", {})
    except Exception as e:
        return {"error": f"Backend communication error: {str(e)}"}
    return {}

