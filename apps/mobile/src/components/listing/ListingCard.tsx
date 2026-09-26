import { router, type Href } from 'expo-router';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { Badge, Card } from '@/components/ui';
import { colors, radius, spacing, typography } from '@/theme';
import type { Listing } from '@/types';
import { formatCurrency, formatDate } from '@/utils';

type ListingCardProps = {
  listing: Listing;
};

function formatRoomType(roomType?: string | null) {
  if (!roomType) {
    return 'Room';
  }

  return roomType
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function getListingLocation(listing: Listing) {
  return [listing.neighborhood, listing.city].filter(Boolean).join(', ');
}

export function ListingCard({ listing }: ListingCardProps) {
  const imageUrl = listing.imageUrl ?? listing.imageUrls?.[0];
  const location = getListingLocation(listing);

  function handlePress() {
    router.push(`/listings/${listing.id}` as Href);
  }

  return (
    <Pressable accessibilityRole="button" onPress={handlePress}>
      {({ pressed }) => (
        <Card style={[styles.card, pressed && styles.pressed]}>
          <View style={styles.imagePlaceholder}>
            {imageUrl ? (
              <Image source={{ uri: imageUrl }} style={styles.image} />
            ) : (
              <Text style={styles.imageText}>No photo</Text>
            )}
          </View>

          <View style={styles.content}>
            <View style={styles.header}>
              <Text numberOfLines={2} style={styles.title}>
                {listing.title}
              </Text>
              <Badge label={listing.status} tone={listing.status === 'ACTIVE' ? 'success' : 'neutral'} />
            </View>

            <Text style={styles.rent}>{formatCurrency(listing.monthlyRent)}/mo</Text>

            <View style={styles.meta}>
              <Text numberOfLines={1} style={styles.metaText}>
                {location || listing.universityName || 'Location available soon'}
              </Text>
              <Text style={styles.metaText}>{formatRoomType(listing.roomType)}</Text>
              <Text style={styles.metaText}>
                {listing.availableFrom ? `Available ${formatDate(listing.availableFrom)}` : 'Available soon'}
              </Text>
            </View>
          </View>
        </Card>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    gap: spacing.md,
  },
  pressed: {
    opacity: 0.86,
  },
  imagePlaceholder: {
    alignItems: 'center',
    aspectRatio: 16 / 9,
    backgroundColor: colors.background,
    borderRadius: radius.md,
    justifyContent: 'center',
    overflow: 'hidden',
  },
  image: {
    height: '100%',
    width: '100%',
  },
  imageText: {
    ...typography.caption,
    color: colors.secondaryText,
    fontWeight: '600',
  },
  content: {
    gap: spacing.sm,
  },
  header: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: spacing.sm,
    justifyContent: 'space-between',
  },
  title: {
    ...typography.heading,
    color: colors.text,
    flex: 1,
  },
  rent: {
    ...typography.body,
    color: colors.text,
    fontWeight: '700',
  },
  meta: {
    gap: spacing.xs,
  },
  metaText: {
    ...typography.caption,
    color: colors.secondaryText,
  },
});
