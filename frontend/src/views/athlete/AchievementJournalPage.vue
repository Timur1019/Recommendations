<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppShell from '../../layouts/AppShell.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { useI18n } from 'vue-i18n'
import { useAthleteStore } from '../../stores/athlete'
import { useAuthStore } from '../../stores/auth'
import { achievementApi } from '../../services/api/achievementApi'
import './achievement-journal.css'

const { t } = useI18n()
const athleteStore = useAthleteStore()
const auth = useAuthStore()

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const LEVELS = ['REGIONAL', 'NATIONAL', 'INTERNATIONAL', 'WORLD']
const MEDALS = [
  { value: 'GOLD', labelKey: 'journal.medalGold' },
  { value: 'SILVER', labelKey: 'journal.medalSilver' },
  { value: 'BRONZE', labelKey: 'journal.medalBronze' },
  { value: 'SECOND_PLACE', labelKey: 'journal.medal2' },
  { value: 'THIRD_PLACE', labelKey: 'journal.medal3' },
  { value: 'PARTICIPATION', labelKey: 'journal.medalPart' },
]

const submitting = ref(false)
const savingName = ref(false)
const loadingInsight = ref(false)
const error = ref('')
const insight = ref(null)
const aiStatus = ref(null)

const identity = reactive({
  firstName: '',
  lastName: '',
})

const form = reactive({
  items: [newAchievement()],
})

function splitFullName(full) {
  const s = (full || '').trim()
  if (!s) return { firstName: '', lastName: '' }
  const i = s.indexOf(' ')
  if (i < 0) return { firstName: s, lastName: '' }
  return { firstName: s.slice(0, i), lastName: s.slice(i + 1).trim() }
}

function newAchievement() {
  return {
    competitionName: '',
    competitionDate: '',
    competitionLevel: 'NATIONAL',
    medalType: 'GOLD',
    medalPhotoUrl: '',
    weeks: [newWeek()],
  }
}

function newWeek() {
  return {
    weekStartDate: '',
    slots: [{ dayOfWeek: 'MONDAY', time: '', activity: '' }],
  }
}

function addAchievement() {
  form.items.push(newAchievement())
}

function removeAchievement(i) {
  if (form.items.length > 1) form.items.splice(i, 1)
}

function addWeek(ai) {
  form.items[ai].weeks.push(newWeek())
}

function removeWeek(ai, wi) {
  if (form.items[ai].weeks.length > 1) form.items[ai].weeks.splice(wi, 1)
}

function addSlot(ai, wi) {
  form.items[ai].weeks[wi].slots.push({ dayOfWeek: 'MONDAY', time: '', activity: '' })
}

function removeSlot(ai, wi, si) {
  const slots = form.items[ai].weeks[wi].slots
  if (slots.length > 1) slots.splice(si, 1)
}

function dayLabel(d) {
  return t(`journal.days.${d}`)
}

function hasModelFocus(ins, d) {
  const v = ins?.modelFocusByDay?.[d]
  if (v == null || typeof v !== 'string') return false
  const t = v.trim()
  return t.length > 0 && t !== '—'
}

function buildWeekPayload(week) {
  const byDay = {}
  for (const s of week.slots) {
    const time = (s.time || '').trim()
    const act = (s.activity || '').trim()
    if (!time || !act) continue
    const d = s.dayOfWeek
    if (!byDay[d]) byDay[d] = []
    byDay[d].push({ time, activity: act })
  }
  const days = DAYS.filter((d) => byDay[d]?.length).map((d) => ({ dayOfWeek: d, entries: byDay[d] }))
  if (!days.length) return null
  return { weekStartDate: week.weekStartDate, days }
}

const chartMax = computed(() => {
  const rows = insight.value?.chartByWeekday || []
  const m = Math.max(0, ...rows.map((r) => Number(r.activityCount) || 0))
  return m || 1
})

