/**
 * LLM 호출 클라이언트
 *
 * 화면은 이 파일을 직접 쓰지 않고 api/modules/chat.js 를 통해 호출합니다.
 * 공급자 규격 차이는 아래 adapters 안에만 존재합니다. 새 규격이 필요하면
 * adapter 하나만 추가하면 되고, 화면 코드는 그대로입니다.
 */
import { llmConfig, llmReadiness } from '@/api/llm/config'
import { ApiError } from '@/api/http'

/* ─── 공급자별 규격 차이 ─────────────────────────────────────── */

const adapters = {
  /** OpenAI 호환 — 상용 API 대부분 · vLLM · Ollama · LM Studio · Groq 등 */
  openai: {
    path: '/chat/completions',
    headers: (key) => (key ? { Authorization: `Bearer ${key}` } : {}),
    body: (system, messages, stream) => ({
      model: llmConfig.model,
      stream,
      temperature: llmConfig.temperature,
      max_tokens: llmConfig.maxTokens,
      messages: [{ role: 'system', content: system }, ...messages],
    }),
    // 스트리밍 조각에서 새로 늘어난 텍스트를 꺼냅니다.
    delta: (chunk) => chunk?.choices?.[0]?.delta?.content ?? '',
    // 스트리밍을 쓰지 않을 때의 전체 응답
    full: (data) => data?.choices?.[0]?.message?.content ?? '',
  },

  /** Anthropic Messages API — system 이 별도 필드이고 헤더 규격이 다릅니다. */
  anthropic: {
    path: '/messages',
    headers: (key) => ({
      'x-api-key': key,
      'anthropic-version': '2023-06-01',
      // 브라우저에서 직접 호출할 때 필요합니다. BE 프록시로 옮기면 지워도 됩니다.
      'anthropic-dangerous-direct-browser-access': 'true',
    }),
    body: (system, messages, stream) => ({
      model: llmConfig.model,
      stream,
      system,
      temperature: llmConfig.temperature,
      max_tokens: llmConfig.maxTokens,
      messages,
    }),
    delta: (chunk) => (chunk?.type === 'content_block_delta' ? (chunk.delta?.text ?? '') : ''),
    full: (data) => data?.content?.map((c) => c.text ?? '').join('') ?? '',
  },
}

function getAdapter() {
  const adapter = adapters[llmConfig.dialect]
  if (!adapter) {
    throw new ApiError(0, `지원하지 않는 VITE_LLM_DIALECT 입니다: ${llmConfig.dialect}`)
  }
  return adapter
}

/* ─── 에러 정규화 ────────────────────────────────────────────── */

function toApiError(status, payload) {
  const raw = payload?.error?.message || payload?.message || ''
  const message =
    status === 401 || status === 403
      ? 'API 키가 유효하지 않습니다. .env.local 의 VITE_LLM_API_KEY 를 확인해주세요.'
      : status === 404
        ? `모델 또는 주소를 찾을 수 없습니다. VITE_LLM_MODEL · VITE_LLM_BASE_URL 을 확인해주세요.${raw ? ` (${raw})` : ''}`
        : status === 429
          ? '요청이 한도를 초과했습니다. 잠시 후 다시 시도해주세요.'
          : raw || '답변을 생성하지 못했습니다.'
  return new ApiError(status, message, payload?.error?.type ?? null, payload ?? null)
}

/* ─── SSE 파싱 ───────────────────────────────────────────────── */

/** 스트리밍 응답을 줄 단위로 읽어 JSON 조각을 순서대로 내보냅니다. */
async function* readSse(response) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    // 이벤트 경계는 빈 줄입니다. 마지막 조각은 다음 청크와 이어붙입니다.
    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''

    for (const event of events) {
      for (const line of event.split('\n')) {
        if (!line.startsWith('data:')) continue
        const payload = line.slice(5).trim()
        if (!payload || payload === '[DONE]') continue
        try {
          yield JSON.parse(payload)
        } catch {
          /* 조각난 JSON 은 건너뜁니다 */
        }
      }
    }
  }
}

/* ─── 호출 ───────────────────────────────────────────────────── */

/**
 * 대화를 요청하고 답변을 조각 단위로 전달합니다.
 *
 * @param {object}   params
 * @param {string}   params.system    시스템 프롬프트
 * @param {Array<{role:'user'|'assistant', content:string}>} params.messages
 * @param {(text:string)=>void} [params.onDelta]  새로 도착한 텍스트 조각
 * @param {AbortSignal} [params.signal]
 * @returns {Promise<string>} 완성된 답변 전체
 */
export async function chat({ system, messages, onDelta, signal }) {
  const ready = llmReadiness()
  if (!ready.ok) throw new ApiError(0, `LLM 설정이 완료되지 않았습니다 — ${ready.reason}`)

  const adapter = getAdapter()
  const stream = typeof onDelta === 'function'

  let response
  try {
    response = await fetch(`${llmConfig.baseUrl}${adapter.path}`, {
      method: 'POST',
      signal,
      headers: {
        'Content-Type': 'application/json',
        ...adapter.headers(llmConfig.apiKey),
      },
      body: JSON.stringify(adapter.body(system, messages, stream)),
    })
  } catch (e) {
    if (e.name === 'AbortError') throw e
    throw new ApiError(0, 'LLM 서버에 연결하지 못했습니다. 주소와 네트워크를 확인해주세요.')
  }

  if (!response.ok) {
    let payload = null
    try {
      payload = await response.json()
    } catch {
      /* 본문이 JSON 이 아닐 수 있습니다 */
    }
    throw toApiError(response.status, payload)
  }

  // 스트리밍을 요청하지 않았거나 본문을 읽을 수 없는 환경
  if (!stream || !response.body) {
    const data = await response.json()
    return adapter.full(data)
  }

  let answer = ''
  for await (const chunk of readSse(response)) {
    const piece = adapter.delta(chunk)
    if (!piece) continue
    answer += piece
    onDelta(piece)
  }
  return answer
}
