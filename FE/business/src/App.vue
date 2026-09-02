<script setup>
import { computed, onMounted, ref } from 'vue'

const sections = [
  { title:'사업 개요', page:1 }, { title:'문제 정의', page:3 }, { title:'솔루션', page:6 },
  { title:'기술 개발 계획', page:8 }, { title:'시장 분석', page:10 }, { title:'사업화 전략', page:11 },
  { title:'매출 계획', page:16 }, { title:'인력 및 예산', page:17 }, { title:'성과 지표', page:19 },
]
const fallback = [
  { id:'f-101', type:'numeric_conflict', severity:'high', status:'unreviewed', title:'2027년 예상 매출이 서로 다릅니다', description:'시장 진입 계획은 18억 원, 매출 계획은 24억 원으로 작성되어 있습니다.', confidence:.98, evidence:[{page:11,text:'2027년 예상 매출 18억 원',topPercent:42},{page:16,text:'2027년 매출 목표 24억 원',topPercent:68}] },
  { id:'f-102', type:'calculation', severity:'high', status:'unreviewed', title:'인건비 합계가 산출식과 맞지 않습니다', description:'인원 × 참여기간 × 월 단가의 재계산 결과와 기재 금액이 다릅니다.', confidence:.97, evidence:[{page:14,text:'5명 × 8개월 × 월 450만 원',topPercent:36},{page:17,text:'총 인건비 2억 400만 원',topPercent:51}] },
  { id:'f-103', type:'tech_kpi', severity:'medium', status:'unreviewed', title:'실시간 분석 목표와 기술 구성을 확인해야 합니다', description:'0.5초 응답 목표에 비해 대형 비전 모델 3종을 순차 실행하며 처리량 근거가 없습니다.', confidence:.84, evidence:[{page:8,text:'대형 비전 모델 3종 순차 처리',topPercent:45},{page:19,text:'평균 응답시간 0.5초 이내',topPercent:61}] },
  { id:'f-104', type:'evidence_missing', severity:'medium', status:'unreviewed', title:'정확도 KPI의 평가 방법이 없습니다', description:'정확도 95%의 시험 데이터 규모와 측정 지표가 정의되지 않았습니다.', confidence:.91, evidence:[{page:19,text:'객체 인식 정확도 95%',topPercent:30}] },
  { id:'f-105', type:'business_conflict', severity:'low', status:'unreviewed', title:'목표 고객과 판매 채널이 일치하지 않습니다', description:'최종 사용자는 개인 고객이지만 판매 계획은 기업 직접 영업만 제시합니다.', confidence:.79, evidence:[{page:5,text:'목표 고객: 20~30대 개인 사용자',topPercent:25},{page:12,text:'대기업 구매팀 직접 영업',topPercent:72}] },
]

const findings = ref(fallback)
const active = ref(fallback[0])
const page = ref(11)
const zoom = ref(82)
const showEvidence = ref(true)
const fileInput = ref()
const filter = ref('all')
const search = ref('')
const searchInput = ref()
const history = ref([])
const toast = ref('')
const loading = ref(false)
const progress = ref(100)
const rightTab = ref('results')
const modal = ref('')
const question = ref('')
const chat = ref([])
const railView = ref('document')
const filename = ref('AI 매장 안내 로봇 사업계획서.pdf')

const typeLabel = { numeric_conflict:'수치 불일치', calculation:'계산 오류', tech_kpi:'기술·KPI', evidence_missing:'근거 누락', business_conflict:'항목 간 모순' }
const statusLabel = { unreviewed:'미확인', confirmed:'확인', dismissed:'기각', pending:'보류', revision_requested:'수정 요청' }
const visibleFindings = computed(() => findings.value.filter(f => {
  const matchesStatus = filter.value === 'all' || (filter.value === 'processed' ? f.status !== 'unreviewed' : f.status === filter.value)
  return matchesStatus && (!search.value || `${f.title} ${f.description} ${typeLabel[f.type]}`.includes(search.value))
}))
const reviewed = computed(() => findings.value.filter(f => f.status !== 'unreviewed').length)
const pageTitle = computed(() => sections.slice().reverse().find(section => page.value >= section.page)?.title || '사업 개요')

function notice(message) { toast.value = message; setTimeout(() => toast.value = '', 2200) }
function goPage(target) { page.value = Math.min(21, Math.max(1, Number(target) || 1)); document.querySelector('.document')?.scrollTo({top:0,behavior:'smooth'}) }

