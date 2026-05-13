<script setup>
import { computed, onMounted, ref } from 'vue'
import BaseButton from '../../components/common/BaseButton.vue'
import AdminAddAthleteModal from '../../components/admin/AdminAddAthleteModal.vue'
import AdminAddCoachModal from '../../components/admin/AdminAddCoachModal.vue'
import { useI18n } from 'vue-i18n'
import { useAdminStore } from '../../stores/admin'
import { adminApi } from '../../services/api/adminApi'

const { t } = useI18n()
const admin = useAdminStore()

const coaches = ref([])
const assigningAthleteId = ref(null)

const q = ref('')
const role = ref('')
const active = ref('')
const showAddAthlete = ref(false)
const showAddCoach = ref(false)

onMounted(() => {
  admin.fetchUsers()
  loadCoaches()
})

async function loadCoaches() {
  try {
    coaches.value = await adminApi.listCoaches()
  } catch {
    coaches.value = []
  }
}

async function onCoachAssign(u, event) {
  const raw = event.target.value
  const coachId = raw === '' ? null : Number(raw)
  if (!u.athleteId) return
  assigningAthleteId.value = u.athleteId
  try {
    await admin.assignAthleteCoach(u.athleteId, coachId)
  } finally {
    assigningAthleteId.value = null
  }
}

function isBusy(userId) {
  return Boolean(admin.busyByUserId[userId])
}

const filteredUsers = computed(() => {
  const query = q.value.trim().toLowerCase()
  return admin.users.filter((u) => {
    if (role.value && u.role !== role.value) return false
    if (active.value === 'true' && u.active !== true) return false
    if (active.value === 'false' && u.active !== false) return false
    if (!query) return true
    return (
      String(u.id).includes(query) ||
      (u.fullName || '').toLowerCase().includes(query) ||
      (u.email || '').toLowerCase().includes(query)
    )
  })
})

function afterUserCreated() {
  admin.fetchUsers()
  loadCoaches()
}
</script>

