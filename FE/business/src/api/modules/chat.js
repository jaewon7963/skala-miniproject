/**
 * 검토 화면 챗봇 — Mock ↔ 실제 LLM 전환 지점
 *
 * 다른 모듈(auth · documents · reviews)이 USE_MOCK 으로 갈리듯,
 * 챗봇은 LLM 설정이 갖춰졌는지(LLM_READY)로 갈립니다.
 * 키가 없거나 VITE_LLM_ENABLED=false 면 기존 목업 응답이 그대로 동작하므로,
 * 키 없이도 화면 개발과 시연이 막히지 않습니다.
 */
import mock from '@/api/mock/handlers'
import { chat } from '@/api/llm/client'
import { llmConfig, LLM_READY, llmReadiness } from '@/api/llm/config'
import { buildChatSystemPrompt } from '@/api/llm/prompts'

/** 최근 N턴만 보냅니다. 대화가 길어져도 토큰이 선형으로 늘지 않습니다. */
function trimHistory(messages) {
  return messages
    .filter((m) => m.role === 'user' || m.role === 'assistant')
    .slice(-llmConfig.historyTurns)
    .map((m) => ({ role: m.role, content: m.text ?? '' }))
    .filter((m) => m.content.trim())
}

export const chatApi = {
  /** 지금 어떤 모델로 답하는지 (화면 표시용) */
  status: () => ({
    ready: LLM_READY,
    reason: llmReadiness().reason,
    model: LLM_READY ? llmConfig.model : 'mock',
  }),

  /**
   * @param {object} params
   * @param {object|null} params.document  현재 문서 (제목 · 쪽수)
   * @param {Array} params.findings        검출된 검토 항목
   * @param {Array} params.history         이전 대화 [{role, text}]
   * @param {string} params.question       이번 질문
   * @param {(text:string)=>void} [params.onDelta]
   * @param {AbortSignal} [params.signal]
   * @returns {Promise<{answer:string, evidences:Array, promotable:boolean}>}
   */
  async send({ document, findings = [], history = [], question, onDelta, signal }) {
    if (!LLM_READY) {
      const result = await mock.reviews.ask(document?.jobId ?? null, { question })
      onDelta?.(result.answer)
      return result
    }

    const answer = await chat({
      system: buildChatSystemPrompt(document, findings),
      messages: [...trimHistory(history), { role: 'user', content: question }],
      onDelta,
      signal,
    })

    // 근거 연결(evidences)과 검토 항목 승격(promotable)은 RAG 도입 후 채웁니다.
    // 지금은 대화만 담당하므로 화면 계약만 맞춰 빈 값을 돌려줍니다.
    return { answer, evidences: [], promotable: false }
  },
}
