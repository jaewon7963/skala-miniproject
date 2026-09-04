#!/bin/bash
# BizXray 기능 스모크 테스트.
#
#   docker compose up -d --build   # 스택을 먼저 띄운다
#   ./scripts/smoke-test.sh
#
# 모든 호출이 nginx(5173)를 거치므로 프록시 설정까지 함께 검증된다.
# 데모 계정(A·E)은 읽기만 하고, 쓰기 흐름(B)은 매번 새 임시 계정으로 돌린 뒤 탈퇴시킨다.
# 그래서 시연 상태는 이 스크립트를 몇 번 돌려도 그대로 남는다.
BASE=${BASE:-http://localhost:5173/api}
PDF="$(cd "$(dirname "$0")/.." && pwd)/docs/samples/bizxray-demo-plan.pdf"
PASS=0; FAIL=0
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
J() { python3 -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }

ok()   { PASS=$((PASS+1)); printf "  \033[32m✓\033[0m %-52s %s\n" "$1" "$2"; }
bad()  { FAIL=$((FAIL+1)); printf "  \033[31m✗\033[0m %-52s %s\n" "$1" "$2"; }
check(){ if [ $# -ne 3 ]; then bad "$1" "검사 인자가 $# 개 — 따옴표가 깨졌다"; return; fi; \
         if [ "$2" = "$3" ]; then ok "$1" "$2"; else bad "$1" "받음=$2 기대=$3"; fi; }

# HTTP 상태코드만
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

echo
echo "━━━ A. 데모 계정 — 읽기 전용 (시연 상태를 건드리지 않는다) ━━━"

check "GET /health" "$(code $BASE/health)" "200"

LOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"kim@company.com","password":"logic1234"}')
TOK=$(echo "$LOGIN" | J 'd["token"]')
[ -n "$TOK" ] && ok "POST /auth/login (데모 계정)" "$(echo "$LOGIN" | J 'd["user"]["email"]')" || bad "POST /auth/login" "토큰 없음"
AUTH="Authorization: Bearer $TOK"

check "GET /auth/me" "$(curl -s -H "$AUTH" $BASE/auth/me | J 'd["organization"]')" "company.com"
check "GET /tags (기준 데이터 7건)" "$(curl -s -H "$AUTH" $BASE/tags | J 'len(d)')" "7"

# 데모 계정은 사람이 실제로 쓰는 계정이다. 문서를 더 올리거나 검토를 진행했을 수 있으므로
# "문서 몇 건" 같은 전제를 두지 않고, 시드된 문서를 이름으로 찾아 그것만 확인한다.
# 검토 항목 개수처럼 사람 손을 타는 값은 B(임시 계정)에서 정확히 본다.
DOCS=$(curl -s -H "$AUTH" $BASE/documents)
DEMO_ID=$(echo "$DOCS" | J '[i["id"] for i in d["items"] if i["name"].startswith("AI 매장 안내 로봇")][0]')
if [ -n "$DEMO_ID" ]; then ok "시드된 시연 문서를 찾음" "id=$DEMO_ID"; else bad "시드된 시연 문서" "문서함에 없음"; fi

check "GET /documents?q=로봇 (검색)" "$(curl -s -H "$AUTH" "$BASE/documents?q=%EB%A1%9C%EB%B4%87" | J 'str(d["total"] >= 1)')" "True"
check "GET /documents?q=없는말 (검색 0건)" "$(curl -s -H "$AUTH" "$BASE/documents?q=zzzznope" | J 'd["total"]')" "0"
check "GET /documents/\$DEMO_ID — 페이지 수" "$(curl -s -H "$AUTH" $BASE/documents/$DEMO_ID | J 'd["pageCount"]')" "8"
check "GET /documents/\$DEMO_ID" "$(curl -s -H "$AUTH" $BASE/documents/$DEMO_ID | J 'd["name"]')" "AI 매장 안내 로봇 사업계획서"
check "GET /documents/$DEMO_ID/parse-status" "$(curl -s -H "$AUTH" $BASE/documents/$DEMO_ID/parse-status | J 'd["parseStatus"]')" "DONE"
check "GET /documents/$DEMO_ID/sections (목차 8)" "$(curl -s -H "$AUTH" $BASE/documents/$DEMO_ID/sections | J 'len(d)')" "8"
check "GET /documents/$DEMO_ID/pages (본문 8쪽)" "$(curl -s -H "$AUTH" $BASE/documents/$DEMO_ID/pages | J 'len(d)')" "8"
check "GET /documents/$DEMO_ID/file (PDF 원본)" "$(curl -s -o /dev/null -w '%{http_code} %{size_download}' -H "$AUTH" $BASE/documents/$DEMO_ID/file)" "200 53478"

JOB=$(curl -s -H "$AUTH" $BASE/documents/$DEMO_ID/review-jobs/latest)
JID=$(echo "$JOB" | J 'd["id"]')
check "GET /documents/$DEMO_ID/review-jobs/latest" "$(echo "$JOB" | J 'd["status"]')" "DONE"
check "  검토 항목이 5건 이상 남아 있음" "$(echo "$JOB" | J 'str(d["summary"]["total"] >= 5)')" "True"
check "  세 유형이 모두 나옴" "$(echo "$JOB" | J 'str(all(d["summary"]["byType"].get(k,0) > 0 for k in ("ERROR","NEEDS_CHECK","NO_EVIDENCE")))')" "True"

check "GET /review-jobs/$JID/findings" "$(curl -s -H "$AUTH" $BASE/review-jobs/$JID/findings | J 'str(len(d) >= 5)')" "True"
check "GET /review-jobs/$JID/pages" "$(curl -s -H "$AUTH" $BASE/review-jobs/$JID/pages | J 'len(d)')" "8"

# 근거 앵커가 실제 원문 블록을 가리키는지 (화면 하이라이트 점프의 조건)
ANCHORS=$(curl -s -H "$AUTH" $BASE/review-jobs/$JID/pages > $TMP/pages.json; \
  curl -s -H "$AUTH" $BASE/review-jobs/$JID/findings > $TMP/find.json; \
  python3 - "$TMP" <<'PY'
import json,sys
t=sys.argv[1]
pages=json.load(open(t+'/pages.json')); finds=json.load(open(t+'/find.json'))
ids={b['id'] for p in pages for b in p.get('blocks',[])}
miss=[e['anchorId'] for f in finds for e in f.get('evidence',[]) if e['anchorId'] not in ids]
print(len(miss))
PY
)
check "근거 앵커 ↔ 원문 블록 정합성 (끊긴 앵커 수)" "$ANCHORS" "0"

ASK=$(curl -s -X POST $BASE/review-jobs/$JID/questions -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"question":"표 합계가 왜 맞지 않나요?"}')
check "POST /review-jobs/$JID/questions (AI 질의응답)" "$(echo "$ASK" | J 'str(d["promotable"])')" "True"
check "  답변이 실제 검토 항목을 근거로 함" "$(echo "$ASK" | J 'd["evidences"][0]["anchorId"]')" "b-6-2"

echo
echo "━━━ B. 임시 계정 — 업로드부터 리포트까지 전체 쓰기 흐름 ━━━"

EMAIL="smoke$(date +%s)@test.com"
SIGN=$(curl -s -X POST $BASE/auth/signup -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"smoke1234\",\"agreeTerms\":true,\"agreePrivacy\":true}")
TOK2=$(echo "$SIGN" | J 'd["token"]')
[ -n "$TOK2" ] && ok "POST /auth/signup (임시 계정)" "$EMAIL" || bad "POST /auth/signup" "$(echo $SIGN|head -c 120)"
AUTH2="Authorization: Bearer $TOK2"

UP=$(curl -s -X POST $BASE/documents -H "$AUTH2" -F "file=@$PDF")
DID=$(echo "$UP" | J 'd["id"]')
[ -n "$DID" ] && ok "POST /documents (PDF 업로드)" "id=$DID · $(echo "$UP" | J 'd["status"]')" || bad "POST /documents" "$(echo $UP|head -c 120)"

for i in $(seq 1 40); do
  PS=$(curl -s -H "$AUTH2" $BASE/documents/$DID/parse-status | J 'd["parseStatus"]')
  [ "$PS" = "DONE" ] && break; sleep 1
done
check "  파싱 완료까지 폴링" "$PS" "DONE"
check "  파싱 결과 페이지 수" "$(curl -s -H "$AUTH2" $BASE/documents/$DID | J 'd["pageCount"]')" "8"

NJ=$(curl -s -X POST $BASE/review-jobs -H "$AUTH2" -H 'Content-Type: application/json' -d "{\"documentId\":\"$DID\"}")
JID2=$(echo "$NJ" | J 'd["id"]')
[ -n "$JID2" ] && ok "POST /review-jobs (분석 시작)" "job=$JID2 · $(echo "$NJ" | J 'd["status"]')" || bad "POST /review-jobs" "$(echo $NJ|head -c 120)"

SEEN_RUNNING=no
for i in $(seq 1 60); do
  R=$(curl -s -H "$AUTH2" $BASE/review-jobs/$JID2)
  ST=$(echo "$R" | J 'd["status"]')
  [ "$ST" = "RUNNING" ] && SEEN_RUNNING=yes
  [ "$ST" = "DONE" ] || [ "$ST" = "FAILED" ] && break
  sleep 1
done
check "  진행 중(RUNNING) 상태가 화면에 노출됨" "$SEEN_RUNNING" "yes"
check "  분석 완료까지 폴링" "$ST" "DONE"
check "  진행률 100%" "$(echo "$R" | J 'str(d["parseProgress"])+"/"+str(d["analyzeProgress"])')" "100/100"
check "  검토 항목 재현성 (매번 같은 5건)" "$(echo "$R" | J 'd["summary"]["total"]')" "5"

F1=$(curl -s -H "$AUTH2" $BASE/review-jobs/$JID2/findings | J 'd[0]["id"]')
F2=$(curl -s -H "$AUTH2" $BASE/review-jobs/$JID2/findings | J 'd[1]["id"]')
check "PATCH findings/$F1/verdict (검토 반영)" \
  "$(curl -s -X PATCH $BASE/review-jobs/$JID2/findings/$F1/verdict -H "$AUTH2" -H 'Content-Type: application/json' -d '{"verdict":"ACCEPTED"}' | J 'd["verdict"]')" "ACCEPTED"
check "PATCH findings/$F2/verdict (오류 아님)" \
  "$(curl -s -X PATCH $BASE/review-jobs/$JID2/findings/$F2/verdict -H "$AUTH2" -H 'Content-Type: application/json' -d '{"verdict":"REJECTED"}' | J 'd["verdict"]')" "REJECTED"

MAN=$(curl -s -X POST $BASE/review-jobs/$JID2/findings -H "$AUTH2" -H 'Content-Type: application/json' \
  -d '{"type":"NEEDS_CHECK","method":"MANUAL","title":"사람이 직접 추가한 항목","description":"원문에서 직접 지정한 검토 항목","evidence":[{"anchorId":"b-1-4","page":1,"label":"직접 선택"}]}')
check "POST /review-jobs/$JID2/findings (수동 추가)" "$(echo "$MAN" | J 'd["method"]')" "MANUAL"
check "  총 6건으로 증가" "$(curl -s -H "$AUTH2" $BASE/review-jobs/$JID2/findings | J 'len(d)')" "6"

TAGID=$(curl -s -H "$AUTH2" $BASE/tags | J 'd[0]["id"]')
TAGBODY=$(printf '{"tagIds":["%s"]}' "$TAGID")
check "PATCH /documents/$DID (태그 지정)" \
  "$(curl -s -X PATCH $BASE/documents/$DID -H "$AUTH2" -H 'Content-Type: application/json' -d "$TAGBODY" | J 'd["tags"][0]')" "AI"
check "PATCH /documents/$DID/name (이름 변경)" \
  "$(curl -s -X PATCH $BASE/documents/$DID/name -H "$AUTH2" -H 'Content-Type: application/json' -d '{"name":"이름 바꾼 사업계획서"}' | J 'd["name"]')" "이름 바꾼 사업계획서"

CP=$(curl -s -X POST $BASE/review-jobs/$JID2/complete -H "$AUTH2")
check "POST /review-jobs/$JID2/complete (검토 완료)" "$(echo "$CP" | J '"완료시각 있음" if d["completedAt"] else "없음"')" "완료시각 있음"
check "  라이브러리 배지 DONE 으로 전환" "$(curl -s -H "$AUTH2" $BASE/documents/$DID | J 'd["status"]')" "DONE"

REP=$(curl -s -H "$AUTH2" $BASE/review-jobs/$JID2/report)
check "GET /review-jobs/$JID2/report (리포트)" "$(echo "$REP" | J 'str(d["summary"]["total"])')" "6"
check "  채택/반려 집계" "$(echo "$REP" | J 'str(d["summary"]["accepted"])+"/"+str(d["summary"]["rejected"])')" "1/1"

echo
echo "━━━ C. 오류 처리 ━━━"
check "잘못된 비밀번호 → 401" "$(code -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"email":"kim@company.com","password":"wrong"}')" "401"
check "토큰 없이 문서 목록 → 401" "$(code $BASE/documents)" "401"
check "남의 문서 조회 → 403" "$(code -H "$AUTH2" $BASE/documents/1)" "403"
check "없는 문서 조회 → 404" "$(code -H "$AUTH2" $BASE/documents/99999)" "404"
check "같은 파일 재업로드 → 409" "$(code -X POST $BASE/documents -H "$AUTH2" -F "file=@$PDF")" "409"
check "PDF 아닌 파일 업로드 → 415" "$(printf 'not a pdf' > $TMP/not-a.pdf; code -X POST $BASE/documents -H "$AUTH2" -F "file=@$TMP/not-a.pdf")" "415"
check "짧은 비밀번호로 가입 → 422" "$(code -X POST $BASE/auth/signup -H 'Content-Type: application/json' -d '{"email":"short@test.com","password":"ab1","agreeTerms":true,"agreePrivacy":true}')" "422"
check "이미 쓰는 이메일로 가입 → 409" "$(code -X POST $BASE/auth/signup -H 'Content-Type: application/json' -d '{"email":"kim@company.com","password":"logic1234","agreeTerms":true,"agreePrivacy":true}')" "409"

echo
echo "━━━ D. 계정 관리 · 뒷정리 ━━━"
check "DELETE /documents/$DID (소프트 삭제)" "$(code -X DELETE $BASE/documents/$DID -H "$AUTH2")" "204"
check "  목록에서 사라짐" "$(curl -s -H "$AUTH2" $BASE/documents | J 'd["total"]')" "0"
check "PATCH /auth/me/password (비밀번호 변경)" "$(curl -s -X PATCH $BASE/auth/me/password -H "$AUTH2" -H 'Content-Type: application/json' -d '{"currentPassword":"smoke1234","newPassword":"smoke5678"}' | J 'str(d["ok"])')" "True"
NEWPW=$(printf '{"email":"%s","password":"smoke5678"}' "$EMAIL")
OLDPW=$(printf '{"email":"%s","password":"smoke1234"}' "$EMAIL")
check "  바뀐 비밀번호로 로그인" "$(code -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "$NEWPW")" "200"
check "  옛 비밀번호는 막힘" "$(code -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "$OLDPW")" "401"
TOK3=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"smoke5678\"}" | J 'd["token"]')
check "POST /auth/logout" "$(code -X POST $BASE/auth/logout -H "Authorization: Bearer $TOK3")" "200"
check "DELETE /auth/me (임시 계정 탈퇴)" "$(code -X DELETE $BASE/auth/me -H "Authorization: Bearer $TOK3" -H 'Content-Type: application/json' -d '{"password":"smoke5678"}')" "200"

echo
echo "━━━ E. 데모 계정이 손상되지 않았는지 ━━━"
FIN=$(curl -s -H "$AUTH" $BASE/documents/$DEMO_ID/review-jobs/latest)
check "시연 문서 분석 결과 살아 있음" "$(echo "$FIN" | J 'd["status"]')" "DONE"
check "시연 문서 여전히 조회됨" "$(code -H "$AUTH" $BASE/documents/$DEMO_ID)" "200"

echo
printf "════ 통과 \033[32m%d\033[0m · 실패 \033[31m%d\033[0m ════\n" $PASS $FAIL
exit $FAIL
