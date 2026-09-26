import { StyleSheet, Text, View } from 'react-native';

import { colors, radius, spacing, typography } from '@/theme';

type UploadProgressIndicatorProps = {
  progress: number;
};

export function UploadProgressIndicator({ progress }: UploadProgressIndicatorProps) {
  const percent = Math.round(progress * 100);

  return (
    <View style={styles.container}>
      <View style={styles.track}>
        <View style={[styles.fill, { width: `${percent}%` }]} />
      </View>
      <Text style={styles.label}>{percent}%</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: spacing.xs,
  },
  track: {
    backgroundColor: colors.border,
    borderRadius: radius.pill,
    height: 8,
    overflow: 'hidden',
  },
  fill: {
    backgroundColor: colors.primary,
    borderRadius: radius.pill,
    height: '100%',
  },
  label: {
    ...typography.caption,
    color: colors.secondaryText,
    textAlign: 'right',
  },
});
