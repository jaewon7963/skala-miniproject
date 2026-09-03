/** 인증 화면 검증 규칙 (AUTH-02 / F-06) */

export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateEmail(value) {
  if (!value) return '이메일을 입력해주세요'
  if (!EMAIL_RE.test(value)) return '올바른 이메일 형식이 아닙니다'
  return ''
}

/** 8자 이상 · 영문과 숫자 각 1자 이상 */
export function validatePassword(value) {
  if (!value) return '비밀번호를 입력해주세요'
  if (value.length < 8) return '8자 이상 입력해주세요'
  if (!/[A-Za-z]/.test(value) || !/[0-9]/.test(value)) return '영문과 숫자를 각 1자 이상 포함해주세요'
  return ''
}
