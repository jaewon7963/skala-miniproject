import http from '@/api/http'
import EP from '@/api/endpoints'
import { USE_MOCK } from '@/api/config'
import mock from '@/api/mock/handlers'

/**
 * 인증 API (AUTH-01 ~ AUTH-06)
 * 응답 계약
 *  login/signup -> { token, user: { id, name, email, organization } }
 */
export const authApi = {
  login: (payload) => (USE_MOCK ? mock.auth.login(payload) : http.post(EP.auth.login, payload)),
  signup: (payload) => (USE_MOCK ? mock.auth.signup(payload) : http.post(EP.auth.signup, payload)),
  me: () => (USE_MOCK ? mock.auth.me() : http.get(EP.auth.me)),
  logout: () => (USE_MOCK ? mock.auth.logout() : http.post(EP.auth.logout)),
  updateProfile: (payload) =>
    USE_MOCK ? mock.auth.updateProfile(payload) : http.patch(EP.auth.profile, payload),
  changePassword: (payload) =>
    USE_MOCK ? mock.auth.changePassword(payload) : http.patch(EP.auth.password, payload),
  withdraw: (payload) =>
    USE_MOCK ? mock.auth.withdraw(payload) : http.delete(EP.auth.withdraw, { body: payload }),
}

export default authApi
