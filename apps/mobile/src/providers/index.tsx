import type { PropsWithChildren } from 'react';
import { useEffect } from 'react';

import { useAuthStore } from '@/store';

import { QueryProvider } from './QueryProvider';
import { ThemeProvider } from './ThemeProvider';

function AuthBootstrap({ children }: PropsWithChildren) {
  const hydrateAuth = useAuthStore((state) => state.hydrateAuth);

  useEffect(() => {
    void hydrateAuth();
  }, [hydrateAuth]);

  return children;
}

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ThemeProvider>
      <QueryProvider>
        <AuthBootstrap>{children}</AuthBootstrap>
      </QueryProvider>
    </ThemeProvider>
  );
}

export * from './QueryProvider';
export * from './ThemeProvider';
