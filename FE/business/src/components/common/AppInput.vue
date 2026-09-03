<script setup>
defineProps({
  modelValue: { type: [String, Number], default: '' },
  label: String,
  type: { type: String, default: 'text' },
  placeholder: String,
  error: String,
  hint: String,
  disabled: Boolean,
  autocomplete: String,
})
defineEmits(['update:modelValue', 'enter'])
</script>

<template>
  <div class="field">
    <label v-if="label" class="form-label">{{ label }}</label>
    <input
      class="field__input"
      :class="{ 'is-error': error }"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :autocomplete="autocomplete"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('enter')"
    />
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="hint" class="form-hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.field {
  width: 100%;
}
.field__input {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid var(--c-border-strong);
  border-radius: var(--r-md);
  background: var(--c-surface);
  transition:
    border-color var(--transition),
    box-shadow var(--transition);
}
.field__input::placeholder {
  color: var(--c-text-subtle);
}
.field__input:focus {
  outline: none;
  border-color: var(--c-primary-500);
  box-shadow: var(--ring);
}
.field__input.is-error {
  border-color: var(--c-danger-500);
}
.field__input:disabled {
  background: var(--c-bg-subtle);
  color: var(--c-text-muted);
}
</style>