async function saveDisplayName() {
  error.value = ''
  const fn = identity.firstName?.trim()
  const ln = identity.lastName?.trim()
  if (!fn || !ln) {
    error.value = t('journal.nameBothRequired')
    return
  }
  savingName.value = true
  try {
    await athleteStore.patchDisplayName(fn, ln)
    await auth.fetchMe()
  } catch (e) {
    error.value = e?.response?.data?.message || t('journal.nameSaveFailed')
  } finally {
    savingName.value = false
  }
}

async function submit() {
  error.value = ''
  const athleteId = athleteStore.profile?.id
  if (!athleteId) {
    error.value = t('journal.noAthleteProfile')
    return
  }
  const items = []
  for (const it of form.items) {
    if (!it.competitionName?.trim() || !it.competitionDate) {
      error.value = t('journal.fillCompetition')
      return
    }
    const weeks = it.weeks.map(buildWeekPayload).filter(Boolean)
    if (!weeks.length) {
      error.value = t('journal.needWeekSchedule')
      return
    }
    items.push({
      competitionName: it.competitionName.trim(),
      competitionDate: it.competitionDate,
      competitionLevel: it.competitionLevel,
      medalType: it.medalType,
      medalPhotoUrl: it.medalPhotoUrl?.trim() || null,
      weeks,
    })
  }
  submitting.value = true
  try {
    await achievementApi.requestBatch({ athleteId, items })
    form.items = [newAchievement()]
    await loadInsight()
  } catch (e) {
    error.value = e?.response?.data?.message || t('journal.submitFailed')
  } finally {
    submitting.value = false
  }
}

async function loadInsight() {
  loadingInsight.value = true
  try {
    insight.value = await achievementApi.myInsight()
  } catch {
    insight.value = null
  } finally {
    loadingInsight.value = false
  }
}

async function loadAiStatus() {
  try {
    aiStatus.value = await achievementApi.aiStatus()
  } catch {
    aiStatus.value = null
  }
}

onMounted(async () => {
  await athleteStore.fetchMyProfile()
  const full = athleteStore.profile?.user?.fullName
  Object.assign(identity, splitFullName(full))
  await Promise.all([loadInsight(), loadAiStatus()])
})
</script>

