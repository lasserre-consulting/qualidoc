#!/usr/bin/env bash
# deploy.sh — Build, push et déploiement complet sur Kubernetes
# Usage : ./deploy.sh [registry]

set -euo pipefail

REGISTRY="${1:-registry.qualidoc.fr}"

BACKEND_IMAGE="$REGISTRY/qualidoc-backend:latest"
FRONTEND_IMAGE="$REGISTRY/qualidoc-frontend:latest"

echo "=== QualiDoc — Déploiement K8s ==="
echo "Registry : $REGISTRY"
echo ""

# ── 1. Build et push backend ──────────────────────────────────────────────────
echo "[1/4] Build image backend..."
docker build \
  -f ../qualidoc-backend/Dockerfile \
  -t "$BACKEND_IMAGE" \
  ../qualidoc-backend

echo "[2/4] Push image backend..."
docker push "$BACKEND_IMAGE"

# ── 2. Build et push frontend ─────────────────────────────────────────────────
echo "[3/4] Build image frontend..."
docker build \
  -f ../qualidoc-frontend/Dockerfile \
  -t "$FRONTEND_IMAGE" \
  ../qualidoc-frontend

echo "[4/4] Push image frontend..."
docker push "$FRONTEND_IMAGE"

# ── 3. Apply des manifestes et rollout restart ────────────────────────────────
echo ""
echo "[5/5] Déploiement sur Kubernetes..."
kubectl apply -k .
kubectl rollout restart deployment/qualidoc-backend -n qualidoc
kubectl rollout restart deployment/qualidoc-frontend -n qualidoc

# ── 4. Attendre que les pods soient prêts ─────────────────────────────────────
echo ""
echo "Attente déploiement backend..."
kubectl rollout status deployment/qualidoc-backend -n qualidoc --timeout=300s

echo "Attente déploiement frontend..."
kubectl rollout status deployment/qualidoc-frontend -n qualidoc --timeout=120s

echo ""
echo "=== Déploiement terminé avec succès ! ==="
echo ""
kubectl get pods -n qualidoc
