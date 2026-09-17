# Product Requirements Document (PRD) - PHC-NET AI

## 1. Problem Statement

India's rural and semi-urban primary healthcare delivery relies on an interconnected network of Primary Health Centres (PHCs). However, the supply chain suffers from fundamental systemic challenges:

1. **Information Asymmetry & Lag**: PHC inventory and patient logs are typically maintained in physical registers or siloed batch systems, leading to delayed visibility at the district level.
2. **Predictable yet Unmitigated Stock-Outs**: Seasonal infectious surges (vector-borne illnesses in monsoon, respiratory spikes in winter) exhaust life-saving medicines (e.g., ORS, Paracetamol, Amoxicillin, Chloroquine, Anti-venom) unexpectedly.
3. **Co-located Surplus Waste**: When one PHC faces a zero-stock crisis, another PHC 15 km away often holds 6 months of excess stock nearing expiration due to static, rigid quota-based allocation.
4. **Lack of Explainable AI Assistance**: Frontline healthcare workers and district medical officers are hesitant to adopt opaque "black-box" recommendations without grounded, interpretable reasoning.

---

## 2. Target Users & Personas

### Persona 1: Dr. Rajesh Sharma - District Health Officer (DHO)
- **Role**: Oversees 40 PHCs in a district.
- **Pain Point**: Spends hours on phone calls verifying which PHCs are out of anti-rabies serum or antibiotics during an outbreak. Needs an instant bird's-eye command dashboard with automated cross-allocation recommendations.
- **Primary Use Case**: Reviewing district-wide risk levels, validating system-generated transfer plans, and running emergency contingency simulations.

### Persona 2: Sunita Devi - PHC Staff Nurse & Pharmacist
- **Role**: Manages daily patient registration, dispensing, and medicine store at a remote PHC.
- **Pain Point**: Tedious manual registers, sudden stock depletion, fear of punitive action for stock-outs or expired medicine write-offs.
- **Primary Use Case**: Simple daily dispensing logging, receiving push alerts when safety stock is breached, and one-click acceptance of incoming stock transfers.

### Persona 3: State Health Directorate Analyst
- **Role**: Allocates state-level annual procurement budgets and sets emergency protocols.
- **Pain Point**: Lack of empirical forecasting; reliance on historical annual averages that fail to anticipate localized epidemics.
- **Primary Use Case**: Analyzing long-term trends via BigQuery, auditing transfer costs, and testing resilience under hypothetical emergency surges.

---

## 3. Functional Requirements (FR)

### FR-1: PHC & Master Data Management
- The system shall register PHCs with attributes: unique ID, name, district, state, latitude, longitude, and population served.
- The system shall maintain an Essential Medicine List (EML) with attributes: unique ID, generic name, category, dosage form, unit, safety stock minimum threshold, and shelf-life months.

### FR-2: Daily Inventory & Footfall Logging
- PHC staff shall log daily physical dispensing or stock receipts.
- PHC staff shall log daily total patient OPD footfall.
- The backend shall validate positive quantities and record `updated_at` timestamps.

### FR-3: Deterministic Stock-Out & Days-Remaining Engine
- For every PHC and medicine pair, the system shall compute:
  $$\text{Days to Stock-out} = \frac{\text{Current Stock}}{\text{Average Daily Demand}}$$
- The system shall assign risk categories based on configurable thresholds:
  - `< 3 days`: `CRITICAL`
  - `3–7 days`: `HIGH`
  - `7–14 days`: `MEDIUM`
  - `> 14 days`: `LOW`
- The system shall output the mathematical derivation alongside every risk rating.

### FR-4: Machine Learning Demand Forecasting
- A standalone ML service shall consume historical demand and footfall data to predict rolling 7-day demand vectors.
- The service must report verifiable validation metrics ($MAE$, $RMSE$) against a simple moving-average baseline.
- Model metadata (version, training window, error metrics) must be explicitly persisted with predictions.

### FR-5: Cross-PHC Redistribution Optimization Engine
- When a PHC drops into `CRITICAL` or `HIGH` risk, the engine shall identify donor PHCs possessing surplus stock.
- The engine must enforce hard constraints:
  - Source PHC retains its own safety stock buffer: $\text{Quantity}_{\text{source}} \ge \text{SafetyStock} + \text{Demand}_{\text{leadtime}}$.
  - Transfer quantity must be greater than zero and must not exceed destination shortage.
  - Geographical distance must be within a configurable threshold (e.g., $\le 50\text{ km}$).
  - Expired or near-expiry batches (shelf-life $< 30$ days) cannot be transferred.
- Compute transfer logistics cost estimation based on inter-PHC Haversine distance and transit tier.