<template>
  <AppShell>
    <div class="aj-page">
      <header class="aj-hero">
        <div class="aj-hero__inner">
          <p class="aj-hero__kicker">{{ t('journal.heroKicker') }}</p>
          <h1 class="aj-hero__title font-headline">{{ t('journal.title') }}</h1>
          <p class="aj-hero__lead">{{ t('journal.subtitle') }}</p>
          <RouterLink class="aj-hero__link" to="/my-achievements-media">{{ t('nav.achievementGallery') }} →</RouterLink>
        </div>
      </header>

      <div v-if="error" class="alert alert-danger rounded-3 shadow-sm">{{ error }}</div>

      <section class="aj-card">
        <div class="aj-card__head">
          <div>
            <h2 class="aj-card__title">{{ t('journal.athleteCardTitle') }}</h2>
            <p class="aj-card__subtitle">{{ t('journal.athleteCardHint') }}</p>
          </div>
        </div>
        <div class="aj-identity-grid">
          <div class="aj-field">
            <label class="d-block">{{ t('journal.firstName') }}</label>
            <input v-model="identity.firstName" class="form-control aj-input" type="text" autocomplete="given-name" />
          </div>
          <div class="aj-field">
            <label class="d-block">{{ t('journal.lastName') }}</label>
            <input v-model="identity.lastName" class="form-control aj-input" type="text" autocomplete="family-name" />
          </div>
          <div>
            <button type="button" class="aj-btn-primary w-100" :disabled="savingName" @click="saveDisplayName">
              {{ savingName ? t('ui.loading') : t('journal.saveName') }}
            </button>
          </div>
        </div>
        <div v-if="auth.user?.email" class="aj-email-pill">
          <span class="material-symbols-outlined aj-email-pill__icon" aria-hidden="true">mail</span>
          {{ auth.user.email }}
        </div>
      </section>

      <section class="aj-card">
        <div class="aj-card__head">
          <div>
            <h2 class="aj-card__title">{{ t('journal.aiCardTitle') }}</h2>
            <p class="aj-card__subtitle">{{ t('journal.aiCardSubtitle') }}</p>
          </div>
        </div>
        <div v-if="aiStatus" class="aj-ai-status">
          <span
            class="aj-badge"
            :class="aiStatus.llmConfigured ? 'aj-badge--ok' : 'aj-badge--off'"
          >
            {{ aiStatus.llmConfigured ? t('journal.aiConfigured') : t('journal.aiNotConfigured') }}
          </span>
          <span class="small text-muted">{{ aiStatus.baseUrl }} · {{ aiStatus.model }}</span>
        </div>
        <p class="small text-muted mb-0 mt-2">{{ t('journal.aiSecurityNote') }}</p>
        <pre class="aj-code">{{ t('journal.aiYamlBlock') }}</pre>
      </section>

      <div
        v-for="(it, ai) in form.items"
        :key="ai"
        class="aj-achievement"
      >
        <div class="aj-achievement__accent" aria-hidden="true" />
        <div class="aj-achievement__body">
          <div class="aj-achievement__top">
            <div>
              <div class="aj-step-label">{{ t('journal.stepAchievement') }}</div>
              <h3 class="aj-achievement__title">{{ t('journal.achievementN', { n: ai + 1 }) }}</h3>
            </div>
            <button
              v-if="form.items.length > 1"
              type="button"
              class="aj-btn-outline btn-sm"
              @click="removeAchievement(ai)"
            >
              {{ t('journal.removeAchievement') }}
            </button>
          </div>

          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label small fw-semibold text-uppercase text-muted">{{ t('journal.competitionName') }}</label>
              <input v-model="it.competitionName" class="form-control aj-input" />
            </div>
            <div class="col-md-3">
              <label class="form-label small fw-semibold text-uppercase text-muted">{{ t('journal.competitionDate') }}</label>
              <input v-model="it.competitionDate" class="form-control aj-input" type="date" />
            </div>
            <div class="col-md-3">
              <label class="form-label small fw-semibold text-uppercase text-muted">{{ t('journal.levelLabel') }}</label>
              <div class="aj-pill-grid">
                <button
                  v-for="lv in LEVELS"
                  :key="lv"
                  type="button"
                  class="aj-pill"
                  :class="{ 'aj-pill--active': it.competitionLevel === lv }"
                  @click="it.competitionLevel = lv"
                >
                  {{ t(`journal.level.${lv}`) }}
                </button>
              </div>
            </div>
          </div>

          <div class="aj-section-label">{{ t('journal.medal') }}</div>
          <div class="aj-pill-grid">
            <button
              v-for="m in MEDALS"
              :key="m.value"
              type="button"
              class="aj-pill"
              :class="{ 'aj-pill--active': it.medalType === m.value }"
              @click="it.medalType = m.value"
            >
              {{ t(m.labelKey) }}
            </button>
          </div>

          <div class="aj-section-label">{{ t('journal.weeksTitle') }}</div>
          <div v-for="(wk, wi) in it.weeks" :key="wi" class="aj-week">
            <div class="aj-week__head">
              <div class="flex-grow-1 aj-week-date-wrap">
                <label class="form-label small fw-semibold text-uppercase text-muted mb-1">{{ t('journal.weekStart') }}</label>
                <input v-model="wk.weekStartDate" class="form-control aj-input" type="date" />
              </div>
              <button
                v-if="it.weeks.length > 1"
                type="button"
                class="aj-btn-outline btn-sm align-self-end"
                @click="removeWeek(ai, wi)"
              >
                {{ t('journal.removeWeek') }}
              </button>
            </div>
            <div class="aj-table-wrap">
              <table class="aj-table table table-sm mb-0">
                <thead>
                  <tr>
                    <th>{{ t('journal.colDay') }}</th>
                    <th>{{ t('journal.colTime') }}</th>
                    <th>{{ t('journal.colActivity') }}</th>
                    <th class="aj-table-col-actions" />
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(sl, si) in wk.slots" :key="si">
                    <td>
                      <select v-model="sl.dayOfWeek" class="form-select form-select-sm aj-input">
                        <option v-for="d in DAYS" :key="d" :value="d">{{ dayLabel(d) }}</option>
                      </select>
                    </td>
                    <td><input v-model="sl.time" class="form-control form-control-sm aj-input" placeholder="06:00" /></td>
                    <td><input v-model="sl.activity" class="form-control form-control-sm aj-input" /></td>
                    <td>
                      <button
                        v-if="wk.slots.length > 1"
                        type="button"
                        class="aj-link-btn"
                        :aria-label="t('common.close')"
                        @click="removeSlot(ai, wi, si)"
                      >
                        ×
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <button type="button" class="aj-btn-outline btn-sm mt-2" @click="addSlot(ai, wi)">
              + {{ t('journal.addSlot') }}
            </button>
          </div>
          <button type="button" class="aj-btn-outline btn-sm" @click="addWeek(ai)">
            + {{ t('journal.addWeek') }}
          </button>
        </div>
      </div>

      <div class="aj-toolbar">
        <button type="button" class="aj-btn-outline" @click="addAchievement">
          + {{ t('journal.addAchievement') }}
        </button>
        <BaseButton variant="primary" :loading="submitting" @click="submit">{{ t('journal.saveAll') }}</BaseButton>
        <BaseButton variant="outline-secondary" :loading="loadingInsight" @click="loadInsight">
          {{ t('journal.refreshInsight') }}
        </BaseButton>
      </div>

      <section v-if="insight" class="aj-insight">
        <h2 class="fs-5 font-headline fw-bold mb-3">{{ t('journal.insightTitle') }}</h2>
        <p class="aj-insight__text">{{ insight.analysisText }}</p>
        <h3 class="fs-6 fw-bold mb-3">{{ t('journal.suggestedPlan') }}</h3>
        <p class="small text-muted mb-2">{{ t('journal.planHint') }}</p>
        <div class="aj-plan-grid">
          <div v-for="d in DAYS" :key="d" class="aj-plan-item">
            <div class="aj-plan-item__day">{{ dayLabel(d) }}</div>
            <div class="aj-plan-item__body">
              <div
                v-if="(insight.suggestedWeekPlan?.[d] || '').trim()"
                class="aj-plan-line aj-plan-line--journal"
              >
                <span class="aj-plan-line__label">{{ t('journal.planFromJournal') }}</span>
                <span class="aj-plan-line__text">{{ insight.suggestedWeekPlan[d] }}</span>
              </div>
              <div
                v-if="hasModelFocus(insight, d)"
                class="aj-plan-line aj-plan-line--model"
              >
                <span class="aj-plan-line__label">{{ t('journal.planFromModel') }}</span>
                <span class="aj-plan-line__text">{{ insight.modelFocusByDay[d] }}</span>
              </div>
              <div
                v-if="!((insight.suggestedWeekPlan?.[d] || '').trim()) && !hasModelFocus(insight, d)"
                class="aj-plan-line aj-plan-line--empty"
              >
                —
              </div>
            </div>
          </div>
        </div>
        <h3 class="fs-6 fw-bold mt-4 mb-2">{{ t('journal.chartTitle') }}</h3>
        <div class="aj-bars">
          <div v-for="row in insight.chartByWeekday" :key="row.dayOfWeek" class="aj-bar-row">
            <span class="fw-semibold">{{ dayLabel(row.dayOfWeek) }}</span>
            <div class="aj-bar-track">
              <div
                class="aj-bar-fill"
                :style="{ width: `${Math.min(100, ((row.activityCount || 0) / chartMax) * 100)}%` }"
              />
            </div>
            <span class="aj-bar-count">{{ row.activityCount }}</span>
          </div>
        </div>
      </section>
    </div>
  </AppShell>
</template>
