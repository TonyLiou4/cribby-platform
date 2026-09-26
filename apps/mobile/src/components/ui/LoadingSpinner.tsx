import { ActivityIndicator, StyleSheet, View, type ViewProps } from 'react-native';

import { colors, spacing } from '@/theme';

export type LoadingSpinnerProps = ViewProps & {
  size?: 'small' | 'large';
};

export function LoadingSpinner({ size = 'small', style, ...viewProps }: LoadingSpinnerProps) {
  return (
    <View style={[styles.container, style]} {...viewProps}>
      <ActivityIndicator size={size} color={colors.primary} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.md,
  },
});