async function loadFindings() {
  try {
    const response = await fetch('/api/documents/demo/findings')
    if (!response.ok) throw new Error()
    findings.value = await response.json(); active.value = findings.value[0]; goPage(active.value.evidence[0].page)
  } catch { notice('백엔드 연결 전: 샘플 데이터로 실행 중입니다.') }
}

async function loadHistory(id) {
  try { const response = await fetch(`/api/findings/${id}/history`); history.value = response.ok ? await response.json() : [] }
  catch { history.value = [] }
}

function selectFinding(finding, evidence = finding.evidence[0]) {
  active.value = finding; goPage(evidence.page); loadHistory(finding.id)
  const position = evidence.topPercent ?? 0
  requestAnimationFrame(() => document.querySelector('.document')?.scrollTo({top:Math.max(0, position * 4),behavior:'smooth'}))
}

async function decide(status) {
  const updated = { ...active.value, status }
  findings.value = findings.value.map(f => f.id === updated.id ? updated : f); active.value = updated
  try {
    const response = await fetch(`/api/findings/${updated.id}/decision`, {method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({status,comment:`${statusLabel[status]} 처리`})})
    if (response.ok) { active.value = await response.json(); findings.value = findings.value.map(f => f.id === updated.id ? active.value : f) }
    await loadHistory(updated.id)
  } catch { history.value.unshift({actor:'김대현',action:'decision.changed',before:'unreviewed',after:status,createdAt:new Date().toLocaleString('ko-KR')}) }
  notice(`${statusLabel[status]}으로 처리했습니다.`)
}

async function upload(event) {
  const file = event.target.files[0]; if (!file) return
  if (!file.name.toLowerCase().endsWith('.pdf')) return notice('PDF 파일만 업로드할 수 있습니다.')
  loading.value = true; progress.value = 15; filename.value = file.name
  const timer = setInterval(() => progress.value = Math.min(88, progress.value + 12), 180)
  const form = new FormData(); form.append('file', file)
  try { const response = await fetch('/api/documents',{method:'POST',body:form}); if (!response.ok) throw new Error(); await loadFindings(); progress.value = 100; notice(`${file.name} 분석을 완료했습니다.`) }
  catch { notice('업로드에 실패했습니다. 백엔드 상태를 확인하세요.') }
  finally { clearInterval(timer); loading.value = false; event.target.value = '' }
}

function ask() {
  const text = question.value.trim(); if (!text) return
  chat.value.push({role:'user',text}); question.value = ''
  const answer = text.includes('KPI') || text.includes('기술') ? '응답시간 0.5초 목표에 대한 처리량 시험 근거와 정확도 95%의 평가 데이터 정의가 필요합니다.' : text.includes('매출') ? '2027년 매출이 p.11에서는 18억 원, p.16에서는 24억 원으로 충돌합니다.' : '관련 근거를 찾았습니다. 검토 결과 탭에서 원문 위치와 비교할 수 있습니다.'
  setTimeout(() => chat.value.push({role:'assistant',text:answer}), 250)
}

function exportCsv() {
  const rows = [['유형','중요도','상태','내용','페이지'],...findings.value.map(f => [typeLabel[f.type],f.severity,statusLabel[f.status],f.title,f.evidence.map(e=>e.page).join('/')])]
  const blob = new Blob(['\ufeff'+rows.map(row=>row.map(v=>`"${String(v).replaceAll('"','""')}"`).join(',')).join('\n')],{type:'text/csv;charset=utf-8'})
  const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href=url; link.download='logiccheck-review.csv'; link.click(); URL.revokeObjectURL(url); notice('검토 결과를 내려받았습니다.')
}

function focusSearch() { searchInput.value?.focus() }
function copyShare() { navigator.clipboard?.writeText(location.href); notice('현재 화면 링크를 복사했습니다.') }
onMounted(loadFindings)
</script>

