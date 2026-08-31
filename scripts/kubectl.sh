#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

NS="ecommerce"

CLEAN=0
if [ "${1:-}" = "--clean" ]; then
  CLEAN=1
fi

SERVICES=(api-gateway cart-service discovery-service frontend-service notification-service order-service payment-service product-service user-service)

if [ "$CLEAN" -eq 1 ]; then
  echo ">>> Meglevo deploymentek torlese: ${SERVICES[*]}"
  kubectl delete deployment "${SERVICES[@]}" -n "$NS" --ignore-not-found=true
fi

echo ">>> (1) Namespace"
kubectl apply -f k8s/00-namespace.yaml

echo ">>> (2) MySQL (ConfigMap + PVC + Deployment + Service)"
kubectl apply -f k8s/03-mysql.yaml

# Olyan sorrendben deployolunk, hogy a fuggosegek (db, discovery) hamarabb keszen legyenek.
for svc in discovery-service user-service product-service cart-service order-service payment-service notification-service api-gateway frontend-service; do
  echo ">>> deploy: $svc"
  kubectl apply -R -f "k8s/$svc/"
  kubectl rollout status deployment/"$svc" -n "$NS" --timeout=300s || echo "!! WARN: $svc rollout nem fejezodott be 300s alatt"
done

echo
echo ">>> KESZ - deploymentek allapota:"
kubectl get deployments -n "$NS"
echo
kubectl get pods -n "$NS"
