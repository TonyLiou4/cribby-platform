export const colors = {
  background: '#F5F5F7',
  surface: '#FFFFFF',
  primary: '#007AFF',
  text: '#111827',
  secondaryText: '#6B7280',
  border: '#E5E5EA',
  success: '#34C759',
  warning: '#FF9500',
  error: '#FF3B30',
  disabled: '#C7C7CC',
  overlay: 'rgba(17, 24, 39, 0.24)',
} as const;

export type ColorToken = keyof typeof colors;
