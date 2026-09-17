# Phased Development Plan & Roadmap - PHC-NET AI

## 1. Project Philosophy & Engineering Methodology

This project adheres to rigorous production-grade software engineering standards:
- **Milestone Isolation**: Each milestone represents a discrete, independently testable layer of the system.
- **Verification Gates**: No code is accepted without passing linting, automated unit tests, and integration verification.
- **Fail-Fast Error Handling**: Unchecked runtime exceptions, silent fallbacks, or ungrounded mock responses are prohibited.
- **Measurable Scientific Rigor**: Machine learning and optimization algorithms are evaluated with verifiable benchmark metrics ($MAE, RMSE$, runtime latency, constraint satisfaction ratios).

---

## 2. Milestone Matrix & Detailed Roadmap

```mermaid
gantt
    title PHC-NET AI Implementation Roadmap (Milestones 0 - 15)
    dateFormat  YYYY-MM-DD
    section Foundation
    Milestone 0: Specs & Repo Setup       :done, m0, 2026-09-16, 1d
    Milestone 1: Database & Data Gen      :active, m1, after m0, 2d
    Milestone 2: Spring Boot Core APIs    :m2, after m1, 3d
    Milestone 3: React Command Dashboard  :m3, after m2, 3d
    section Core Engines
    Milestone 4: Deterministic Stock-out  :m4, after m3, 2d
    Milestone 5: ML Demand Forecasting    :m5, after m4, 3d
    Milestone 6: Redistribution Solver    :m6, after m5, 3d
    section AI & Event Architecture
    Milestone 7: Gemini Tool Agent        :m7, after m6, 3d
    Milestone 8: Pub/Sub Event Backbone   :m8, after m7, 2d
    Milestone 9: BigQuery Analytics       :m9, after m8, 2d
    section Scenarios & Enterprise
    Milestone 10: Emergency Simulator     :m10, after m9, 2d
    Milestone 11: RBAC & Auth Security    :m11, after m10, 2d
    section Cloud & Verification
    Milestone 12: Docker Environment      :m12, after m11, 2d
    Milestone 13: Google Cloud Deploy     :m13, after m12, 3d
    Milestone 14: Testing & AI Evaluation :m14, after m13, 2d
    Milestone 15: Polish & Final Demo     :m15, after m14, 2d
```

---

### Milestone 0: Project Specification & Architecture Setup (Current Milestone)
- **Goal**: Finalize architecture specs, domain models, database designs, roadmap, and initialize clean workspace scaffolding.
- **Deliverables**:
  - `/docs/architecture.md`
  - `/docs/product-requirements.md`
  - `/docs/database-design.md`
  - `/docs/development-plan.md`
  - `/README.md`
  - Root repository structure (`backend/`, `ml-service/`, `frontend/`, `scripts/`, `docs/`)
  - Configuration templates (`.env.example`, `.gitignore`)
- **Acceptance Criteria**:
  - All architecture documents approved.
  - Zero syntax/format errors in markdown and templates.
  - Repository skeleton ready for bootstrapping.

---

### Milestone 1: PostgreSQL Schema & Synthetic Data Generator
- **Goal**: Provision the local/containerized PostgreSQL database, run Flyway/DDL migrations, and develop an empirical synthetic data generator script in Python.
- **Deliverables**:
  - DDL schema files & Flyway migration scripts (`V1__initial_schema.sql`).
  - Python synthetic data engine (`scripts/generate_synthetic_data.py`) generating:
    - 100 realistic PHCs across 5 districts with geographic coordinates.
    - 20 essential drugs mapped to categories and safety stock minimums.
    - 365 days of correlated historical daily demand and patient footfall.
    - Controlled deficit and surplus clusters.
  - Seeder script writing directly to PostgreSQL and exporting CSV/JSON seeds.
- **Acceptance Criteria**:
  - Schema creates cleanly with all foreign keys, check constraints, and indexes.
  - Seeder populates $> 700,000$ historical records within 30 seconds.
  - Query benchmarks show $< 10\text{ ms}$ index lookups.

---

### Milestone 2: Spring Boot PHC, Medicine & Inventory Core APIs
- **Goal**: Implement standard RESTful CRUD and query services in Spring Boot 3 / Java 21 with DTOs, repository patterns, pagination, and OpenAPI Swagger documentation.
- **Deliverables**:
  - Entities, Repositories, Services, and Controllers for `PHC`, `Medicine`, `Inventory`, and `Footfall`.
  - Spring Data JPA with derived queries and custom `@Query` native optimizations.
  - Exception handling filter with RFC 7807 `ProblemDetail` JSON responses.
  - SpringDoc OpenAPI / Swagger UI exposed at `/swagger-ui.html`.