### FR-6: Gemini AI Reasoning & Tool-Calling Agent
- Users can query the platform in natural language (e.g., *"Which PHCs in Pune district need urgent anti-malarial restocking?"* or *"Why is PHC 102 flagged as high risk?"*).
- The AI Agent must use **tool/function calling** to query the backend REST endpoints (`getPHCInventory`, `getStockoutRisk`, `findSurplusPHCs`, `getDemandForecast`).
- Gemini must **never** fabricate stock counts or days to stock-out. If an endpoint returns empty or null, the agent must output a clear disclaimer that data is unavailable.
- Structured JSON output must accompany agent responses for direct UI visualization.

### FR-7: Emergency Outbreak Simulation
- An interactive simulation sandbox enabling DHOs and admins to inject synthetic emergency scenarios:
  - Epidemic Footfall Surge ($+10\%$ to $+200\%$)
  - Disease-Specific Demand Spike ($+50\%$ to $+500\%$ on targeted medicine categories)
  - Supply Cutoff / Vendor Delay ($0$ external replenishment for $N$ days)
- The simulation must run deterministically via seed parameters, recalculate risks immediately, and generate a proposed cross-district rebalancing plan with before-and-after vulnerability matrices.

### FR-8: Command Center Dashboard & Map Visualization
- An operations dashboard displaying real-time metrics:
  - Total PHCs Monitored, Critical Stock-outs, Predicted Shortages (7-day horizon), Pending Transfers, Network Health Index.
- Interactive Map showing geo-located PHCs color-coded by risk status (`Green = LOW`, `Amber = MEDIUM`, `Orange = HIGH`, `Red = CRITICAL`).
- Side-panel drilldown on marker click showing inventory breakdown, demand trend chart, and active transfer alerts.

### FR-9: Transfer Workflow & Human-in-the-Loop Governance
- Stock transfers proposed by the optimization engine remain in `PENDING_APPROVAL` status until explicitly reviewed by an authorized officer.
- Authorized users can `APPROVE`, `REJECT` (with mandatory reason), or `ADJUST` quantity.
- Approved transfers update dispatch and receipt audit logs.

### FR-10: Event-Driven Processing (Pub/Sub)
- Core state mutations (e.g., inventory deduction, transfer approval) publish structured JSON events to Google Cloud Pub/Sub topics (`phc.inventory.updated`, `phc.transfer.status`).
- Dedicated background worker services consume events to trigger async risk recalculation and notification dispatch without blocking user HTTP requests.

### FR-11: Analytical Telemetry & BigQuery Warehouse
- Anonymized historical snapshots of demand, footfall, risk scores, and transfers are streamed or bulk-loaded to BigQuery.
- Powers longitudinal reporting: Seasonal demand curves, stock-out prevention rates, and cost-benefit ratios of redistribution vs. emergency reorders.

### FR-12: System Health & Observability
- Real-time status monitoring for all micro-components (Backend, ML Service, PostgreSQL, Redis, Pub/Sub, Gemini Gateway).
- Live latency and throughput counters (P50/P95/P99 latency, event queue depth, failed tool calls).

---

## 4. Non-Functional Requirements (NFR)

| Dimension | Specification |
|---|---|
| **Response Latency** | REST APIs $< 100\text{ ms}$ (P95); Deterministic stock-out & transfer optimization $< 250\text{ ms}$; Gemini reasoning pipeline $< 3.5\text{ s}$ total roundtrip. |
| **Availability** | 99.9% uptime target for operational endpoints; offline-tolerant data entry client cache for unstable rural connectivity. |
| **Scalability** | Capable of supporting 5,000+ PHCs, 100+ medicines, and 10,000 daily inventory transactions without architectural changes. |
| **Security & RBAC** | Stateless JWT tokens with HTTPS/TLS 1.3 encryption. Role-based endpoint guards ensuring PHC managers can only modify their assigned facility. |
| **Data Privacy** | Zero patient PII collection; strictly aggregate epidemiological and logistical counters. |
| **Grounding & Faithfulness** | Zero hallucinated arithmetic. Any recommendation generated by Gemini must cite the exact deterministic calculation results retrieved via tool calls. |

---

## 5. Explicit Anti-Requirements (What We Will NOT Do)

1. **No Fake AI Wrapper**: We will not have Gemini simulate fake inventory numbers or invent transfer quantities. All data originates from PostgreSQL or the optimization solver.
2. **No Ungrounded Medical Claims**: The AI assistant will not diagnose patients or prescribe dosages. It is strictly a supply-chain and logistical assistant.
3. **No Fabricated Performance Claims**: Model accuracy ($MAE$, $RMSE$) must be evaluated on real synthetic test splits and reported transparently.
4. **No Premature Kubernetes**: The prototype runs on clean Docker Compose locally and serverless Google Cloud Run in production.
