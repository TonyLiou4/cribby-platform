import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

import { AppProviders } from '@/providers';
import { colors, typography } from '@/theme';

const stackHeaderOptions = {
  headerShown: true,
  headerBackTitle: 'Back',
  headerShadowVisible: false,
  headerStyle: {
    backgroundColor: colors.background,
  },
  headerTitleAlign: 'center' as const,
  headerTitleStyle: {
    ...typography.body,
    color: colors.text,
    fontWeight: '700' as const,
  },
  headerTintColor: colors.primary,
};

export default function RootLayout() {
  return (
    <AppProviders>
      <StatusBar style="dark" />
      <Stack
        screenOptions={{
          animation: 'slide_from_right',
          contentStyle: { backgroundColor: colors.background },
          headerShown: false,
        }}
      >
        <Stack.Screen name="index" options={{ headerShown: false }} />
        <Stack.Screen name="(auth)" options={{ headerShown: false }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen
          name="listings/[id]"
          options={{
            ...stackHeaderOptions,
            title: 'Listing',
          }}
        />
        <Stack.Screen
          name="listings/my-listings"
          options={{
            ...stackHeaderOptions,
            headerBackTitle: 'Profile',
            title: 'My Listings',
          }}
        />
        <Stack.Screen
          name="listings/edit/[id]"
          options={{
            ...stackHeaderOptions,
            title: 'Edit Listing',
          }}
        />
        <Stack.Screen
          name="conversations/[id]"
          options={{
            ...stackHeaderOptions,
            headerBackTitle: 'Messages',
            title: 'Conversation',
          }}
        />
        <Stack.Screen
          name="profile/edit"
          options={{
            ...stackHeaderOptions,
            headerBackTitle: 'Profile',
            title: 'Edit Profile',
          }}
        />
        <Stack.Screen
          name="settings/verification"
          options={{
            ...stackHeaderOptions,
            headerBackTitle: 'Profile',
            title: 'Verification',
          }}
        />
      </Stack>
    </AppProviders>
  );
}
