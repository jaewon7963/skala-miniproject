<script setup>
defineProps({
  modelValue: { type: [String, Number, null], default: '' },
  options: { type: Array, default: () => [] }, // [{ value, label }]
  label: String,
  size: { type: String, default: 'md' },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <label class="select" :class="`select--${size}`">
    <span v-if="label" class="select__label">{{ label }}</span>
    <select
      class="select__control"
      :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value)"
    >
      <option v-for="option in options" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
  </label>
</template>

<style scoped>
.select {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 8px 0 12px;
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-full);
  background: var(--mat-fill);
  backdrop-filter: blur(var(--mat-blur-sm)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur-sm)) saturate(var(--mat-saturate));
  box-shadow: var(--shadow-inner-top);
  transition: background var(--transition);
}
.select:hover {
  background: var(--mat-fill-strong);
}
.select--sm {
  height: 28px;
}
.select__label {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  white-space: nowrap;
}
.select__control {
  border: none;
  background: none;
  outline: none;
  font-size: var(--fs-md);
  font-weight: 600;
  padding-right: 2px;
  cursor: pointer;
}
</style>
