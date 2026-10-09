#!/usr/bin/env bash
# Smoke test executado pelo pipeline logo apos cada deploy.
# Uso: ./scripts/smoke-test.sh <url-base> <ambiente-esperado>
#   ex: ./scripts/smoke-test.sh http://localhost:8081 staging
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
AMBIENTE_ESPERADO="${2:-local}"
TENTATIVAS=60

ok()    { echo "  [OK]   $*"; }
falha() { echo "  [FALHA] $*"; exit 1; }

echo "== Smoke test em $BASE_URL (ambiente esperado: $AMBIENTE_ESPERADO) =="

echo "1. Aguardando a API ficar saudavel..."
for i in $(seq 1 "$TENTATIVAS"); do
  status=$(curl -fs "$BASE_URL/actuator/health" | jq -r '.status' 2>/dev/null || true)
  if [ "$status" = "UP" ]; then
    ok "health = UP (tentativa $i)"
    break
  fi
  [ "$i" -eq "$TENTATIVAS" ] && falha "API nao ficou UP em $((TENTATIVAS * 2))s"
  sleep 2
done
curl -fs "$BASE_URL/actuator/health" | jq '{status, mongo: .components.mongo.status}'

echo "2. Conferindo se o ambiente implantado e o correto..."
info=$(curl -fs "$BASE_URL/actuator/info")
echo "$info" | jq .
ambiente=$(echo "$info" | jq -r '.app.ambiente')
[ "$ambiente" = "$AMBIENTE_ESPERADO" ] || falha "ambiente '$ambiente' diferente do esperado '$AMBIENTE_ESPERADO'"
ok "ambiente = $ambiente, versao = $(echo "$info" | jq -r '.app.versao')"

echo "3. Login com usuario de teste..."
token=$(curl -fs -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"operador@inovagab.com","senha":"123456"}' | jq -r '.token')
[ -n "$token" ] && [ "$token" != "null" ] || falha "login nao retornou token"
ok "token JWT recebido"

echo "4. Endpoint autenticado..."
perfil=$(curl -fs "$BASE_URL/api/auth/me" -H "Authorization: Bearer $token" | jq -r '.perfil')
[ "$perfil" = "OPERADOR" ] || falha "/api/auth/me retornou perfil '$perfil'"
ok "/api/auth/me -> perfil OPERADOR"

qtd=$(curl -fs "$BASE_URL/api/orientacoes" -H "Authorization: Bearer $token" | jq 'length')
ok "/api/orientacoes -> $qtd orientacao(oes) (leitura no MongoDB)"

echo "5. Seguranca: requisicao sem token deve ser bloqueada..."
codigo=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/ideias")
[ "$codigo" = "401" ] || falha "esperado 401 sem token, recebido $codigo"
ok "/api/ideias sem token -> 401"

echo "== Smoke test concluido com sucesso em $AMBIENTE_ESPERADO =="
