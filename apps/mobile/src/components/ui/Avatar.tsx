import { Image, StyleSheet, Text, View } from 'react-native';

import { colors, radius, typography } from '@/theme';

export type AvatarProps = {
  name: string;
  imageUrl?: string | null;
  size?: number;
};

export function Avatar({ name, imageUrl, size = 44 }: AvatarProps) {
  const initials = name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');

  const dynamicStyle = {
    width: size,
    height: size,
    borderRadius: size / 2,
  };

  if (imageUrl) {
    return <Image source={{ uri: imageUrl }} style={[styles.avatar, dynamicStyle]} />;
  }

  return (
    <View style={[styles.avatar, styles.fallback, dynamicStyle]}>
      <Text style={styles.initials}>{initials || '?'}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  avatar: {
    overflow: 'hidden',
  },
  fallback: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.border,
    borderRadius: radius.pill,
  },
  initials: {
    ...typography.caption,
    color: colors.text,
    fontWeight: '700',
  },
});
