import { StyleSheet, Text } from 'react-native';

import { Card, Screen } from '@/components/ui';
import { colors, spacing, typography } from '@/theme';

type PlaceholderScreenProps = {
  title: string;
  message: string;
};

export function PlaceholderScreen({ title, message }: PlaceholderScreenProps) {
  return (
    <Screen centered>
      <Card style={styles.card}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.message}>{message}</Text>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  card: {
    gap: spacing.sm,
    maxWidth: 380,
    width: '100%',
  },
  title: {
    ...typography.heading,
    color: colors.text,
    textAlign: 'center',
  },
  message: {
    ...typography.body,
    color: colors.secondaryText,
    textAlign: 'center',
  },
});
