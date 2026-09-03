import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi, tokenStore } from '@/api'

const USER_KEY = 'logicheck.user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))
  const token = ref(tokenStore.get())
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))

  function persist(nextToken, nextUser) {
    token.value = nextToken
    user.value = nextUser
    tokenStore.set(nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  function clear() {
    token.value = null
    user.value = null
    tokenStore.clear()
    localStorage.removeItem(USER_KEY)
  }

  async function login(payload) {
    loading.value = true
    try {
      const { token: nextToken, user: nextUser } = await authApi.login(payload)
      persist(nextToken, nextUser)
      return nextUser
    } finally {
      loading.value = false
    }
  }

  async function signup(payload) {
    loading.value = true
    try {
      const { token: nextToken, user: nextUser } = await authApi.signup(payload)
      persist(nextToken, nextUser)
      return nextUser
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clear()
    }
  }

  async function updateProfile(payload) {
    const next = await authApi.updateProfile(payload)
    user.value = next
    localStorage.setItem(USER_KEY, JSON.stringify(next))
    return next
  }

  const changePassword = (payload) => authApi.changePassword(payload)

  async function withdraw(payload) {
    const result = await authApi.withdraw(payload)
    clear()
    return result
  }

  return {
    user,
    token,
    loading,
    isAuthenticated,
    login,
    signup,
    logout,
    updateProfile,
    changePassword,
    withdraw,
    clear,
  }
})