<template>
  <div class="row g-3 kn-admin-stack">
    <div class="col-12">
      <div class="app-card kn-admin-card p-4">
        <div class="kn-admin-toolbar mb-0">
          <h2 class="kn-admin-card__title">{{ t('admin.usersModuleActions') }}</h2>
          <div class="d-flex flex-wrap gap-2">
            <BaseButton class="rounded-pill px-3" variant="primary" @click="showAddAthlete = true">{{
              t('admin.addAthlete')
            }}</BaseButton>
            <BaseButton class="rounded-pill px-3" variant="outline-primary" @click="showAddCoach = true">{{
              t('admin.addCoach')
            }}</BaseButton>
          </div>
        </div>
      </div>
    </div>

    <div class="col-12">
      <div class="app-card kn-admin-card p-4">
        <div class="kn-admin-toolbar">
          <h2 class="kn-admin-card__title mb-0">{{ t('admin.users') }}</h2>
          <button
            type="button"
            class="btn btn-sm btn-outline-secondary rounded-pill px-3"
            :disabled="admin.loading"
            @click="admin.fetchUsers()"
          >
            {{ t('ui.refresh') }}
          </button>
        </div>

        <div class="row g-2 kn-admin-filters">
          <div class="col-12 col-md-6 col-lg-5">
            <input v-model="q" class="form-control" :placeholder="t('adminUsers.searchPlaceholder')" />
          </div>
          <div class="col-6 col-md-3 col-lg-3">
            <select v-model="role" class="form-select">
              <option value="">{{ t('adminUsers.allRoles') }}</option>
              <option value="ADMIN">ADMIN</option>
              <option value="COACH">COACH</option>
              <option value="ATHLETE">ATHLETE</option>
            </select>
          </div>
          <div class="col-6 col-md-3 col-lg-2">
            <select v-model="active" class="form-select">
              <option value="">{{ t('adminUsers.allStatuses') }}</option>
              <option value="true">{{ t('adminUsers.activeOnly') }}</option>
              <option value="false">{{ t('adminUsers.inactiveOnly') }}</option>
            </select>
          </div>
        </div>

        <div v-if="admin.error" class="alert alert-danger py-2">{{ admin.error }}</div>

        <div v-if="admin.loading" class="d-flex align-items-center gap-2 app-muted">
          <div class="spinner-border spinner-border-sm" aria-hidden="true"></div>
          {{ t('ui.loading') }}
        </div>

        <div v-else class="kn-data-table-wrap">
          <div class="kn-data-table-responsive kn-data-table-responsive--auto">
            <table class="table table-sm align-middle mb-0 kn-data-table kn-data-table--admin-users">
              <thead>
                <tr>
                  <th class="kn-th--id">{{ t('adminUsers.id') }}</th>
                  <th class="kn-th--name">{{ t('adminUsers.fullName') }}</th>
                  <th class="kn-th--email">{{ t('adminUsers.email') }}</th>
                  <th class="kn-th--role">{{ t('ui.role') }}</th>
                  <th class="kn-th--coach">{{ t('admin.colAssignCoach') }}</th>
                  <th class="kn-th--status">{{ t('ui.status') }}</th>
                  <th class="kn-th--actions text-end">{{ t('ui.actions') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in filteredUsers" :key="u.id">
                  <td class="kn-td--id text-muted">{{ u.id }}</td>
                  <td class="kn-td--name fw-semibold">{{ u.fullName }}</td>
                  <td class="kn-td--email text-muted">{{ u.email }}</td>
                  <td class="kn-td--role">
                    <span class="kn-type-pill" :title="u.role">{{ u.role }}</span>
                  </td>
                  <td class="kn-td--coach">
                    <template v-if="u.role === 'ATHLETE' && u.athleteId">
                      <select
                        class="form-select form-select-sm"
                        :value="u.coachId != null ? String(u.coachId) : ''"
                        :disabled="assigningAthleteId === u.athleteId"
                        @change="onCoachAssign(u, $event)"
                      >
                        <option value="">{{ t('admin.noCoachAssigned') }}</option>
                        <option v-for="c in coaches" :key="c.id" :value="String(c.id)">
                          {{ c.fullName }} ({{ c.email }})
                        </option>
                      </select>
                    </template>
                    <span v-else class="text-muted small">—</span>
                  </td>
                  <td class="kn-td--status">
                    <span class="badge rounded-pill px-2" :class="u.active ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ u.active ? 'ACTIVE' : 'INACTIVE' }}
                    </span>
                  </td>
                  <td class="kn-td--actions text-end">
                    <div class="d-flex flex-wrap gap-1 justify-content-end align-items-center">
                      <div class="btn-group btn-group-sm kn-admin-role-group" role="group" aria-label="role">
                        <button
                          type="button"
                          class="btn btn-outline-primary"
                          :disabled="isBusy(u.id)"
                          @click="admin.changeRole(u.id, 'ATHLETE')"
                        >
                          ATHLETE
                        </button>
                        <button
                          type="button"
                          class="btn btn-outline-primary"
                          :disabled="isBusy(u.id)"
                          @click="admin.changeRole(u.id, 'COACH')"
                        >
                          COACH
                        </button>
                        <button
                          type="button"
                          class="btn btn-outline-primary"
                          :disabled="isBusy(u.id)"
                          @click="admin.changeRole(u.id, 'ADMIN')"
                        >
                          ADMIN
                        </button>
                      </div>
                      <button
                        type="button"
                        class="btn btn-sm rounded-pill px-3"
                        :class="u.active ? 'btn-outline-danger' : 'btn-outline-success'"
                        :disabled="isBusy(u.id)"
                        @click="admin.toggleActive(u.id, !u.active)"
                      >
                        {{ u.active ? t('ui.deactivate') : t('ui.activate') }}
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="filteredUsers.length === 0">
                  <td colspan="7" class="text-center app-muted py-4">{{ t('ui.empty') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>

  <AdminAddAthleteModal v-model="showAddAthlete" @created="afterUserCreated" />
  <AdminAddCoachModal v-model="showAddCoach" @created="afterUserCreated" />
</template>
