import { zodResolver } from '@hookform/resolvers/zod';
import { Link, router } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';

import { register } from '@/api/auth';
import { Button, Card, Screen, TextField } from '@/components/ui';
import { routes } from '@/constants';
import { useAuthStore } from '@/store';
import { colors, radius, spacing, typography } from '@/theme';
import type { UserType } from '@/types';
import { getApiErrorMessage } from '@/utils';

const userTypes: { label: string; value: UserType }[] = [
  { label: 'Student', value: 'STUDENT' },
  { label: 'Alumni', value: 'ALUMNI' },
  { label: 'Incoming student', value: 'INCOMING_STUDENT' },
  { label: 'Professional / Other', value: 'OTHER' },
];

const registerSchema = z.object({
  firstName: z.string().trim().min(1, 'First name is required.'),
  lastName: z.string().trim().min(1, 'Last name is required.'),
  email: z.email('Enter a valid email address.'),
  password: z.string().min(8, 'Password must be at least 8 characters.'),
  userType: z.enum(['STUDENT', 'ALUMNI', 'INCOMING_STUDENT', 'OTHER'], {
    error: 'Select a user type.',
  }),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterScreen() {
  const setAuth = useAuthStore((state) => state.setAuth);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      userType: 'STUDENT',
    },
  });

  async function onSubmit(values: RegisterFormValues) {
    try {
      const auth = await register(values);
      await setAuth(auth.user, auth.accessToken, auth.refreshToken);
      router.replace(routes.authenticated);
    } catch (error) {
      setError('root', {
        message: getApiErrorMessage(error, 'Unable to create your account. Please try again.'),
      });
    }
  }

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>Create account.</Text>
        <Text style={styles.subtitle}>
          Trusted shared housing for students, alumni, and early-career professionals.
        </Text>
      </View>

      <Card style={styles.formCard}>
        <View style={styles.row}>
          <Controller
            control={control}
            name="firstName"
            render={({ field: { onBlur, onChange, value } }) => (
              <TextField
                autoComplete="given-name"
                containerStyle={styles.rowField}
                error={errors.firstName?.message}
                label="First name"
                onBlur={onBlur}
                onChangeText={onChange}
                placeholder="Alex"
                textContentType="givenName"
                value={value}
              />
            )}
          />
          <Controller
            control={control}
            name="lastName"
            render={({ field: { onBlur, onChange, value } }) => (
              <TextField
                autoComplete="family-name"
                containerStyle={styles.rowField}
                error={errors.lastName?.message}
                label="Last name"
                onBlur={onBlur}
                onChangeText={onChange}
                placeholder="Morgan"
                textContentType="familyName"
                value={value}
              />
            )}
          />
        </View>

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
              autoComplete="new-password"
              error={errors.password?.message}
              label="Password"
              onBlur={onBlur}
              onChangeText={onChange}
              placeholder="At least 8 characters"
              secureTextEntry
              textContentType="newPassword"
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="userType"
          render={({ field: { onChange, value } }) => (
            <View style={styles.fieldGroup}>
              <Text style={styles.label}>User type</Text>
              <View style={styles.optionGrid}>
                {userTypes.map((userType) => {
                  const isSelected = userType.value === value;

                  return (
                    <Pressable
                      accessibilityRole="radio"
                      accessibilityState={{ checked: isSelected }}
                      key={userType.value}
                      onPress={() => onChange(userType.value)}
                      style={[styles.option, isSelected && styles.optionSelected]}
                    >
                      <Text style={[styles.optionText, isSelected && styles.optionTextSelected]}>
                        {userType.label}
                      </Text>
                    </Pressable>
                  );
                })}
              </View>
              {errors.userType?.message ? (
                <Text style={styles.error}>{errors.userType.message}</Text>
              ) : null}
            </View>
          )}
        />

        {errors.root?.message ? <Text style={styles.errorCentered}>{errors.root.message}</Text> : null}

        <Button
          disabled={isSubmitting}
          loading={isSubmitting}
          onPress={handleSubmit(onSubmit)}
          title="Create account"
        />
      </Card>

      <View style={styles.footer}>
        <Text style={styles.footerText}>Already have an account?</Text>
        <Link href={routes.auth.login} style={styles.footerLink}>
          Sign in
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
  row: {
    flexDirection: 'row',
    gap: spacing.md,
  },
  rowField: {
    flex: 1,
    minWidth: 0,
  },
  fieldGroup: {
    gap: spacing.sm,
  },
  label: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  optionGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  option: {
    borderColor: colors.border,
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  optionSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  optionText: {
    ...typography.caption,
    color: colors.text,
    fontWeight: '600',
  },
  optionTextSelected: {
    color: colors.surface,
  },
  error: {
    ...typography.caption,
    color: colors.error,
  },
  errorCentered: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
  footer: {
    alignItems: 'center',
    flexDirection: 'row',
    flexWrap: 'wrap',
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
