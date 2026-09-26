import type { Href } from 'expo-router';

export const routes = {
  auth: {
    login: '/login' as Href,
    register: '/register' as Href,
    verifyEmail: '/verify-email' as Href,
  },
  authenticated: '/(tabs)/home' as Href,
} as const;
