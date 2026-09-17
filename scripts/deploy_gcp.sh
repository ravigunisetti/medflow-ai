#!/usr/bin/env bash
# ==============================================================================
# PHC-NET AI — Automated Google Cloud Platform (GCP) Deployment Script
# Region: asia-south1 (Mumbai)
# ==============================================================================

set -euo pipefail

PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project)}"
REGION="asia-south1"
REPO_NAME="phcnet-repo"
INSTANCE_NAME="phcnet-db-primary"
DB_NAME="phcnet_db"

echo "=========================================================="
echo "🚀 Initiating PHC-NET AI GCP Deployment for project: ${PROJECT_ID}"
echo "📍 Deployment Region: ${REGION}"
echo "=========================================================="

# 1. Enable Required GCP APIs
echo "Step 1: Enabling Cloud APIs..."
gcloud services enable \
    run.googleapis.com \
    sqladmin.googleapis.com \
    artifactregistry.googleapis.com \
    secretmanager.googleapis.com \
    pubsub.googleapis.com \
    bigquery.googleapis.com \
    cloudbuild.googleapis.com

# 2. Setup Artifact Registry Repository
echo "Step 2: Checking Artifact Registry..."
if ! gcloud artifacts repositories describe "${REPO_NAME}" --location="${REGION}" &>/dev/null; then
    echo "Creating Artifact Registry repository: ${REPO_NAME}..."
    gcloud artifacts repositories create "${REPO_NAME}" \
        --repository-format=docker \
        --location="${REGION}" \
        --description="PHC-NET AI Production Container Images"
fi

# Configure Docker credentials
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# 3. Setup Pub/Sub Topics and Subscriptions
echo "Step 3: Configuring Google Cloud Pub/Sub topics..."
gcloud pubsub topics create phc-inventory-updates || true
gcloud pubsub topics create phc-alerts-raised || true

gcloud pubsub subscriptions create phc-inventory-processor-sub \
    --topic=phc-inventory-updates \
    --ack-deadline=60 || true

# 4. Setup Cloud SQL (PostgreSQL 16)
echo "Step 4: Checking Cloud SQL PostgreSQL instance..."
if ! gcloud sql instances describe "${INSTANCE_NAME}" &>/dev/null; then
    echo "Provisioning Cloud SQL instance: ${INSTANCE_NAME} (High-Availability Tier)..."
    gcloud sql instances create "${INSTANCE_NAME}" \
        --database-version=POSTGRES_16 \
        --tier=db-custom-2-7680 \
        --region="${REGION}" \
        --availability-type=REGIONAL \
        --storage-type=SSD \
        --storage-size=20GB \
        --storage-auto-increase \
        --backup-start-time=02:00
fi

# 5. Build and Push Container Images
echo "Step 5: Building and Pushing Container Images..."

BACKEND_IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/phcnet-backend:latest"
ML_IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/phcnet-ml-service:latest"
FRONTEND_IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/phcnet-frontend:latest"

echo "Building Backend..."
docker build -t "${BACKEND_IMAGE}" ./backend
docker push "${BACKEND_IMAGE}"

echo "Building ML Service..."
docker build -t "${ML_IMAGE}" ./ml-service
docker push "${ML_IMAGE}"

echo "Building Frontend..."
docker build -t "${FRONTEND_IMAGE}" ./frontend
docker push "${FRONTEND_IMAGE}"

# 6. Deploy Services to Cloud Run
echo "Step 6: Deploying Cloud Run Services..."

gcloud run deploy phcnet-backend \
    --image="${BACKEND_IMAGE}" \
    --region="${REGION}" \
    --platform=managed \
    --allow-unauthenticated \
    --port=8080 \
    --memory=2Gi \
    --cpu=2 \
    --add-cloudsql-instances="${PROJECT_ID}:${REGION}:${INSTANCE_NAME}" \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,SPRING_DATASOURCE_URL=jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${PROJECT_ID}:${REGION}:${INSTANCE_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"

BACKEND_URL=$(gcloud run services describe phcnet-backend --region="${REGION}" --format='value(status.url)')

gcloud run deploy phcnet-ml-service \
    --image="${ML_IMAGE}" \
    --region="${REGION}" \
    --platform=managed \
    --allow-unauthenticated \
    --port=8000 \
    --memory=2Gi \
    --cpu=2 \
    --set-env-vars="BACKEND_URL=${BACKEND_URL},ENVIRONMENT=production"

ML_URL=$(gcloud run services describe phcnet-ml-service --region="${REGION}" --format='value(status.url)')

gcloud run deploy phcnet-frontend \
    --image="${FRONTEND_IMAGE}" \
    --region="${REGION}" \
    --platform=managed \
    --allow-unauthenticated \
    --port=80 \
    --memory=512Mi \
    --cpu=1

FRONTEND_URL=$(gcloud run services describe phcnet-frontend --region="${REGION}" --format='value(status.url)')

echo "=========================================================="
echo "🎉 Deployment Completed Successfully!"
echo "📱 Frontend URL:   ${FRONTEND_URL}"
echo "⚡ Backend API:    ${BACKEND_URL}"
echo "🤖 ML Service:     ${ML_URL}"
echo "=========================================================="
