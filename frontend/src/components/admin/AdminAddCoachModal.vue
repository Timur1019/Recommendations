<script setup>
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import BaseInput from '../common/BaseInput.vue'
import BaseButton from '../common/BaseButton.vue'
import { authApi } from '../../services/api/authApi'
import './AdminAddAthleteModal.css'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'created'])

const { t } = useI18n()

const form = reactive({
  email: '',
  password: '',
  fullName: '',
  phone: '',
  region: 'TASHKENT',
  federationMember: false,
  experienceYears: '',
  loading: false,
  error: '',
})

function resetForm() {
  form.email = ''
  form.password = ''
  form.fullName = ''
  form.phone = ''
  form.region = 'TASHKENT'
  form.federationMember = false
  form.experienceYears = ''
  form.error = ''
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) resetForm()
  },
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  form.error = ''
  if (!form.email?.trim() || !form.password || !form.fullName?.trim()) {
    form.error = t('admin.addAthleteValidationRequired')
    return
  }
  form.loading = true
  try {
    const expRaw = (form.experienceYears || '').trim()
    const expNum = expRaw === '' ? null : Number(expRaw)
    const experienceYears = expRaw !== '' && Number.isFinite(expNum) ? expNum : null
    await authApi.registerCoach({
      email: form.email.trim(),
      password: form.password,
      fullName: form.fullName.trim(),
      phone: form.phone?.trim() || null,
      region: form.region,
      federationMember: Boolean(form.federationMember),
      experienceYears,
    })
    emit('created')
    close()
  } catch (e) {
    form.error = e?.response?.data?.message || t('auth.errors.registerFailed')
  } finally {
    form.loading = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="admin-modal"
      role="dialog"
      aria-modal="true"
      :aria-label="t('admin.addCoachModalTitle')"
      @click.self="close"
    >
      <div class="admin-modal__dialog app-card bg-white p-4 p-md-4" @click.stop>
        <div class="admin-modal__header">
          <div>
            <div class="admin-modal__title">{{ t('admin.addCoachModalTitle') }}</div>
            <div class="admin-modal__subtitle">{{ t('admin.addCoachModalHint') }}</div>
          </div>
          <button type="button" class="btn btn-sm btn-outline-secondary admin-modal__close" @click="close">
            {{ t('common.close') }}
          </button>
        </div>

        <div class="row g-3">
          <div class="col-12 col-md-6">
            <BaseInput v-model="form.fullName" :label="t('auth.fullName')" autocomplete="name" />
          </div>
          <div class="col-12 col-md-6">
            <BaseInput v-model="form.phone" :label="t('auth.phone')" autocomplete="tel" />
          </div>
          <div class="col-12 col-md-6">
            <BaseInput v-model="form.email" :label="t('auth.email')" autocomplete="email" />
          </div>
          <div class="col-12 col-md-6">
            <BaseInput v-model="form.password" :label="t('auth.password')" type="password" autocomplete="new-password" />
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
          <div class="col-12 col-md-6">
            <BaseInput
              v-model="form.experienceYears"
              :label="t('auth.experienceYears')"
              type="number"
              min="0"
              max="80"
              placeholder="—"
            />
          </div>
          <div class="col-12">
            <div class="form-check">
              <input id="fed-member" v-model="form.federationMember" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="fed-member">{{ t('admin.federationMember') }}</label>
            </div>
          </div>
        </div>

        <div v-if="form.error" class="alert alert-danger mt-3 py-2 mb-0">{{ form.error }}</div>

        <div class="d-flex gap-2 justify-content-end mt-4">
          <button type="button" class="btn btn-outline-secondary" :disabled="form.loading" @click="close">
            {{ t('common.cancel') }}
          </button>
          <BaseButton variant="primary" :loading="form.loading" @click="submit">{{ t('common.create') }}</BaseButton>
        </div>
      </div>
    </div>
  </Teleport>
</template>
