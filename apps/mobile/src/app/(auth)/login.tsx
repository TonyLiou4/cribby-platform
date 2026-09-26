import { zodResolver } from '@hookform/resolvers/zod';
import { Link, router } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';

import { login } from '@/api/auth';
import { Button, Card, Screen, TextField } from '@/components/ui';
import { routes } from '@/constants';
import { useAuthStore } from '@/store';
import { colors, spacing, typography } from '@/theme';
import { getApiErrorMessage } from '@/utils';

const loginSchema = z.object({
  email: z.email('Enter a valid email address.'),
  password: z.string().min(8, 'Password must be at least 8 characters.'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginScreen() {
  const setAuth = useAuthStore((state) => state.setAuth);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  async function onSubmit(values: LoginFormValues) {
    try {
      const auth = await login(values);
      await setAuth(auth.user, auth.accessToken, auth.refreshToken);
      router.replace(routes.authenticated);
    } catch (error) {
      setError('root', {
        message: getApiErrorMessage(error, 'Unable to sign in. Please try again.'),
      });
    }
  }

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>Welcome back.</Text>
        <Text style={styles.subtitle}>Sign in to continue finding safe rooms and compatible roommates.</Text>
      </View>

      <Card style={styles.formCard}>
        <Controller
          control={control}
          name="email"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              autoCapitalize="none"
              autoComplete="email"
              autoCorrect={false}
              error={errors.email?.message}
              keyboardType="email-address"
              label="Email"
              onBlur={onBlur}
              onChangeText={onChange}
              placeholder="you@example.com"
              textContentType="emailAddress"
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="password"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              autoCapitalize="none"
              autoComplete="password"
              error={errors.password?.message}
              label="Password"
              onBlur={onBlur}
              onChangeText={onChange}
              placeholder="Enter your password"
              secureTextEntry
              textContentType="password"
              value={value}
            />
          )}
        />

        {errors.root?.message ? <Text style={styles.error}>{errors.root.message}</Text> : null}

        <Button
          disabled={isSubmitting}
          loading={isSubmitting}
          onPress={handleSubmit(onSubmit)}
          title="Sign in"
        />
      </Card>

      <View style={styles.footer}>
        <Text style={styles.footerText}>New here?</Text>
        <Link href={routes.auth.register} style={styles.footerLink}>
          Create an account
        </Link>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  screen: {
    justifyContent: 'center',
    gap: spacing.lg,
  },
  header: {
    gap: spacing.sm,
  },
  title: {
    ...typography.title,
    color: colors.text,
    textAlign: 'center',
  },
  subtitle: {
    ...typography.body,
    color: colors.secondaryText,
    textAlign: 'center',
  },
  formCard: {
    gap: spacing.md,
  },
  error: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
  footer: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.xs,
    justifyContent: 'center',
  },
  footerText: {
    ...typography.body,
    color: colors.secondaryText,
  },
  footerLink: {
    ...typography.body,
    color: colors.primary,
    fontWeight: '600',
  },
});
