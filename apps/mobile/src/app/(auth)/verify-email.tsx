import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';

import {
  resendVerificationEmail,
  sendVerificationEmail,
  verifyEmail,
} from '@/api/auth';
import { Button, Card, LoadingSpinner, Screen, TextField } from '@/components/ui';
import { routes } from '@/constants';
import { colors, spacing, typography } from '@/theme';
import { getApiErrorMessage } from '@/utils';

const emailSchema = z.object({
  email: z.email('Enter a valid email address.'),
});

type EmailFormValues = z.infer<typeof emailSchema>;

export default function VerifyEmailScreen() {
  const { token } = useLocalSearchParams<{ token?: string }>();
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(Boolean(token));
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<EmailFormValues>({
    resolver: zodResolver(emailSchema),
    defaultValues: {
      email: '',
    },
  });

  useEffect(() => {
    const tokenValue = Array.isArray(token) ? token[0] : token;

    if (!tokenValue) {
      return;
    }

    async function verifyToken() {
      try {
        await verifyEmail(tokenValue);
        setStatusMessage('Your email has been verified. You can now sign in.');
        setStatusError(null);
      } catch (error) {
        setStatusError(getApiErrorMessage(error, 'Unable to verify this email link.'));
      } finally {
        setIsVerifying(false);
      }
    }

    void verifyToken();
  }, [token]);

  async function onSend(values: EmailFormValues) {
    try {
      await sendVerificationEmail(values);
      setStatusMessage('Verification email sent. Please check your inbox.');
      setStatusError(null);
    } catch (error) {
      setError('root', {
        message: getApiErrorMessage(error, 'Unable to send verification email.'),
      });
    }
  }

  async function onResend(values: EmailFormValues) {
    try {
      await resendVerificationEmail(values);
      setStatusMessage('Verification email resent. Please check your inbox.');
      setStatusError(null);
    } catch (error) {
      setError('root', {
        message: getApiErrorMessage(error, 'Unable to resend verification email.'),
      });
    }
  }

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>Verify email.</Text>
        <Text style={styles.subtitle}>
          Confirm your email address to help keep the Roommate Marketplace community trusted.
        </Text>
      </View>

      <Card style={styles.formCard}>
        {isVerifying ? (
          <LoadingSpinner size="large" />
        ) : (
          <>
            {statusMessage ? <Text style={styles.success}>{statusMessage}</Text> : null}
            {statusError ? <Text style={styles.errorCentered}>{statusError}</Text> : null}

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

            {errors.root?.message ? <Text style={styles.errorCentered}>{errors.root.message}</Text> : null}

            <View style={styles.actions}>
              <Button
                disabled={isSubmitting}
                loading={isSubmitting}
                onPress={handleSubmit(onSend)}
                title="Send email"
              />
              <Button
                disabled={isSubmitting}
                onPress={handleSubmit(onResend)}
                title="Resend"
                variant="secondary"
              />
            </View>
          </>
        )}
      </Card>

      <Link href={routes.auth.login} style={styles.footerLink}>
        Back to sign in
      </Link>
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
  actions: {
    gap: spacing.sm,
  },
  success: {
    ...typography.body,
    color: colors.success,
    textAlign: 'center',
  },
  errorCentered: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
  footerLink: {
    ...typography.body,
    color: colors.primary,
    fontWeight: '600',
    textAlign: 'center',
  },
});