- **Endpoints**:
  - `GET /api/phcs`, `POST /api/phcs`, `GET /api/phcs/{id}`
  - `GET /api/medicines`, `POST /api/medicines`
  - `GET /api/inventory`, `POST /api/inventory`, `PUT /api/inventory/{id}`
  - `GET /api/footfall`
- **Acceptance Criteria**:
  - Unit and integration tests with `@SpringBootTest` and MockMvc passing $> 85\%$ line coverage.
  - Paginated responses with sorting support.

---

### Milestone 3: React Command Dashboard & Map Visualization
- **Goal**: Build the operational dashboard frontend in React 19, TypeScript, Vite, and Tailwind CSS.
- **Deliverables**:
  - Enterprise layout: Sidebar navigation, command header, network summary KPIs.
  - Interactive Map view (Leaflet / Mapbox GL) plotting PHCs color-coded by stock risk.
  - Filterable tables for inventory and facilities.
  - Recharts component displaying 30-day historical footfall and medicine consumption trends.
- **Acceptance Criteria**:
  - Client compiles with zero TypeScript errors (`tsc --noEmit`).
  - Fluid UI with loading skeletons and error boundary fallbacks.

---

### Milestone 4: Deterministic Stock-Out & Risk Engine
- **Goal**: Build the mathematical stock-out risk engine in Spring Boot without external AI dependencies.
- **Deliverables**:
  - Deterministic calculation service:
    $$\bar{D}_{\text{daily}} = \frac{1}{N}\sum D_i, \quad S_{\text{days}} = \frac{\text{Current Stock}}{\bar{D}_{\text{daily}}}$$
  - Configurable risk threshold service (CRITICAL, HIGH, MEDIUM, LOW).
  - Risk breakdown explanation payload: formula, variable values, safety stock ratio.
  - Endpoints: `GET /api/predictions/stockout`, `GET /api/alerts/critical`.
- **Acceptance Criteria**:
  - Precision unit tests covering boundary values (0 stock, 0 demand, negative protection).
  - Microsecond latency execution per PHC.

---

### Milestone 5: Python Demand Forecasting Service + Evaluation
- **Goal**: Build an independent FastAPI service for ML demand forecasting and evaluate performance against moving-average baselines.
- **Deliverables**:
  - FastAPI service structure in `ml-service/`.
  - Feature engineering pipeline: Day-of-week, month/season, 7-day lagged footfall, rolling 14-day demand averages.
  - Model comparison: 7-day Moving Average vs. HistGradientBoosting / Ridge Regressor.
  - Offline evaluation script generating real metrics ($MAE, RMSE, MAPE$) exported to `/docs/ml-evaluation-report.md`.
  - Endpoint: `POST /api/v1/forecast/predict-7day`.
- **Acceptance Criteria**:
  - ML model achieves statistically significant $MAE$ reduction over simple moving average on seasonal spike periods.
  - Structured response including model version and confidence interval bounds.

---

### Milestone 6: Redistribution Optimization Engine
- **Goal**: Implement the deterministic linear programming / greedy cost-minimization algorithm for inter-PHC stock redistribution.
- **Deliverables**:
  - Optimization solver in Spring Boot:
    - Haversine distance matrix between all PHC pairs.
    - Safety stock preservation constraint check.
    - Transit cost estimation model ($\text{Cost} = \text{Base} + \text{Dist} \times \text{Rate} \times \text{Volume}$).
  - Endpoints: `POST /api/transfers/optimize`, `GET /api/transfers/recommendations`.
- **Acceptance Criteria**:
  - Mathematical constraint verification: Zero donor PHCs drop below safety stock post-transfer.
  - Zero phantom transfers; deterministic reproducibility.

---

### Milestone 7: Gemini AI Agent (Tool Calling & Grounded Explanations)
- **Goal**: Build the intelligent conversational agent using the Google GenAI SDK with structured function/tool calling.
- **Deliverables**:
  - Gemini 2.5 Flash agent integration in `ml-service/`.
  - Tool declarations: `getPHCInventory`, `getStockoutRisk`, `findSurplusPHCs`, `calculateTransferRecommendation`, `getPatientFootfall`.
  - Structured output schemas (Pydantic models) rendering action cards in the UI.
  - Explanation endpoint: `POST /api/v1/agent/explain-risk` returning natural language rationale grounded strictly in retrieved parameters.
- **Acceptance Criteria**:
  - Unit test suite verifying agent calls correct tools for given user queries.
  - Hallucination guard: Any missing metric triggers an explicit "metric unavailable" response.

---

### Milestone 8: Google Cloud Pub/Sub Event-Driven Processing
- **Goal**: Decouple inventory mutations from risk recalculation using asynchronous event publishing and consumption.
- **Deliverables**:
  - Google Cloud Pub/Sub emulator/client configuration in Spring Boot.
  - Events: `INVENTORY_UPDATED`, `ALERT_RAISED`, `TRANSFER_REQUESTED`.
  - Consumer listener with idempotent message handling and dead-letter retry logic.
