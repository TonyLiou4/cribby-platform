import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { logout } from '@/api/auth';
import { Avatar, Badge, Button, Card, LoadingSpinner, Screen } from '@/components/ui';
import { routes } from '@/constants';
import { useCurrentUser } from '@/hooks';
import { useAuthStore } from '@/store';
import { colors, spacing, typography } from '@/theme';
import type { User } from '@/types';
import { getApiErrorMessage } from '@/utils';

function getFullName(user?: User | null) {
  return [user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'Your profile';
}

function formatLabel(value?: string | null) {
  if (!value) {
    return 'Not set';
  }

  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function VerificationBadges({ user }: { user?: User | null }) {
  return (
    <View style={styles.badges}>
      <Badge label="Email Verified" tone={user?.emailVerified ? 'success' : 'neutral'} />
      <Badge label="Student Verified" tone={user?.studentVerified ? 'success' : 'neutral'} />
      <Badge label="Alumni Verified" tone={user?.alumniVerified ? 'success' : 'neutral'} />
      <Badge label="Phone Verified" tone={user?.phoneVerified ? 'success' : 'neutral'} />
    </View>
  );
}

export default function ProfileScreen() {
  const userQuery = useCurrentUser();
  const storedUser = useAuthStore((state) => state.user);
  const refreshToken = useAuthStore((state) => state.refreshToken);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const user = userQuery.data ?? storedUser;
  const fullName = getFullName(user);

  async function handleLogout() {
    try {
      await logout(refreshToken);
    } finally {
      await clearAuth();
      router.replace(routes.auth.login);
    }
  }

  if (userQuery.isLoading && !user) {
    return (
      <Screen centered>
        <LoadingSpinner size="large" />
      </Screen>
    );
  }

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <Card style={styles.profileCard}>
        <Avatar imageUrl={user?.profilePhotoUrl} name={fullName} size={88} />
        <View style={styles.identity}>
          <Text style={styles.name}>{fullName}</Text>
          <Text style={styles.email}>{user?.email ?? 'Email unavailable'}</Text>
          <Text style={styles.meta}>{formatLabel(user?.userType)}</Text>
        </View>
        <VerificationBadges user={user} />
      </Card>

      {userQuery.error ? (
        <Text style={styles.error}>{getApiErrorMessage(userQuery.error, 'Unable to refresh profile.')}</Text>
      ) : null}

      <Card style={styles.section}>
        <Text style={styles.sectionTitle}>About</Text>
        <InfoRow label="School or community" value={user?.universityName ?? 'Not set'} />
        <InfoRow label="Phone" value={user?.phoneNumber ?? 'Not set'} />
        <InfoRow label="Gender" value={formatLabel(user?.gender)} />
        <Text style={styles.bio}>
          {user?.bio || 'Add a short bio so compatible roommates can get to know you.'}
        </Text>
      </Card>

      <Card style={styles.actions}>
        <Button title="Edit Profile" onPress={() => router.push('/profile/edit')} />
        <Button title="My Listings" variant="secondary" onPress={() => router.push('/listings/my-listings')} />
        <Button title="Verification" variant="secondary" onPress={() => router.push('/settings/verification')} />
        <Button title="Logout" variant="ghost" textStyle={styles.logoutText} onPress={handleLogout} />
      </Card>
    </Screen>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.infoRow}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    gap: spacing.md,
    paddingBottom: spacing.xl,
  },
  profileCard: {
    alignItems: 'center',
    gap: spacing.md,
  },
  identity: {
    alignItems: 'center',
    gap: spacing.xs,
  },
  name: {
    ...typography.title,
    color: colors.text,
    textAlign: 'center',
  },
  email: {
    ...typography.body,
    color: colors.secondaryText,
    textAlign: 'center',
  },
  meta: {
    ...typography.caption,
    color: colors.secondaryText,
    fontWeight: '600',
  },
  badges: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    justifyContent: 'center',
  },
  section: {
    gap: spacing.md,
  },
  sectionTitle: {
    ...typography.heading,
    color: colors.text,
  },
  infoRow: {
    flexDirection: 'row',
    gap: spacing.md,
    justifyContent: 'space-between',
  },
  infoLabel: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  infoValue: {
    ...typography.caption,
    color: colors.text,
    flex: 1,
    fontWeight: '600',
    textAlign: 'right',
  },
  bio: {
    ...typography.body,
    color: colors.secondaryText,
  },
  actions: {
    gap: spacing.sm,
  },
  logoutText: {
    color: colors.error,
  },
  error: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
});
