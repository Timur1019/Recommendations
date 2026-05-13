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
  weightCategory: '73',
  dateOfBirth: '',
  rank: '',
  loading: false,
  error: '',
})

function resetForm() {
  form.email = ''
  form.password = ''
  form.fullName = ''
  form.phone = ''
  form.region = 'TASHKENT'
  form.weightCategory = '73'
  form.dateOfBirth = ''
  form.rank = ''
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
  if (!form.dateOfBirth) {
    form.error = t('admin.addAthleteDobRequired')
    return
  }
  form.loading = true
  try {
    await authApi.registerAthlete({
      email: form.email.trim(),
      password: form.password,
      fullName: form.fullName.trim(),
      phone: form.phone?.trim() || null,
      region: form.region,
      weightCategory: form.weightCategory,
      dateOfBirth: form.dateOfBirth,
      rank: form.rank?.trim() || null,
      coachId: null,
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
      :aria-label="t('admin.addAthleteModalTitle')"
      @click.self="close"
    >
      <div class="admin-modal__dialog app-card bg-white p-4 p-md-4" @click.stop>
        <div class="admin-modal__header">
          <div>
            <div class="admin-modal__title">{{ t('admin.addAthleteModalTitle') }}</div>
            <div class="admin-modal__subtitle">{{ t('admin.addAthleteModalHint') }}</div>
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
            <BaseInput v-model="form.rank" :label="t('auth.rank')" />
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
