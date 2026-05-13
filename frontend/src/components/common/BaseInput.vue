<script setup>
defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  error: { type: String, default: '' },
  autocomplete: { type: String, default: '' },
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="mb-3">
    <label v-if="label" class="form-label fw-semibold">{{ label }}</label>
    <input
      class="form-control"
      :class="{ 'is-invalid': Boolean(error) }"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <div v-if="error" class="invalid-feedback">{{ error }}</div>
  </div>
</template>

