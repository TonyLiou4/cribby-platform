import { StyleSheet, Text, View, type ViewProps } from 'react-native';

import { colors, radius, spacing, typography } from '@/theme';

type BadgeTone = 'neutral' | 'success' | 'warning' | 'error';

export type BadgeProps = ViewProps & {
  label: string;
  tone?: BadgeTone;
};

export function Badge({ label, tone = 'neutral', style, ...viewProps }: BadgeProps) {
  return (
    <View style={[styles.badge, styles[tone], style]} {...viewProps}>
      <Text style={[styles.label, tone !== 'neutral' && styles.inverseLabel]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: 'flex-start',
    borderRadius: radius.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
  },
  neutral: {
    backgroundColor: colors.border,
  },
  success: {
    backgroundColor: colors.success,
  },
  warning: {
    backgroundColor: colors.warning,
  },
  error: {
    backgroundColor: colors.error,
  },
  label: {
    ...typography.caption,
    color: colors.text,
    fontWeight: '600',
  },
  inverseLabel: {
    color: colors.surface,
  },
});