- **Acceptance Criteria**:
  - Sub-50ms API response to client on inventory update while background subscriber handles risk recalculation.
  - Zero message loss during simulated consumer restarts.

---

### Milestone 9: BigQuery Analytics Pipeline
- **Goal**: Create analytical pipeline for longitudinal epidemiological and logistics reporting.
- **Deliverables**:
  - BigQuery schema definitions and export jobs (via Spring batch or direct streaming).
  - Analytical queries answering:
    - District vulnerability index.
    - Seasonal demand surge timelines.
    - Transfer efficiency and prevented stock-out metrics.
  - Backend reporting endpoint: `GET /api/analytics/district-summary`.
- **Acceptance Criteria**:
  - Analytical queries return aggregated summaries without placing load on the OLTP PostgreSQL database.

---

### Milestone 10: Emergency Simulation Engine
- **Goal**: Deliver a comprehensive disaster response simulation feature for district health leadership.
- **Deliverables**:
  - Simulation engine accepting scenario parameters (e.g., Dengue Epidemic: $+60\%$ footfall, $+120\%$ demand for Paracetamol/IV fluids; Flood: logistics distance penalty $+50\%$).
  - Sandbox mode: Runs without corrupting actual operational records.
  - Before vs. After comparison UI displaying newly critical PHCs and instant rebalancing plan.
- **Acceptance Criteria**:
  - Deterministic reproducibility given the same scenario seed.
  - Complete execution under 1.5 seconds.

---

### Milestone 11: Authentication, RBAC & Enterprise Security
- **Goal**: Secure all APIs with JWT authentication and granular role-based permissions.
- **Deliverables**:
  - Spring Security configuration with stateless JWT token generation and validation.
  - User roles: `ROLE_ADMIN`, `ROLE_DISTRICT_OFFICER`, `ROLE_PHC_MANAGER`.
  - Method-level security (`@PreAuthorize`) preventing PHC managers from updating other facilities.
- **Acceptance Criteria**:
  - Automated integration tests verifying 401 Unauthorized and 403 Forbidden on boundary violations.

---

### Milestone 12: Docker & Production-Like Local Environment
- **Goal**: Package all components into optimized container images orchestrated with Docker Compose.
- **Deliverables**:
  - Multi-stage Dockerfiles:
    - `backend/Dockerfile` (Eclipse Temurin 21 JRE, non-root user).
    - `ml-service/Dockerfile` (Python slim, poetry/pip dependencies).
    - `frontend/Dockerfile` (Node build to Nginx Alpine reverse proxy).
  - `docker-compose.yml` orchestrating PostgreSQL, Redis, Pub/Sub emulator, backend, ML service, and frontend.
- **Acceptance Criteria**:
  - Single command `docker compose up` starts the complete environment with automated health checks.

---

### Milestone 13: Google Cloud Deployment (Cloud Run, Cloud SQL)
- **Goal**: Document and script deployment to Google Cloud serverless architecture.
- **Deliverables**:
  - Terraform / Cloud Shell scripts deploying:
    - Cloud SQL (PostgreSQL).
    - Cloud Run services (Frontend, Spring Boot backend, FastAPI ML service).
    - Pub/Sub topics and BigQuery datasets.
  - Workload Identity & IAM role policies (zero service account keys in git).
- **Acceptance Criteria**:
  - Fully functional public/staging URL on Cloud Run with HTTPS.

---

### Milestone 14: Automated Testing, Observability & AI Evaluation
- **Goal**: Execute end-to-end testing, logging verification, and formal AI hallucination evaluation.
- **Deliverables**:
  - End-to-end integration tests using MockMvc and Playwright/Cypress.
  - Structured JSON logging verification across all services.
  - AI Evaluation Benchmark: 50 representative clinical supply-chain queries evaluating tool selection accuracy, factual grounding, and zero hallucination.
- **Acceptance Criteria**:
  - Evaluation report showing $100\%$ tool invocation accuracy and $0\%$ numerical hallucination rate.

---

### Milestone 15: UI Polish, Demo Scenarios & Final Documentation
- **Goal**: Polish UX, record demo flows, and assemble portfolio presentation materials.
- **Deliverables**:
  - Polished Command Center UI with responsive dark/light theme, emergency warning banners, and exportable PDF transfer orders.
  - Step-by-step interactive demo guide for hackathon judges and hiring managers.
  - Complete GitHub README with architectural badges, architecture diagrams, and screencasts.
- **Acceptance Criteria**:
  - Seamless 5-minute live demo execution from cold start to emergency simulation and Gemini AI explanation.
