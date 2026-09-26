import { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { LoadingSpinner, Screen } from '@/components/ui';
import { routes } from '@/constants';
import { useAuthStore } from '@/store';
import { colors, spacing, typography } from '@/theme';

export default function IndexScreen() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const isLoading = useAuthStore((state) => state.isLoading);

  useEffect(() => {
    if (isLoading) {
      return;
    }

    router.replace(isAuthenticated ? routes.authenticated : routes.auth.login);
  }, [isAuthenticated, isLoading]);

  return (
    <Screen centered contentContainerStyle={styles.content}>
      <View style={styles.copy}>
        <Text style={styles.title}>Trusted shared housing.</Text>
        <Text style={styles.subtitle}>
          Find safe rooms and compatible roommates for students, alumni, and early-career professionals.
        </Text>
      </View>
      <LoadingSpinner size="large" />
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: {
    justifyContent: 'center',
  },
  copy: {
    gap: spacing.md,
    maxWidth: 360,
    marginBottom: spacing.lg,
  },
  title: {
    ...typography.display,
    color: colors.text,
    textAlign: 'center',
  },
  subtitle: {
    ...typography.body,
    color: colors.secondaryText,
    textAlign: 'center',
  },
});
