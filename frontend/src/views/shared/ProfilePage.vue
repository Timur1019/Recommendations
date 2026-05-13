<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AppShell from '../../layouts/AppShell.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import BaseInput from '../../components/common/BaseInput.vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../../stores/auth'
import { useAthleteStore } from '../../stores/athlete'
import { authApi } from '../../services/api/authApi'
import { athleteApi } from '../../services/api/athleteApi'
import { coachApi } from '../../services/api/coachApi'
import { achievementApi } from '../../services/api/achievementApi'
import './profile.css'

const { t } = useI18n()
const auth = useAuthStore()
const athleteStore = useAthleteStore()

const loading = ref(true)
const savingUser = ref(false)
const savingAthlete = ref(false)
const message = ref('')
const error = ref('')
const coachProfile = ref(null)
const achievements = ref([])
const avatarBroken = ref(false)

const userForm = reactive({
  avatarUrl: '',
  phone: '',
})

const athleteForm = reactive({
  region: 'TASHKENT',
  weightCategory: '73',
  dateOfBirth: '',
  rank: '',
  sportType: 'KURASH',
  heightCm: '',
  bodyWeightKg: '',
  goalText: '',
  currentMedalCountGold: 0,
  currentMedalCountSilver: 0,
  currentMedalCountBronze: 0,
})

const isAthlete = computed(() => auth.user?.role === 'ATHLETE')
const isCoach = computed(() => auth.user?.role === 'COACH')

const displayAvatarUrl = computed(() => (userForm.avatarUrl || '').trim())

const initials = computed(() => {
  const name = auth.user?.fullName || ''
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  if (parts.length === 1 && parts[0].length >= 2) return parts[0].slice(0, 2).toUpperCase()
  if (parts.length === 1) return parts[0][0].toUpperCase()
  return '?'
})

function syncUserForm() {
  userForm.avatarUrl = auth.user?.avatarUrl || ''
  userForm.phone = auth.user?.phone || ''
  avatarBroken.value = false
}

function fillAthleteForm(p) {
  if (!p) return
  athleteForm.region = p.region || 'TASHKENT'
  athleteForm.weightCategory = p.weightCategory || '73'
  athleteForm.dateOfBirth = p.dateOfBirth || ''
  athleteForm.rank = p.rank || ''
  athleteForm.sportType = p.sportType || 'KURASH'
  athleteForm.heightCm = p.heightCm != null ? String(p.heightCm) : ''
  athleteForm.bodyWeightKg = p.bodyWeightKg != null ? String(p.bodyWeightKg) : ''
  athleteForm.goalText = p.goalText || ''
  athleteForm.currentMedalCountGold = p.currentMedalCountGold ?? 0
  athleteForm.currentMedalCountSilver = p.currentMedalCountSilver ?? 0
  athleteForm.currentMedalCountBronze = p.currentMedalCountBronze ?? 0
}

async function load() {
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    await auth.fetchMe()
    syncUserForm()
    if (isAthlete.value) {
      await athleteStore.fetchMyProfile()
      fillAthleteForm(athleteStore.profile)
      try {
        achievements.value = await achievementApi.myAchievements()
      } catch {
        achievements.value = []
      }
    } else {
      achievements.value = []
    }
    if (isCoach.value) {
      try {
        coachProfile.value = await coachApi.me()
      } catch {
        coachProfile.value = null
      }
    } else {
      coachProfile.value = null
    }
  } catch (e) {
    error.value = e?.response?.data?.message || t('profile.loadFailed')
  } finally {
    loading.value = false
  }
}

async function saveUser() {
  savingUser.value = true
  message.value = ''
  error.value = ''
  try {
    const updated = await authApi.patchMyProfile({
      avatarUrl: userForm.avatarUrl?.trim() || '',
      phone: userForm.phone?.trim() || '',
    })
    auth.user = updated
    syncUserForm()
    message.value = t('profile.savedAccount')
  } catch (e) {
    error.value = e?.response?.data?.message || t('profile.saveFailed')
  } finally {
    savingUser.value = false
  }
}

