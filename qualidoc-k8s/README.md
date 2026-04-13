# QualiDoc — Déploiement Kubernetes

## Prérequis sur Ubuntu

```bash
# 1. Installer kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# 2. Installer k3s (cluster K8s léger, idéal pour 1 serveur ou dev local)
curl -sfL https://get.k3s.io | sh -
sudo chmod 644 /etc/rancher/k3s/k3s.yaml
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

# Ou pour du dev local : installer minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
minikube start --memory=6g --cpus=4

# 3. Installer Nginx Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

# 4. Installer cert-manager (TLS automatique Let's Encrypt)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
```

## Structure des manifestes

```
qualidoc-k8s/
├── kustomization.yaml          ← Point d'entrée unique
├── deploy.sh                   ← Script de déploiement
├── Dockerfile.backend          ← Image Kotlin/Spring Boot
├── Dockerfile.frontend         ← Image Angular/Nginx
├── nginx.conf                  ← Config Nginx pour SPA
├── namespace/
│   └── namespace.yaml          ← Namespace qualidoc
├── config/
│   ├── configmap.yaml          ← Variables non sensibles
│   └── secrets.yaml            ← Credentials (à chiffrer !)
├── postgres/                   ← StatefulSet + PVC 10 Go
├── minio/                      ← StatefulSet + PVC 50 Go
├── elasticsearch/              ← StatefulSet + PVC 20 Go
├── backend/                    ← Deployment + HPA (2→8 replicas)
├── frontend/                   ← Deployment (2 replicas)
├── ingress/                    ← Routing HTTP/HTTPS
└── monitoring/                 ← ServiceMonitor Prometheus
```

## Déploiement initial

```bash
# 0. Adapter les secrets AVANT de déployer
nano config/secrets.yaml       # Remplacer les CHANGE_ME_IN_PRODUCTION

# 1. Appliquer tous les manifestes d'un coup
kubectl apply -k .

# 2. Vérifier l'état des pods
kubectl get pods -n qualidoc -w

# 3. Vérifier les services
kubectl get svc -n qualidoc
```

## Déploiement d'une nouvelle version

### Via CI/CD (GitHub Actions)

Un push sur `main` déclenche automatiquement : build → push GHCR → deploy K8s.

Pour déclencher manuellement :
```bash
gh workflow run cd-deploy.yml
```

### Via le script local

```bash
# Avec registry et tag par défaut (registry.qualidoc.fr + hash git court)
chmod +x deploy.sh
./deploy.sh

# En spécifiant registry et tag
./deploy.sh registry.qualidoc.fr 1.2.0
```

Le script exécute en séquence :
1. Build image backend (`Dockerfile.backend`)
2. Push image backend
3. Build image frontend (`Dockerfile.frontend`)
4. Push image frontend
5. `kubectl apply -k .` + attente du rollout

### Commandes individuelles

**Build des images :**
```bash
# Backend (depuis qualidoc-k8s/)
docker build -f Dockerfile.backend -t registry.qualidoc.fr/qualidoc-backend:TAG ../qualidoc-backend

# Frontend
docker build -f Dockerfile.frontend -t registry.qualidoc.fr/qualidoc-frontend:TAG ../qualidoc-frontend
```

**Mettre à jour uniquement l'image d'un déploiement :**
```bash
kubectl set image deployment/qualidoc-backend \
  backend=registry.qualidoc.fr/qualidoc-backend:TAG -n qualidoc

kubectl set image deployment/qualidoc-frontend \
  frontend=registry.qualidoc.fr/qualidoc-frontend:TAG -n qualidoc
```

**Vérifier le rollout :**
```bash
kubectl rollout status deployment/qualidoc-frontend -n qualidoc
kubectl rollout status deployment/qualidoc-backend -n qualidoc
```

**Rollback si échec :**
```bash
kubectl rollout undo deployment/qualidoc-frontend -n qualidoc
kubectl rollout undo deployment/qualidoc-backend -n qualidoc
```

## Commandes utiles au quotidien

```bash
# Logs du backend (suivi en temps réel)
kubectl logs -f deployment/qualidoc-backend -n qualidoc

# Logs d'un pod spécifique
kubectl logs -f <nom-du-pod> -n qualidoc

# Accéder au shell d'un pod backend
kubectl exec -it deployment/qualidoc-backend -n qualidoc -- /bin/sh

# Voir l'état du HPA (autoscaling)
kubectl get hpa -n qualidoc

# Forcer un redéploiement (sans changer l'image)
kubectl rollout restart deployment/qualidoc-backend -n qualidoc

# Annuler le dernier déploiement
kubectl rollout undo deployment/qualidoc-backend -n qualidoc

# Voir les événements (diagnostic)
kubectl get events -n qualidoc --sort-by='.lastTimestamp'
```

## Accès en développement local (minikube)

```bash
# Exposer le backend localement
kubectl port-forward svc/backend-service 8080:8080 -n qualidoc

# Exposer le frontend localement
kubectl port-forward svc/frontend-service 4200:80 -n qualidoc

# Ouvrir le dashboard Kubernetes
minikube dashboard
```

## Sécurisation des secrets en production

Ne jamais committer `secrets.yaml` avec de vraies valeurs. Options recommandées :

- **Sealed Secrets** : chiffrement asymétrique, safe à committer
  ```bash
  kubeseal --format yaml < secrets.yaml > secrets.sealed.yaml
  ```
- **HashiCorp Vault** : gestion centralisée des secrets
- **External Secrets Operator** : synchronise depuis AWS Secrets Manager / GCP Secret Manager

## Ressources nécessaires (estimation)

| Service | RAM min | CPU min | Stockage |
|---|---|---|---|
| PostgreSQL | 256 Mi | 250m | 10 Gi |
| MinIO | 256 Mi | 250m | 50 Gi |
| Elasticsearch | 1 Gi | 500m | 20 Gi |
| Backend (×2) | 512 Mi | 500m | — |
| Frontend (×2) | 64 Mi | 100m | — |
| **Total** | **~3 Gi** | **~2.5 CPU** | **80 Gi** |