<template>
  <main class="app-shell">
    <header class="topbar"><div class="brand"><span>B</span><b>LogicCheck</b></div><div class="document-title"><i></i> {{ filename.replace('.pdf','') }} <small>v3</small></div><div class="top-actions"><input ref="fileInput" type="file" accept="application/pdf" hidden @change="upload"><button @click="copyShare">공유</button><button @click="fileInput.click()">PDF 교체</button><button class="dark" @click="modal='complete'">검토 완료</button><em>김</em></div></header>
    <div class="toolbar"><button aria-label="축소" @click="zoom=Math.max(55,zoom-10)">−</button><button class="zoom" @click="zoom=82">{{ zoom }}%</button><button aria-label="확대" @click="zoom=Math.min(130,zoom+10)">＋</button><span></span><button @click="goPage(page-1)">‹</button><input class="page-input" :value="page" @change="goPage($event.target.value)"><b>/ 21</b><button @click="goPage(page+1)">›</button><label><input v-model="showEvidence" type="checkbox"> 원문 근거 표시</label><button @click="modal='versions'">버전 비교</button><button @click="exportCsv">결과 내보내기</button><div><strong>{{ loading ? `분석 중 ${progress}%` : '분석 완료' }}</strong><b>{{ findings.length }}개 항목</b></div></div>
    <section class="workspace">
      <nav class="rail"><b>B</b><button :class="{on:railView==='document'}" title="문서 검토" @click="railView='document'">▤</button><button :class="{on:railView==='saved'}" title="확인한 오류" @click="railView='saved';filter='confirmed'">☆</button><button title="오류 관계" @click="modal='impact'">⌁</button><button title="검토 요약" @click="modal='complete'">□</button><button class="bottom" title="도움말" @click="modal='help'">?</button></nav>
      <aside class="outline"><header>문서 목차 <button @click="focusSearch">⌕</button></header><div class="score"><span>검토 진행률</span><strong>{{ reviewed }} / {{ findings.length }}</strong><progress :value="reviewed" :max="findings.length"></progress></div><ol><li v-for="(section,index) in sections" :key="section.title" :class="{active:pageTitle===section.title}" @click="goPage(section.page)"><span>0{{ index+1 }}</span>{{ section.title }}<em v-if="findings.some(f=>f.evidence.some(e=>e.page>=section.page && e.page<(sections[index+1]?.page||22)))">{{ findings.filter(f=>f.evidence.some(e=>e.page>=section.page && e.page<(sections[index+1]?.page||22))).length }}</em></li></ol></aside>
      <article class="document"><div class="paper" :style="{transform:`scale(${zoom/100})`,transformOrigin:'top center',marginBottom:`-${(100-zoom)*5}px`}"><div class="page-number">{{ page }}</div><small>중소기업 기술개발 지원사업</small><h1>AI 기반 무인매장<br>안내 로봇 사업계획서</h1><div class="meta"><span>주관기관</span><b>써니팩토리</b><span>개발기간</span><b>2026.09 — 2028.08</b></div><hr><h2>{{ sections.find(s=>s.title===pageTitle)?.title }}</h2><p>매장 내 고객의 위치와 행동을 실시간으로 분석하고 개인화된 상품 안내를 제공하는 자율주행 로봇 시스템을 개발한다.</p><div class="pipeline"><span>카메라 입력<small>30 FPS 영상</small></span>→<span class="flagged">비전 AI 모델<small>모델 3종 순차 처리</small></span>→<span>행동 분석<small>실시간 추론</small></span>→<span>로봇 안내<small>0.5초 이내</small></span></div><h3>핵심 목표 및 산출 근거</h3><table><thead><tr><th>평가 항목</th><th>현재 수준</th><th>최종 목표</th><th>평가 방법</th></tr></thead><tbody><tr><td>객체 인식 정확도</td><td>82%</td><td><mark>95%</mark></td><td><mark class="danger">평가 방법 미기재</mark></td></tr><tr><td>평균 응답 시간</td><td>2.1초</td><td><mark>0.5초 이내</mark></td><td>자체 시험</td></tr><tr><td>2027년 예상 매출</td><td>—</td><td><mark :class="{danger:[11,16].includes(page)}">{{ page===11?'18억 원':'24억 원' }}</mark></td><td>산출 근거 확인</td></tr></tbody></table><div v-if="showEvidence" class="evidence-card"><b>{{ typeLabel[active.type] }}</b><span>{{ active.evidence.find(e=>e.page===page)?.text || active.evidence[0].text }}</span></div></div></article>
      <aside class="review"><div class="tabs"><button :class="{active:rightTab==='results'}" @click="rightTab='results'">검토 결과 <span>{{ findings.length }}</span></button><button :class="{active:rightTab==='chat'}" @click="rightTab='chat'">AI 질문</button></div><template v-if="rightTab==='results'"><div class="summary"><button @click="filter='all'"><b>{{ findings.filter(f=>f.severity==='high').length }}</b><small>중대 오류</small></button><button @click="filter='unreviewed'"><b>{{ findings.filter(f=>f.status==='unreviewed').length }}</b><small>미확인</small></button><button @click="filter='processed'"><b>{{ reviewed }}</b><small>처리 완료</small></button></div><div class="filter"><input ref="searchInput" v-model="search" class="search-input" placeholder="오류 검색"><select v-model="filter"><option value="all">전체</option><option value="unreviewed">미확인</option><option value="processed">처리 완료</option><option value="confirmed">확인</option><option value="dismissed">기각</option><option value="pending">보류</option><option value="revision_requested">수정 요청</option></select></div><div v-if="!visibleFindings.length" class="empty">조건에 맞는 오류가 없습니다.</div><div class="finding-list"><button v-for="finding in visibleFindings" :key="finding.id" :class="{active:active.id===finding.id}" @click="selectFinding(finding)"><span :class="finding.severity">{{ typeLabel[finding.type] }}</span><b>{{ finding.title }}</b><small>p. {{ finding.evidence.map(e=>e.page).join(' · p. ') }} · {{ statusLabel[finding.status] }}</small></button></div><section class="detail"><div class="confidence">신뢰도 {{ Math.round(active.confidence*100) }}%</div><h3>{{ active.title }}</h3><p>{{ active.description }}</p><b>비교한 원문</b><button v-for="evidence in active.evidence" :key="`${active.id}-${evidence.page}`" class="reference" @click="selectFinding(active,evidence)">↗ p. {{ evidence.page }} · {{ evidence.text }}</button><div class="actions"><button @click="decide('dismissed')">오류 아님</button><button @click="decide('pending')">보류</button><button class="accept" @click="decide('confirmed')">오류 확인</button></div><button class="revision" @click="decide('revision_requested')">작성자에게 수정 요청</button><details><summary>검토 이력 {{ history.length }}</summary><p v-if="!history.length">아직 기록이 없습니다.</p><p v-for="item in history" :key="item.createdAt">{{ item.actor }} · {{ statusLabel[item.after] || item.action }} · {{ new Date(item.createdAt).toLocaleString('ko-KR') }}</p></details></section></template><section v-else class="chat"><div class="chat-log"><div v-if="!chat.length" class="chat-intro"><b>문서에 질문하세요</b><p>“매출 수치가 맞아?” 또는 “기술과 KPI가 적합해?”처럼 질문할 수 있습니다.</p><button @click="question='기술과 KPI가 적합한가요?';ask()">기술과 KPI가 적합한가요?</button></div><p v-for="(message,index) in chat" :key="index" :class="message.role">{{ message.text }}</p></div><form @submit.prevent="ask"><input v-model="question" placeholder="사업계획서에 질문"><button>↑</button></form></section></aside>
    </section>
    <div v-if="loading" class="analysis-bar"><span :style="{width:`${progress}%`}"></span><b>문서를 분석하고 있습니다 · {{ progress }}%</b></div><div v-if="toast" class="toast">{{ toast }}</div>
    <div v-if="modal" class="modal-backdrop" @click.self="modal=''" role="presentation"><section class="modal"><button class="close" @click="modal=''">×</button><template v-if="modal==='complete'"><h2>검토 요약</h2><div class="report-grid"><b>{{ findings.length }}<small>전체 오류</small></b><b>{{ findings.filter(f=>f.severity==='high').length }}<small>중대 오류</small></b><b>{{ reviewed }}<small>처리 완료</small></b></div><p>미확인 오류 {{ findings.length-reviewed }}건이 남아 있습니다.</p><button class="primary" @click="exportCsv();modal=''">결과 CSV 내려받기</button></template><template v-else-if="modal==='versions'"><h2>버전 비교</h2><div class="version-row"><b>v2</b><span>이전 제출본</span><em>오류 7건</em></div><div class="version-row current"><b>v3</b><span>현재 검토본</span><em>해결 2 · 신규 1 · 유지 4</em></div><button class="primary" @click="notice('변경된 3개 항목을 표시했습니다.');modal=''">변경 결과 보기</button></template><template v-else-if="modal==='impact'"><h2>오류 영향 관계</h2><div class="impact-flow"><b>판매단가</b><i>→</i><b>예상 매출</b><i>→</i><b>손익분기점</b><i>→</i><b>투자 필요액</b></div><p>선택한 수치 오류가 3개의 후속 계획에 영향을 줍니다.</p></template><template v-else><h2>빠른 도움말</h2><ul><li>오류를 누르면 해당 원문 페이지로 이동합니다.</li><li>비교 원문을 누르면 페이지 사이를 오갈 수 있습니다.</li><li>판정 결과와 담당자는 검토 이력에 기록됩니다.</li><li>상단에서 확대·페이지 이동·내보내기가 가능합니다.</li></ul></template></section></div>
  </main>
</template>
