import { router, useLocalSearchParams, type Href } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import {
  ListingForm,
  ListingImageManager,
  type ListingFormValues,
  defaultListingFormValues,
  toListingRequest,
} from '@/components/listing';
import { Button, Card, LoadingSpinner, Screen } from '@/components/ui';
import { useListingDetails, useUpdateListing } from '@/hooks';
import { colors, spacing, typography } from '@/theme';
import type { Listing } from '@/types';
import { getApiErrorMessage } from '@/utils';

function toFormValues(listing: Listing): ListingFormValues {
  return {
    title: listing.title ?? defaultListingFormValues.title,
    description: listing.description ?? defaultListingFormValues.description,
    city: listing.city ?? defaultListingFormValues.city,
    neighborhood: listing.neighborhood ?? defaultListingFormValues.neighborhood,
    universityName: listing.universityName ?? defaultListingFormValues.universityName,
    monthlyRent: String(listing.monthlyRent ?? ''),
    utilitiesIncluded: Boolean(listing.utilitiesIncluded),
    availableFrom: listing.availableFrom ?? defaultListingFormValues.availableFrom,
    roomType: listing.roomType ?? defaultListingFormValues.roomType,
    genderPreference: listing.genderPreference ?? defaultListingFormValues.genderPreference,
    exactAddress: listing.exactAddress ?? defaultListingFormValues.exactAddress,
  };
}

export default function EditListingScreen() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const listingId = Number(Array.isArray(id) ? id[0] : id);
  const hasValidId = Number.isFinite(listingId) && listingId > 0;
  const listingQuery = useListingDetails(hasValidId ? listingId : undefined);
  const updateListing = useUpdateListing(listingId);
  const [formError, setFormError] = useState<string | undefined>();

  async function handleSubmit(values: ListingFormValues) {
    setFormError(undefined);

    try {
      const listing = await updateListing.mutateAsync(toListingRequest(values));
      router.replace(`/listings/${listing.id}` as Href);
    } catch (error) {
      setFormError(getApiErrorMessage(error, 'Unable to update this listing. Please try again.'));
    }
  }

  if (!hasValidId) {
    return (
      <Screen centered>
        <Card style={styles.stateCard}>
          <Text style={styles.stateTitle}>Listing not found.</Text>
          <Button title="Go back" variant="secondary" onPress={() => router.back()} />
        </Card>
      </Screen>
    );
  }

  if (listingQuery.isLoading || !listingQuery.data) {
    return (
      <Screen centered>
        <LoadingSpinner size="large" />
      </Screen>
    );
  }

  if (listingQuery.error) {
    return (
      <Screen centered>
        <Card style={styles.stateCard}>
          <Text style={styles.stateTitle}>Listing could not load.</Text>
          <Text style={styles.stateMessage}>
            {getApiErrorMessage(listingQuery.error, 'Unable to load this listing.')}
          </Text>
          <Button title="Try again" variant="secondary" onPress={() => listingQuery.refetch()} />
        </Card>
      </Screen>
    );
  }

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>Manage listing</Text>
        <Text style={styles.title}>Edit room details</Text>
        <Text style={styles.subtitle}>Update the visible listing details and save your changes.</Text>
      </View>

      <ListingForm
        defaultValues={toFormValues(listingQuery.data)}
        error={formError}
        isLoading={updateListing.isPending}
        onSubmit={handleSubmit}
        submitLabel="Save changes"
        successMessage={updateListing.isSuccess ? 'Listing updated.' : undefined}
      />

      <ListingImageManager listingId={listingId} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  screen: {
    gap: spacing.lg,
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
  stateCard: {
    gap: spacing.md,
    maxWidth: 380,
    width: '100%',
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
});
