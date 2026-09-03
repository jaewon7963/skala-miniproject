<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { validateName, validatePassword } from '@/utils/validators'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppModal from '@/components/common/AppModal.vue'

/** AUTH-05 프로필 · AUTH-06 로그아웃 / 탈퇴 (F-08) */
const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()

const profile = ref({ name: auth.user?.name ?? '' })
const profileError = ref('')
const savingProfile = ref(false)

const password = ref({ currentPassword: '', newPassword: '' })
const passwordErrors = ref({ currentPassword: '', newPassword: '' })
const savingPassword = ref(false)

const withdrawOpen = ref(false)
const withdrawPassword = ref('')

async function saveProfile() {
  profileError.value = validateName(profile.value.name)
  if (profileError.value) return
  savingProfile.value = true
  try {
    await auth.updateProfile({ name: profile.value.name.trim() })
    ui.success('변경 사항을 저장했습니다')
  } catch (e) {
    ui.error(e.message)
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  passwordErrors.value.currentPassword = password.value.currentPassword
    ? ''
    : '현재 비밀번호를 입력해주세요'
  passwordErrors.value.newPassword = validatePassword(password.value.newPassword)
  if (Object.values(passwordErrors.value).some(Boolean)) return

  savingPassword.value = true
  try {
    await auth.changePassword({ ...password.value })
    password.value = { currentPassword: '', newPassword: '' }
    ui.success('비밀번호를 변경했습니다')
  } catch (e) {
    ui.error(e.message)
  } finally {
    savingPassword.value = false
  }
}

async function logout() {
  await auth.logout()
  ui.info('로그아웃되었습니다')
  router.push({ name: 'login' })
}

async function withdraw() {
  try {
    await auth.withdraw({ password: withdrawPassword.value })
    withdrawOpen.value = false
    ui.info('탈퇴 요청을 접수했습니다')
    router.push({ name: 'login' })
  } catch (e) {
    ui.error(e.message)
  }
}
</script>

<template>
  <div class="settings">
    <h1 class="settings__title">설정</h1>

    <section class="card u-card">
      <h2 class="card__title">프로필</h2>
      <div class="card__body">
        <AppInput v-model="profile.name" label="이름" :error="profileError" />
        <AppInput
          :model-value="auth.user?.email"
          label="이메일"
          disabled
          hint="이메일은 변경할 수 없습니다 (조직 식별 기준)"
        />
      </div>
      <footer class="card__foot">
        <AppButton :loading="savingProfile" @click="saveProfile">변경 사항 저장</AppButton>
      </footer>
    </section>

    <section class="card u-card">
      <h2 class="card__title">보안</h2>
      <div class="card__body">
        <AppInput
          v-model="password.currentPassword"
          label="현재 비밀번호"
          type="password"
          :error="passwordErrors.currentPassword"
        />
        <AppInput
          v-model="password.newPassword"
          label="새 비밀번호"
          type="password"
          :error="passwordErrors.newPassword"
          hint="8자 이상 · 영문과 숫자 각 1자 이상"
        />
      </div>
      <footer class="card__foot">
        <AppButton variant="secondary" :loading="savingPassword" @click="changePassword">
          비밀번호 변경
        </AppButton>
      </footer>
    </section>

    <section class="card u-card">
      <h2 class="card__title">계정</h2>
      <div class="row">
        <div>
          <p class="row__title">로그아웃</p>
          <p class="row__desc">현재 세션을 종료합니다</p>
        </div>
        <AppButton variant="secondary" size="sm" @click="logout">로그아웃</AppButton>
      </div>
      <div class="row">
        <div>
          <p class="row__title">회원 탈퇴</p>
          <p class="row__desc">문서와 검토 기록이 30일 후 영구 삭제 · 비식별화됩니다</p>
        </div>
        <AppButton variant="danger" size="sm" @click="withdrawOpen = true">탈퇴 요청</AppButton>
      </div>
    </section>

    <!-- 탈퇴 : 비밀번호 재확인 -->
    <AppModal :open="withdrawOpen" title="정말 탈퇴하시겠어요?" @close="withdrawOpen = false">
      <p class="confirm">
        탈퇴하면 문서 · 분석 작업 · 검토 항목이 함께 삭제됩니다. 계속하려면 비밀번호를 다시
        입력해주세요.
      </p>
      <AppInput v-model="withdrawPassword" type="password" placeholder="비밀번호" />
      <template #footer>
        <AppButton variant="secondary" @click="withdrawOpen = false">취소</AppButton>
        <AppButton variant="danger" @click="withdraw">탈퇴하기</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.settings {
  max-width: 640px;
  margin: 0 auto;
}
.settings__title {
  font-size: var(--fs-xl);
  margin-bottom: 18px;
}
.card {
  padding: 18px;
  margin-bottom: 14px;
}
.card__title {
  font-size: var(--fs-base);
  margin-bottom: 12px;
}
.card__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid var(--c-border);
}
.row:first-of-type {
  border-top: none;
}
.row__title {
  font-weight: 600;
  font-size: var(--fs-md);
}
.row__desc {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.confirm {
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.6;
  margin-bottom: 12px;
}
</style>
