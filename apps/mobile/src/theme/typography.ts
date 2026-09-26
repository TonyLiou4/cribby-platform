import type { TextStyle } from 'react-native';

export const typography = {
  display: {
    fontSize: 34,
    lineHeight: 41,
    fontWeight: '700',
  },
  title: {
    fontSize: 28,
    lineHeight: 34,
    fontWeight: '700',
  },
  heading: {
    fontSize: 22,
    lineHeight: 28,
    fontWeight: '600',
  },
  body: {
    fontSize: 17,
    lineHeight: 24,
    fontWeight: '400',
  },
  caption: {
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '400',
  },
} satisfies Record<string, TextStyle>;

export type TypographyToken = keyof typeof typography;