async function saveAthlete() {
  if (!isAthlete.value || !athleteStore.profile) return
  savingAthlete.value = true
  message.value = ''
  error.value = ''
  try {
    const p = athleteStore.profile
    const payload = {
      fullName: auth.user?.fullName || p.user?.fullName,
      phone: userForm.phone?.trim() || null,
      region: athleteForm.region,
      weightCategory: athleteForm.weightCategory,
      dateOfBirth: athleteForm.dateOfBirth,
      rank: athleteForm.rank?.trim() || null,
      currentMedalCountGold: Number(athleteForm.currentMedalCountGold) || 0,
      currentMedalCountSilver: Number(athleteForm.currentMedalCountSilver) || 0,
      currentMedalCountBronze: Number(athleteForm.currentMedalCountBronze) || 0,
      sportType: athleteForm.sportType?.trim() || null,
      heightCm:
        athleteForm.heightCm === '' || !Number.isFinite(Number(athleteForm.heightCm))
          ? null
          : Number(athleteForm.heightCm),
      bodyWeightKg:
        athleteForm.bodyWeightKg === '' || !Number.isFinite(Number(athleteForm.bodyWeightKg))
          ? null
          : Number(athleteForm.bodyWeightKg),
      goalText: athleteForm.goalText?.trim() || null,
    }
    athleteStore.profile = await athleteApi.updateMe(payload)
    await auth.fetchMe()
    syncUserForm()
    message.value = t('profile.savedSports')
  } catch (e) {
    error.value = e?.response?.data?.message || t('profile.saveFailed')
  } finally {
    savingAthlete.value = false
  }
}

function onAvatarError() {
  avatarBroken.value = true
}

watch(
  () => userForm.avatarUrl,
  () => {
    avatarBroken.value = false
  },
)

