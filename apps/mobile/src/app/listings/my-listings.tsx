import { router, type Href } from 'expo-router';
import { useState } from 'react';
import { Alert, FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';

import { Badge, Button, Card, LoadingSpinner, Screen } from '@/components/ui';
import { useDeleteListing, useMyListings, useUpdateListingStatus } from '@/hooks';
import { colors, radius, spacing, typography } from '@/theme';
import type { Listing, ManageableListingStatus } from '@/types';
import { formatCurrency, getApiErrorMessage } from '@/utils';

const statusFilters: ('ALL' | ManageableListingStatus)[] = ['ALL', 'AVAILABLE', 'PENDING', 'OCCUPIED'];
const listingStatuses: ManageableListingStatus[] = ['AVAILABLE', 'PENDING', 'OCCUPIED'];

function getStatusTone(status: Listing['status']) {
  if (status === 'AVAILABLE' || status === 'ACTIVE') {
    return 'success';
  }

  if (status === 'PENDING') {
    return 'warning';
  }

  return 'neutral';
}

function formatStatus(status: string) {
  return status
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function ManageListingCard({
  listing,
  onDelete,
  onStatusChange,
}: {
  listing: Listing;
  onDelete: (listing: Listing) => void;
  onStatusChange: (id: number, status: ManageableListingStatus) => void;
}) {
  const location = [listing.neighborhood, listing.city].filter(Boolean).join(', ');

  return (
    <Card style={styles.listingCard}>
      <Pressable onPress={() => router.push(`/listings/${listing.id}` as Href)} style={styles.cardHeader}>
        <View style={styles.cardTitleGroup}>
          <Text numberOfLines={2} style={styles.cardTitle}>
            {listing.title}
          </Text>
          <Text style={styles.cardMeta}>
            {formatCurrency(listing.monthlyRent)}/mo · {location || 'Location available soon'}
          </Text>
        </View>
        <Badge label={formatStatus(listing.status)} tone={getStatusTone(listing.status)} />
      </Pressable>

      <View style={styles.actionRow}>
        <Button
          title="Edit"
          variant="secondary"
          style={styles.smallButton}
          textStyle={styles.smallButtonText}
          onPress={() => router.push(`/listings/edit/${listing.id}` as Href)}
        />
        <Button
          title="Delete"
          variant="ghost"
          style={styles.smallButton}
          textStyle={styles.deleteButtonText}
          onPress={() => onDelete(listing)}
        />
      </View>

      <View style={styles.statusActions}>
        {listingStatuses.map((status) => (
          <Button
            key={status}
            title={formatStatus(status)}
            variant={listing.status === status ? 'primary' : 'secondary'}
            style={styles.statusButton}
            textStyle={styles.smallButtonText}
            onPress={() => onStatusChange(listing.id, status)}
          />
        ))}
      </View>
    </Card>
  );
}

export default function MyListingsScreen() {
  const [activeStatus, setActiveStatus] = useState<(typeof statusFilters)[number]>('ALL');
  const myListings = useMyListings();
  const updateStatus = useUpdateListingStatus();
  const deleteListing = useDeleteListing();
  const listings = myListings.data ?? [];
  const filteredListings =
    activeStatus === 'ALL' ? listings : listings.filter((listing) => listing.status === activeStatus);
  const errorMessage = myListings.error
    ? getApiErrorMessage(myListings.error, 'Unable to load your listings.')
    : undefined;
  const actionError = updateStatus.error ?? deleteListing.error;

  function handleStatusChange(id: number, status: ManageableListingStatus) {
    updateStatus.mutate({ id, status });
  }

  function handleDelete(listing: Listing) {
    Alert.alert('Delete listing?', `This will remove "${listing.title}" from Roommate Marketplace.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => deleteListing.mutate(listing.id),
      },
    ]);
  }

  return (
    <Screen>
      <FlatList
        ListEmptyComponent={
          myListings.isLoading ? (
            <LoadingSpinner size="large" />
          ) : errorMessage ? (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>Listings could not load.</Text>
              <Text style={styles.stateMessage}>{errorMessage}</Text>
              <Button title="Try again" variant="secondary" onPress={() => myListings.refetch()} />
            </View>
          ) : (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>You haven&apos;t posted any rooms yet.</Text>
              <Text style={styles.stateMessage}>Create your first listing and start reaching roommates.</Text>
              <Button title="Post a room" onPress={() => router.push('/(tabs)/create-listing')} />
            </View>
          )
        }
        ListHeaderComponent={
          <View style={styles.header}>
            <View style={styles.headerCopy}>
              <Text style={styles.eyebrow}>Manage</Text>
              <Text style={styles.title}>My Listings</Text>
              <Text style={styles.subtitle}>Update availability, edit details, or remove rooms you posted.</Text>
            </View>
            <Button title="Post a room" onPress={() => router.push('/(tabs)/create-listing')} />
            <View style={styles.filterRow}>
              {statusFilters.map((status) => {
                const isSelected = status === activeStatus;

                return (
                  <Pressable
                    key={status}
                    onPress={() => setActiveStatus(status)}
                    style={[styles.filterChip, isSelected && styles.filterChipSelected]}
                  >
                    <Text style={[styles.filterText, isSelected && styles.filterTextSelected]}>
                      {status === 'ALL' ? 'All' : formatStatus(status)}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
            {actionError ? (
              <Text style={styles.error}>{getApiErrorMessage(actionError, 'Listing action failed.')}</Text>
            ) : null}
          </View>
        }
        contentContainerStyle={styles.listContent}
        data={filteredListings}
        keyExtractor={(item) => String(item.id)}
        refreshControl={
          <RefreshControl
            refreshing={myListings.isRefetching}
            tintColor={colors.primary}
            onRefresh={myListings.refetch}
          />
        }
        renderItem={({ item }) => (
          <ManageListingCard
            listing={item}
            onDelete={handleDelete}
            onStatusChange={handleStatusChange}
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
  headerCopy: {
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
  filterRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  filterChip: {
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  filterChipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  filterText: {
    ...typography.caption,
    color: colors.text,
    fontWeight: '600',
  },
  filterTextSelected: {
    color: colors.surface,
  },
  listingCard: {
    gap: spacing.md,
  },
  cardHeader: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: spacing.sm,
    justifyContent: 'space-between',
  },
  cardTitleGroup: {
    flex: 1,
    gap: spacing.xs,
  },
  cardTitle: {
    ...typography.heading,
    color: colors.text,
  },
  cardMeta: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  actionRow: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  statusActions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  smallButton: {
    flex: 1,
    minHeight: 44,
    paddingHorizontal: spacing.md,
  },
  statusButton: {
    minHeight: 40,
    paddingHorizontal: spacing.md,
  },
  smallButtonText: {
    ...typography.caption,
    fontWeight: '700',
  },
  deleteButtonText: {
    ...typography.caption,
    color: colors.error,
    fontWeight: '700',
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
