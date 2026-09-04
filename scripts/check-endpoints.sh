#!/bin/bash
# 프런트가 부르는 경로와 백엔드가 실제로 제공하는 경로를 대조한다.
#
#   docker compose up -d --build     # 스택을 먼저 띄운다
#   ./scripts/check-endpoints.sh
#
# 백엔드 쪽 진실 원본은 springdoc 이 만들어 주는 /v3/api-docs 다. 사람이 옮겨 적은 목록이
# 아니라 실제로 매핑된 것만 들어 있어서, 컨트롤러를 고치면 이 대조도 자동으로 따라온다.
#
# 프런트 쪽은 endpoints.js 하나만 보면 될 것 같지만 그렇지 않다. reviews.getPages 처럼
# EP 상수를 안 쓰고 문자열로 조립한 자리가 있어서 modules/*.js 까지 훑어야 한다.
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
API_DOCS=${API_DOCS:-http://localhost:8081/v3/api-docs}
BASE=${BASE:-http://localhost:5173/api}
FE_DIR="$ROOT/FE/business/src/api"

TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

if ! curl -fsS "$API_DOCS" -o "$TMP/api-docs.json" 2>/dev/null; then
  echo "✗ $API_DOCS 를 읽지 못했습니다. 백엔드가 떠 있는지 확인하세요 (docker compose up -d backend)"
  exit 1
fi

python3 - "$TMP" "$FE_DIR" <<'PY'
import json, re, sys, pathlib

tmp, fe_dir = sys.argv[1], pathlib.Path(sys.argv[2])

GREEN, RED, YELLOW, DIM, OFF = "\033[32m", "\033[31m", "\033[33m", "\033[2m", "\033[0m"

# ── 백엔드: /v3/api-docs 에 실제로 매핑된 것 ────────────────────────────────
doc = json.load(open(f"{tmp}/api-docs.json"))
def norm(path):
    """경로 파라미터 이름이 달라도 같은 자리로 보도록 {} 로 통일한다."""
    return re.sub(r"\{[^}]+\}", "{}", path)

be = {(m.upper(), norm(p))
      for p, ops in doc["paths"].items()
      for m in ops if m in ("get", "post", "put", "patch", "delete")}

# ── 프런트: endpoints.js 의 EP 정의 + modules 의 실제 호출 ──────────────────
ep_src = (fe_dir / "endpoints.js").read_text(encoding="utf-8")

# 'auth: { login: "/auth/login", me: (id) => `/x/${id}` }' 형태를 키 → 경로로 편다
ep = {}
group = None
for line in ep_src.splitlines():
    g = re.match(r"\s*(\w+):\s*\{\s*$", line)
    if g:
        group = g.group(1); continue
    m = re.match(r"\s*(\w+):\s*(?:\([^)]*\)\s*=>\s*)?[`'\"]([^`'\"]+)[`'\"]", line)
    if m and group:
        path = re.sub(r"\$\{[^}]+\}", "{}", m.group(2))
        ep[f"{group}.{m.group(1)}"] = path

# modules/*.js 에서 http.<메서드>(EP.x.y ...) / http.upload(...) / 문자열 조립 호출을 찾는다
calls = []          # (메서드, 경로, 출처)
unresolved = []
for f in sorted((fe_dir / "modules").glob("*.js")):
    src = f.read_text(encoding="utf-8")
    # 인자는 세 가지 모양이다: 백틱 템플릿 / EP.x.y 또는 EP.x.y(...) / 그 밖의 토큰.
    # 단순히 [^,)]+ 로 자르면 EP.reviews.job(jobId) 의 닫는 괄호에서 끊겨 버린다.
    call_re = r"http\.(get|post|put|patch|delete|upload)\(\s*(`[^`]*`|EP\.\w+\.\w+(?:\([^)]*\))?|[^,)]+)"
    for m in re.finditer(call_re, src):
        method, arg = m.group(1).upper(), m.group(2).strip()
        if method == "UPLOAD":
            method = "POST"
        line_no = src[:m.start()].count("\n") + 1
        src_ref = f"{f.name}:{line_no}"

        key = re.match(r"EP\.(\w+)\.(\w+)", arg)
        if key and f"{key.group(1)}.{key.group(2)}" in ep:
            calls.append((method, ep[f"{key.group(1)}.{key.group(2)}"], src_ref))
            continue
        # `${EP.reviews.job(jobId)}/pages` 처럼 조립한 경우
        tpl = re.match(r"`\$\{EP\.(\w+)\.(\w+)\([^)]*\)\}([^`]*)`", arg)
        if tpl and f"{tpl.group(1)}.{tpl.group(2)}" in ep:
            calls.append((method, ep[f"{tpl.group(1)}.{tpl.group(2)}"] + tpl.group(3), src_ref))
            continue
        unresolved.append((method, arg[:48], src_ref))

fe = {(m, "/api" + p) for m, p, _ in calls}
src_of = {(m, "/api" + p): s for m, p, s in calls}

# ── 대조 ────────────────────────────────────────────────────────────────
only_fe = sorted(fe - be)
both    = sorted(fe & be)
only_be = sorted(be - fe)

print()
print("━━━ 프런트가 부르는데 백엔드에 없음 (404 위험) ━━━")
if only_fe:
    for m, p in only_fe:
        print(f"  {RED}✗{OFF} {m:<7} {p}   {DIM}{src_of.get((m,p),'')}{OFF}")
else:
    print(f"  {GREEN}없음 — 프런트 호출 {len(both)}건이 모두 백엔드와 일치합니다{OFF}")

print()
print("━━━ 양쪽 일치 ━━━")
for m, p in both:
    print(f"  {GREEN}✓{OFF} {m:<7} {p}")

print()
print("━━━ 백엔드에만 있음 (프런트 미사용) ━━━")
for m, p in only_be:
    print(f"  {YELLOW}·{OFF} {m:<7} {p}")

if unresolved:
    print()
    print("━━━ 경로를 정적으로 못 읽은 호출 (직접 확인 필요) ━━━")
    for m, arg, s in unresolved:
        print(f"  {YELLOW}?{OFF} {m:<7} {arg}   {DIM}{s}{OFF}")

json.dump([[m, p] for m, p in both], open(f"{tmp}/both.json", "w"))
json.dump([[m, p] for m, p in only_fe], open(f"{tmp}/only_fe.json", "w"))
print()
print(f"요약  일치 {len(both)}  ·  프런트만 {len(only_fe)}  ·  백엔드만 {len(only_be)}  ·  미해석 {len(unresolved)}")
PY

# ── 실제 호출: 경로는 맞는데 메서드가 틀린 경우(405)를 잡는다 ─────────────────
echo
echo "━━━ 실제 호출 확인 (데모 계정) ━━━"
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"kim@company.com","password":"logic1234"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin).get("token",""))' 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "  데모 계정 로그인 실패 — 실제 호출 확인은 건너뜁니다"
  exit 0
