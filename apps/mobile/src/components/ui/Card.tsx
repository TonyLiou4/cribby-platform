import type { PropsWithChildren } from 'react';
import { StyleSheet, View, type ViewProps } from 'react-native';

import { colors, radius, shadows, spacing } from '@/theme';

export type CardProps = PropsWithChildren<ViewProps & {
  elevated?: boolean;
}>;

export function Card({ children, elevated = true, style, ...viewProps }: CardProps) {
  return (
    <View style={[styles.card, elevated && shadows.sm, style]} {...viewProps}>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    padding: spacing.md,
  },
});
