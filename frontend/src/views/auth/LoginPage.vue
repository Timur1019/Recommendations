<script setup>
import { reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import BaseInput from '../../components/common/BaseInput.vue'
import BaseButton from '../../components/common/BaseButton.vue'

const auth = useAuthStore()
const router = useRouter()
const { t } = useI18n()

const form = reactive({
  email: '',
  password: '',
  errors: {},
})

async function submit() {
  form.errors = {}
  if (!form.email) form.errors.email = t('auth.errors.emailRequired')
  if (!form.password) form.errors.password = t('auth.errors.passwordRequired')
  if (Object.keys(form.errors).length) return

  const ok = await auth.login(form.email, form.password)
  if (ok) router.push(auth.homeRoute)
}
</script>

<template>
  <div class="kn-auth-page">
    <div class="kn-auth-container">
      <div class="row g-4 align-items-stretch">
        <div class="col-12 col-lg-6">
          <div class="kn-auth-panel p-4 p-md-5 h-100 d-flex flex-column">
            <div class="mb-4 pb-2 border-bottom border-light">
              <div class="font-headline fw-bold fs-3 text-dark">{{ t('app.name') }}</div>
              <div class="app-muted mt-1">{{ t('auth.loginSubtitle') }}</div>
            </div>

            <BaseInput
              v-model="form.email"
              :label="t('auth.email')"
              placeholder="bakhtiyar@example.com"
              autocomplete="email"
              :error="form.errors.email"
            />
            <BaseInput
              v-model="form.password"
              :label="t('auth.password')"
              type="password"
              placeholder="••••••••"
              autocomplete="current-password"
              :error="form.errors.password"
            />

            <div v-if="auth.error" class="alert alert-danger py-2 mb-3">{{ auth.error }}</div>

            <BaseButton :loading="auth.loading" block pill class="mt-1" @click="submit">{{ t('auth.login') }}</BaseButton>

            <div class="mt-4 small app-muted">
              {{ t('auth.noAccount') }}
              <RouterLink to="/register" class="fw-semibold text-decoration-none kn-auth-link">{{ t('auth.register') }}</RouterLink>
            </div>
          </div>
        </div>

        <div class="col-12 col-lg-6">
          <div class="kn-auth-showcase text-white p-4 p-md-5 h-100 d-flex flex-column justify-content-center">
            <div class="kn-auth-showcase__inner">
              <p class="kn-auth-kicker text-white mb-0">{{ t('common.aiPlan') }}</p>
              <h1 class="kn-auth-title text-white mt-3 mb-3">{{ t('app.tagline') }}</h1>
              <p class="kn-auth-lead text-white mb-0">
                {{ t('recommendations.comparisonTitle') }} · {{ t('recommendations.weekPlanTitle') }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kn-auth-link {
  color: var(--kn-primary);
}
.kn-auth-link:hover {
  color: var(--kn-primary-container);
}
</style>