fi

DOC=$(curl -s "$BASE/documents" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import sys,json;i=json.load(sys.stdin)["items"];print(i[0]["id"] if i else "")' 2>/dev/null)
JOB=$(curl -s "$BASE/documents/$DOC/review-jobs/latest" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin).get("id",""))' 2>/dev/null)

hit() { # 메서드 경로 기대코드들…
  local method=$1 path=$2; shift 2
  local codes="$*"
  local got
  got=$(curl -s -o /dev/null -w '%{http_code}' -X "$method" "$BASE$path" -H "Authorization: Bearer $TOKEN")
  if echo "$codes" | grep -qw "$got"; then
    printf "  \033[32m✓\033[0m %-7s %-52s %s\n" "$method" "$path" "$got"
  else
    printf "  \033[31m✗\033[0m %-7s %-52s %s (기대 %s)\n" "$method" "$path" "$got" "$codes"
  fi
}

hit GET  /auth/me                                   200
hit GET  /tags                                      200
hit GET  /documents                                 200
hit GET  "/documents/$DOC"                          200
hit GET  "/documents/$DOC/review-jobs/latest"       200
hit GET  "/review-jobs/$JOB"                        200
hit GET  "/review-jobs/$JOB/sections"               200
hit GET  "/review-jobs/$JOB/pages"                  200
hit GET  "/review-jobs/$JOB/findings"               200
hit GET  "/review-jobs/$JOB/report"                 200 409
echo
echo "  (쓰기 계열은 상태를 바꾸므로 여기서 부르지 않는다 — scripts/smoke-test.sh 가 담당)"