function formatAchDate(d) {
  if (!d) return '—'
  return d
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="app-card p-4 p-md-5 kn-profile">
      <header class="kn-page-header pb-3 border-bottom border-light mb-4">
        <h1 class="kn-page-header__title">{{ t('profile.title') }}</h1>
        <p class="kn-page-header__desc">{{ t('profile.subtitle') }}</p>
      </header>

      <div v-if="loading" class="d-flex align-items-center gap-2 app-muted py-5">
        <div class="spinner-border spinner-border-sm" aria-hidden="true" />
        {{ t('ui.loading') }}
      </div>

      <template v-else>
        <div v-if="message" class="alert alert-success py-2">{{ message }}</div>
        <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>

        <div class="kn-profile__hero">
          <div class="kn-profile__avatar-wrap">
            <img
              v-if="displayAvatarUrl && !avatarBroken"
              :src="displayAvatarUrl"
              alt=""
              class="kn-profile__avatar"
              referrerpolicy="no-referrer"
              @error="onAvatarError"
            />
            <div
              v-if="!displayAvatarUrl || avatarBroken"
              class="kn-profile__avatar-fallback"
              aria-hidden="true"
            >
              {{ initials }}
            </div>
          </div>
          <div class="kn-profile__hero-text">
            <h2 class="kn-profile__name">{{ auth.user?.fullName || '—' }}</h2>
            <span class="kn-profile__role-pill">{{ auth.user?.role }}</span>
            <p class="app-muted small mt-2 mb-0">{{ auth.user?.email }}</p>
          </div>
        </div>

        <section class="kn-profile__section">
          <h3 class="kn-profile__section-title">{{ t('profile.sectionAccount') }}</h3>
          <div class="row g-3">
            <div class="col-12">
              <BaseInput
                v-model="userForm.avatarUrl"
                :label="t('profile.avatarUrl')"
                :placeholder="t('profile.avatarPlaceholder')"
              />
              <p class="small app-muted mb-0">{{ t('profile.avatarHint') }}</p>
            </div>
            <div class="col-12 col-md-6">
              <BaseInput v-model="userForm.phone" :label="t('auth.phone')" placeholder="+998 …" autocomplete="tel" />
            </div>
          </div>
          <BaseButton class="mt-2" :loading="savingUser" @click="saveUser">{{ t('profile.saveAccount') }}</BaseButton>
        </section>

        <template v-if="isAthlete && athleteStore.profile">
          <section class="kn-profile__section">
            <h3 class="kn-profile__section-title">{{ t('profile.sectionSports') }}</h3>
            <div class="row g-3">
              <div class="col-12 col-md-6">
                <label class="form-label fw-semibold">{{ t('profile.sportType') }}</label>
                <input v-model="athleteForm.sportType" class="form-control" type="text" />
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label fw-semibold">{{ t('profile.heightCm') }}</label>
                <input v-model="athleteForm.heightCm" class="form-control" type="number" min="120" max="250" />
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label fw-semibold">{{ t('profile.bodyWeightKg') }}</label>
                <input v-model="athleteForm.bodyWeightKg" class="form-control" type="number" min="30" max="250" step="0.1" />
              </div>
              <div class="col-12 col-md-6">
                <label class="form-label fw-semibold">{{ t('auth.region') }}</label>
                <select v-model="athleteForm.region" class="form-select">
                  <option value="TASHKENT">Tashkent</option>
                  <option value="SAMARKAND">Samarkand</option>
                  <option value="BUKHARA">Bukhara</option>
                  <option value="ANDIJAN">Andijan</option>
                  <option value="NAMANGAN">Namangan</option>
                  <option value="FERGANA">Fergana</option>
                </select>
              </div>
              <div class="col-12 col-md-6">
                <label class="form-label fw-semibold">{{ t('auth.weightCategory') }}</label>
                <select v-model="athleteForm.weightCategory" class="form-select">
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
                <BaseInput v-model="athleteForm.dateOfBirth" :label="t('auth.dateOfBirth')" type="date" />
              </div>
              <div class="col-12 col-md-6">
                <BaseInput v-model="athleteForm.rank" :label="t('auth.rank')" />
              </div>
              <div class="col-12">
                <label class="form-label fw-semibold">{{ t('profile.goal') }}</label>
                <textarea v-model="athleteForm.goalText" class="form-control" rows="3" />
              </div>
              <div class="col-12 col-md-4">
                <label class="form-label fw-semibold">{{ t('profile.medalsGold') }}</label>
                <input v-model.number="athleteForm.currentMedalCountGold" class="form-control" type="number" min="0" />
              </div>
              <div class="col-12 col-md-4">
                <label class="form-label fw-semibold">{{ t('profile.medalsSilver') }}</label>
                <input v-model.number="athleteForm.currentMedalCountSilver" class="form-control" type="number" min="0" />
              </div>
              <div class="col-12 col-md-4">
                <label class="form-label fw-semibold">{{ t('profile.medalsBronze') }}</label>
                <input v-model.number="athleteForm.currentMedalCountBronze" class="form-control" type="number" min="0" />
              </div>
            </div>
            <BaseButton class="mt-2" :loading="savingAthlete" @click="saveAthlete">
              {{ t('profile.saveSports') }}
            </BaseButton>
          </section>

          <section class="kn-profile__section">
            <h3 class="kn-profile__section-title">{{ t('profile.achievementsTitle') }}</h3>
            <p v-if="!achievements.length" class="app-muted small mb-0">{{ t('profile.noAchievements') }}</p>
            <div v-else class="kn-profile__ach-list">
              <div v-for="a in achievements" :key="a.id" class="kn-profile__ach-item">
                <span class="kn-profile__ach-name">{{ a.competitionName }}</span>
                <span class="kn-profile__ach-meta">
                  {{ formatAchDate(a.competitionDate) }} · {{ a.medalType }} · {{ a.competitionLevel }}
                </span>
              </div>
            </div>
          </section>
        </template>

        <section v-if="isCoach && coachProfile" class="kn-profile__section">
          <h3 class="kn-profile__section-title">{{ t('profile.coachData') }}</h3>
          <div class="row g-3">
            <div class="col-md-6">
              <div class="border rounded-3 p-3 kn-profile-stat">
                <div class="small app-muted">{{ t('auth.region') }}</div>
                <div class="fw-semibold">{{ coachProfile.region || '—' }}</div>
              </div>
            </div>
            <div class="col-md-6">
              <div class="border rounded-3 p-3 kn-profile-stat">
                <div class="small app-muted">{{ t('profile.experienceYears') }}</div>
                <div class="fw-semibold">{{ coachProfile.experienceYears ?? '—' }}</div>
              </div>
            </div>
          </div>
        </section>

        <section v-if="auth.user?.role === 'ADMIN'" class="kn-profile__section">
          <p class="app-muted small mb-0">{{ t('profile.adminNote') }}</p>
        </section>
      </template>
    </div>
  </AppShell>
</template>
