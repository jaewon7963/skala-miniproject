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
  height: 40px;
  padding: 0 13px;
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-md);
  background: var(--mat-fill);
  transition:
    border-color var(--transition),
    background var(--transition),
    box-shadow var(--transition);
}
.field__input::placeholder {
  color: var(--c-text-subtle);
}
.field__input:hover:not(:disabled):not(:focus) {
  background: var(--mat-fill-strong);
}
.field__input:focus {
  outline: none;
  background: var(--c-surface);
  border-color: var(--c-primary-400);
  box-shadow: var(--ring);
}
.field__input.is-error {
  border-color: var(--c-danger-500);
}
.field__input:disabled {
  color: var(--c-text-muted);
  opacity: 0.7;
}
</style>
