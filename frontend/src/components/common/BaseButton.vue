<script setup>
defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: null }, // sm | lg
  block: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' },
  /** Скругление «капсула» для главных CTA */
  pill: { type: Boolean, default: false },
})
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[
      `btn-${variant}`,
      size ? `btn-${size}` : null,
      block ? 'w-100' : null,
      pill ? 'rounded-pill' : null,
    ]"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
    <slot />
  </button>
</template>

