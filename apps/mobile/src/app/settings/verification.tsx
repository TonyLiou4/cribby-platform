import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Badge, Button, Card, LoadingSpinner, Screen, TextField } from '@/components/ui';
import {
  useCreateVerificationRequest,
  useCurrentUser,
  useSendVerificationEmail,
  useVerificationRequests,
} from '@/hooks';
import { colors, spacing, typography } from '@/theme';
import type { VerificationRequest, VerificationType } from '@/types';
import { getApiErrorMessage } from '@/utils';

function statusTone(status: VerificationRequest['status']) {
  if (status === 'APPROVED') {
    return 'success';
  }

  if (status === 'REJECTED') {
    return 'error';
  }

  return 'warning';
}

function formatLabel(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export default function VerificationScreen() {
  const userQuery = useCurrentUser();
  const verificationRequests = useVerificationRequests();
  const createRequest = useCreateVerificationRequest();
  const sendEmail = useSendVerificationEmail();
  const [studentProofUrl, setStudentProofUrl] = useState('');
  const [alumniProofUrl, setAlumniProofUrl] = useState('');
  const [formError, setFormError] = useState<string | undefined>();
  const [successMessage, setSuccessMessage] = useState<string | undefined>();
  const user = userQuery.data;
  const requests = verificationRequests.data ?? [];
  const isLoading = userQuery.isLoading || verificationRequests.isLoading;
  const error = userQuery.error ?? verificationRequests.error ?? createRequest.error ?? sendEmail.error;

  async function handleEmailVerification() {
    if (!user?.email) {
      return;
    }

    try {
      await sendEmail.mutateAsync(user.email);
      setSuccessMessage('Verification email sent. Please check your inbox.');
    } catch {
      setSuccessMessage(undefined);
    }
  }

  async function handleCreateRequest(type: VerificationType) {
    const proofDocumentUrl = type === 'STUDENT' ? studentProofUrl.trim() : alumniProofUrl.trim();

    setFormError(undefined);

    if (type !== 'STUDENT' && type !== 'ALUMNI') {
      setFormError('Select a verification type.');
      setSuccessMessage(undefined);
      return;
    }

    if (!proofDocumentUrl) {
      setFormError('Proof document URL is required.');
      setSuccessMessage(undefined);
      return;
    }

    try {
      await createRequest.mutateAsync({ verificationType: type, proofDocumentUrl });
      setSuccessMessage(`${formatLabel(type)} verification request submitted.`);

      if (type === 'STUDENT') {
        setStudentProofUrl('');
      } else {
        setAlumniProofUrl('');
      }
    } catch {
      setSuccessMessage(undefined);
    }
  }

  if (isLoading) {
    return (
      <Screen centered>
        <LoadingSpinner size="large" />
      </Screen>
    );
  }

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>Trust</Text>
        <Text style={styles.title}>Verification</Text>
        <Text style={styles.subtitle}>Build confidence with roommates by verifying your account.</Text>
      </View>

      {error ? <Text style={styles.error}>{getApiErrorMessage(error, 'Verification action failed.')}</Text> : null}
      {formError ? <Text style={styles.error}>{formError}</Text> : null}
      {successMessage ? <Text style={styles.success}>{successMessage}</Text> : null}

      <Card style={styles.section}>
        <Text style={styles.sectionTitle}>Current status</Text>
        <View style={styles.badges}>
          <Badge label="Email Verified" tone={user?.emailVerified ? 'success' : 'neutral'} />
          <Badge label="Student Verified" tone={user?.studentVerified ? 'success' : 'neutral'} />
          <Badge label="Alumni Verified" tone={user?.alumniVerified ? 'success' : 'neutral'} />
          <Badge label="Phone Verified" tone={user?.phoneVerified ? 'success' : 'neutral'} />
        </View>

        {!user?.emailVerified ? (
          <Button
            disabled={sendEmail.isPending}
            loading={sendEmail.isPending}
            title="Send verification email"
            variant="secondary"
            onPress={handleEmailVerification}
          />
        ) : null}
      </Card>

      <VerificationForm
        isLoading={createRequest.isPending}
        proofDocumentUrl={studentProofUrl}
        setProofDocumentUrl={setStudentProofUrl}
        type="STUDENT"
        onSubmit={() => {
          void handleCreateRequest('STUDENT');
        }}
      />

      <VerificationForm
        isLoading={createRequest.isPending}
        proofDocumentUrl={alumniProofUrl}
        setProofDocumentUrl={setAlumniProofUrl}
        type="ALUMNI"
        onSubmit={() => {
          void handleCreateRequest('ALUMNI');
        }}
      />

      <Card style={styles.section}>
        <Text style={styles.sectionTitle}>Previous requests</Text>
        {requests.length === 0 ? (
          <Text style={styles.emptyText}>No verification requests yet.</Text>
        ) : (
          requests.map((request) => (
            <View key={request.id} style={styles.requestRow}>
              <View style={styles.requestCopy}>
                <Text style={styles.requestTitle}>
                  {formatLabel(request.verificationType)}
                </Text>
                <Text style={styles.requestMeta}>
                  {new Intl.DateTimeFormat('en-US', {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                  }).format(new Date(request.createdAt))}
                </Text>
                {request.rejectionReason ? (
                  <Text style={styles.rejection}>{request.rejectionReason}</Text>
                ) : null}
              </View>
              <Badge label={formatLabel(request.status)} tone={statusTone(request.status)} />
            </View>
          ))
        )}
      </Card>
    </Screen>
  );
}

function VerificationForm({
  type,
  proofDocumentUrl,
  setProofDocumentUrl,
  isLoading,
  onSubmit,
}: {
  type: VerificationType;
  proofDocumentUrl: string;
  setProofDocumentUrl: (value: string) => void;
  isLoading: boolean;
  onSubmit: () => void;
}) {
  return (
    <Card style={styles.section}>
      <Text style={styles.sectionTitle}>{formatLabel(type)} verification</Text>
      <Text style={styles.helpText}>Paste a proof document URL for now. File upload will come later.</Text>
      <TextField
        autoCapitalize="none"
        label="Proof document URL"
        onChangeText={setProofDocumentUrl}
        placeholder="https://..."
        value={proofDocumentUrl}
      />
      <Button
        disabled={!proofDocumentUrl.trim() || isLoading}
        loading={isLoading}
        title={`Request ${formatLabel(type)} verification`}
        onPress={onSubmit}
      />
    </Card>
  );
}

const styles = StyleSheet.create({
  screen: {
    gap: spacing.md,
    paddingBottom: spacing.xl,
  },
  header: {
    gap: spacing.sm,
  },
  eyebrow: {
    ...typography.caption,
    color: colors.secondaryText,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  title: {
    ...typography.title,
    color: colors.text,
  },
  subtitle: {
    ...typography.body,
    color: colors.secondaryText,
  },
  section: {
    gap: spacing.md,
  },
  sectionTitle: {
    ...typography.heading,
    color: colors.text,
  },
  badges: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  helpText: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  requestRow: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: spacing.md,
    justifyContent: 'space-between',
  },
  requestCopy: {
    flex: 1,
    gap: spacing.xs,
  },
  requestTitle: {
    ...typography.body,
    color: colors.text,
    fontWeight: '700',
  },
  requestMeta: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  rejection: {
    ...typography.caption,
    color: colors.error,
  },
  emptyText: {
    ...typography.body,
    color: colors.secondaryText,
  },
  error: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
  success: {
    ...typography.caption,
    color: colors.success,
    textAlign: 'center',
  },
});
