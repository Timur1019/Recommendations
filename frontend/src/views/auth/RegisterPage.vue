<script setup>
import { computed, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import BaseInput from '../../components/common/BaseInput.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { authApi } from '../../services/api/authApi'

const router = useRouter()
const { t } = useI18n()

const form = reactive({
  role: 'ATHLETE',
  email: '',
  password: '',
  fullName: '',
  phone: '',
  region: 'TASHKENT',
  weightCategory: '73',
  dateOfBirth: '',
  rank: '',
  experienceYears: '',
  loading: false,
  error: '',
})

const isAthlete = computed(() => form.role === 'ATHLETE')

function formatRegisterError(e) {
  const d = e?.response?.data
  if (!d) return null
  const parts = [d.message].filter(Boolean)
  if (Array.isArray(d.details) && d.details.length) {
    parts.push(...d.details.filter(Boolean))
  }
  return parts.length ? parts.join(' ') : null
}

async function submit() {
  form.error = ''
  form.loading = true
  try {
    const email = form.email.trim()
    const fullName = form.fullName.trim()
    const password = form.password
    const phone = form.phone.trim()

    if (!email || !fullName || !password) {
      form.error = t('auth.errors.fillRequired')
      return
    }
    if (password.length < 6) {
      form.error = t('auth.errors.passwordMin6')
      return
    }

    if (isAthlete.value) {
      await authApi.registerAthlete({
        email,
        password,
        fullName,
        phone: phone || null,
        region: form.region,
        weightCategory: form.weightCategory,
        dateOfBirth: form.dateOfBirth,
        rank: (form.rank || '').trim() || null,
        coachId: null,
      })
    } else {
      const expRaw = form.experienceYears.trim()
      const expNum = expRaw === '' ? null : Number(expRaw)
      const experienceYears = expRaw !== '' && Number.isFinite(expNum) ? expNum : null
      await authApi.registerCoach({
        email,
        password,
        fullName,
        phone: phone || null,
        region: form.region,
        federationMember: false,
        experienceYears,
      })
    }
    router.push('/login')
  } catch (e) {
    form.error = formatRegisterError(e) || t('auth.errors.registerFailed')
  } finally {
    form.loading = false
  }
}
</script>

<template>
  <div class="kn-auth-page">
    <div class="kn-auth-container">
      <div class="kn-auth-panel p-4 p-md-5">
      <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-4 pb-3 border-bottom border-light">
        <div>
          <div class="font-headline fw-bold fs-3 text-dark">{{ t('auth.registerTitle') }}</div>
          <div class="app-muted mt-1">{{ t('auth.registerSubtitle') }}</div>
        </div>
        <div class="btn-group kn-role-toggle shadow-sm" role="group" aria-label="Role select">
          <input id="r-ath" v-model="form.role" type="radio" class="btn-check" value="ATHLETE" />
          <label class="btn btn-outline-primary" for="r-ath">{{ t('auth.roleAthlete') }}</label>
          <input id="r-coach" v-model="form.role" type="radio" class="btn-check" value="COACH" />
          <label class="btn btn-outline-primary" for="r-coach">{{ t('auth.roleCoach') }}</label>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-12 col-md-6">
          <BaseInput
            v-model="form.fullName"
            :label="t('auth.fullName')"
            placeholder="Бахтиёр Алиев"
            autocomplete="name"
          />
        </div>
        <div class="col-12 col-md-6">
          <BaseInput v-model="form.phone" :label="t('auth.phone')" placeholder="+998 90 000 00 00" autocomplete="tel" />
        </div>
        <div class="col-12 col-md-6">
          <BaseInput v-model="form.email" :label="t('auth.email')" placeholder="bakhtiyar@kurash.uz" autocomplete="email" />
        </div>
        <div class="col-12 col-md-6">
          <BaseInput v-model="form.password" :label="t('auth.password')" type="password" placeholder="••••••••" />
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">{{ t('auth.region') }}</label>
          <select v-model="form.region" class="form-select">
            <option value="TASHKENT">Tashkent</option>
            <option value="SAMARKAND">Samarkand</option>
            <option value="BUKHARA">Bukhara</option>
            <option value="ANDIJAN">Andijan</option>
            <option value="NAMANGAN">Namangan</option>
            <option value="FERGANA">Fergana</option>
          </select>
        </div>

        <template v-if="isAthlete">
          <div class="col-12 col-md-6">
            <label class="form-label fw-semibold">{{ t('auth.weightCategory') }}</label>
            <select v-model="form.weightCategory" class="form-select">
              <option value="60">60</option>
              <option value="66">66</option>
              <option value="73">73</option>
              <option value="81">81</option>
              <option value="90">90</option>
              <option value="100">100</option>
              <option value="100+">100+</option>
            </select>
          </div>
          <div class="col-12 col-md-6">
            <BaseInput v-model="form.dateOfBirth" :label="t('auth.dateOfBirth')" type="date" />
          </div>
          <div class="col-12 col-md-6">
            <BaseInput v-model="form.rank" :label="t('auth.rank')" placeholder="MASTER_OF_SPORT" />
          </div>
        </template>

        <template v-else>
          <div class="col-12 col-md-6">
            <BaseInput v-model="form.experienceYears" :label="t('auth.experienceYears')" placeholder="5" />
          </div>
        </template>
      </div>

      <div v-if="form.error" class="alert alert-danger mt-3 py-2">{{ form.error }}</div>

      <div class="d-flex gap-2 mt-4 flex-wrap align-items-stretch">
        <BaseButton pill :loading="form.loading" @click="submit">{{ t('common.create') }}</BaseButton>
        <RouterLink class="btn btn-outline-secondary rounded-pill px-4" to="/login">{{ t('common.backToLogin') }}</RouterLink>
      </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kn-role-toggle {
  border-radius: var(--kn-radius-btn);
  overflow: hidden;
}

.kn-role-toggle .btn {
  font-weight: 700;
}
</style>
