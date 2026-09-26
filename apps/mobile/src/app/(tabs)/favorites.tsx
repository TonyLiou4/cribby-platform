import { FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';

import { ListingCard } from '@/components/listing';
import { Button, LoadingSpinner, Screen } from '@/components/ui';
import { useFavorites, useRemoveFavorite } from '@/hooks';
import { colors, spacing, typography } from '@/theme';
import type { SavedListing } from '@/types';
import { getApiErrorMessage } from '@/utils';

function SavedListingItem({
  listing,
  onRemove,
  isRemoving,
}: {
  listing: SavedListing;
  onRemove: (listingId: number) => void;
  isRemoving: boolean;
}) {
  return (
    <View style={styles.savedItem}>
      <ListingCard listing={listing} />
      <Button
        disabled={isRemoving}
        loading={isRemoving}
        title="Remove from Saved"
        variant="secondary"
        onPress={() => onRemove(listing.listingId)}
      />
    </View>
  );
}

export default function FavoritesScreen() {
  const favoritesQuery = useFavorites();
  const removeFavorite = useRemoveFavorite();
  const listings = favoritesQuery.data ?? [];
  const errorMessage = favoritesQuery.error
    ? getApiErrorMessage(favoritesQuery.error, 'Unable to load saved rooms.')
    : undefined;
  const removeError = removeFavorite.error
    ? getApiErrorMessage(removeFavorite.error, 'Unable to remove this saved room.')
    : undefined;

  return (
    <Screen>
      <FlatList
        ListEmptyComponent={
          favoritesQuery.isLoading ? (
            <LoadingSpinner size="large" />
          ) : errorMessage ? (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>Saved rooms could not load.</Text>
              <Text style={styles.stateMessage}>{errorMessage}</Text>
              <Button title="Try again" variant="secondary" onPress={() => favoritesQuery.refetch()} />
            </View>
          ) : (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>No saved rooms yet.</Text>
              <Text style={styles.stateMessage}>
                Tap the heart on listings you like to save them here.
              </Text>
            </View>
          )
        }
        ListHeaderComponent={
          <View style={styles.header}>
            <View style={styles.copy}>
              <Text style={styles.eyebrow}>Saved</Text>
              <Text style={styles.title}>Rooms you like</Text>
              <Text style={styles.subtitle}>Keep track of listings that feel like a good fit.</Text>
            </View>
            {removeError ? <Text style={styles.error}>{removeError}</Text> : null}
          </View>
        }
        contentContainerStyle={styles.listContent}
        data={listings}
        keyExtractor={(item) => item.listingId.toString()}
        refreshControl={
          <RefreshControl
            refreshing={favoritesQuery.isRefetching}
            tintColor={colors.primary}
            onRefresh={favoritesQuery.refetch}
          />
        }
        renderItem={({ item }) => (
          <SavedListingItem
            isRemoving={removeFavorite.isPending && removeFavorite.variables === item.listingId}
            listing={item}
            onRemove={(listingId) => removeFavorite.mutate(listingId)}
          />
        )}
        showsVerticalScrollIndicator={false}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  listContent: {
    gap: spacing.md,
    paddingBottom: spacing.xl,
  },
  header: {
    gap: spacing.md,
    paddingBottom: spacing.sm,
  },
  copy: {
    gap: spacing.xs,
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
  savedItem: {
    gap: spacing.sm,
  },
  stateCard: {
    alignItems: 'center',
    gap: spacing.sm,
    paddingVertical: spacing.xl,
  },
  stateTitle: {
    ...typography.heading,
    color: colors.text,
    textAlign: 'center',
  },
  stateMessage: {
    ...typography.body,
    color: colors.secondaryText,
    textAlign: 'center',
  },
  error: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
});
