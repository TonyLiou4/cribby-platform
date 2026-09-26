import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import {
  Modal,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { ListingImageCarousel } from '@/components/listing';
import { Badge, Button, Card, LoadingSpinner, Screen, TextField } from '@/components/ui';
import {
  useFavoriteStatus,
  useListingDetails,
  useListingImages,
  useStartConversation,
  useSubmitReport,
  useToggleFavorite,
} from '@/hooks';
import { colors, radius, shadows, spacing, typography } from '@/theme';
import type { Listing } from '@/types';
import { formatCurrency, formatDate, getApiErrorMessage, getConversationId } from '@/utils';

function formatEnumLabel(value?: string | null, fallback = 'Not specified') {
  if (!value) {
    return fallback;
  }

  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function getLocation(listing: Listing) {
  return [listing.neighborhood, listing.city, listing.universityName].filter(Boolean).join(' • ');
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.detailRow}>
      <Text style={styles.detailLabel}>{label}</Text>
      <Text style={styles.detailValue}>{value}</Text>
    </View>
  );
}

function getReportErrorMessage(error: unknown) {
  const message = getApiErrorMessage(error, 'Unable to submit this report. Please try again.');
  const lowerMessage = message.toLowerCase();

  if (lowerMessage.includes('duplicate') || lowerMessage.includes('already') || lowerMessage.includes('open report')) {
    return 'You already have an open report for this listing.';
  }

  return message;
}

export default function ListingDetailsScreen() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const listingId = Number(Array.isArray(id) ? id[0] : id);
  const hasValidId = Number.isFinite(listingId) && listingId > 0;
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [reportReason, setReportReason] = useState('');
  const [reportReasonError, setReportReasonError] = useState<string | undefined>();
  const [reportSuccessMessage, setReportSuccessMessage] = useState<string | undefined>();

  const listingQuery = useListingDetails(hasValidId ? listingId : undefined);
  const imagesQuery = useListingImages(hasValidId ? listingId : undefined);
  const favoriteQuery = useFavoriteStatus(hasValidId ? listingId : undefined);
  const toggleFavorite = useToggleFavorite(listingId);
  const startConversation = useStartConversation();
  const submitReport = useSubmitReport();

  const listing = listingQuery.data;
  const listingImages = imagesQuery.data ?? [];
  const imageUrls = listing?.imageUrls?.map((url, index) => ({ id: index, url })) ?? [];
  const coverImage = listing?.imageUrl ? [{ id: -1, url: listing.imageUrl }] : [];
  const images = listingImages.length > 0 ? listingImages : [...coverImage, ...imageUrls];

  const isRefreshing = listingQuery.isRefetching || imagesQuery.isRefetching || favoriteQuery.isRefetching;
  const isLoading = listingQuery.isLoading || imagesQuery.isLoading;
  const errorMessage = listingQuery.error
    ? getApiErrorMessage(listingQuery.error, 'Unable to load this listing.')
    : null;
  const favoriteError = toggleFavorite.error
    ? getApiErrorMessage(toggleFavorite.error, 'Unable to update favorite.')
    : null;
  const conversationError = startConversation.error
    ? getApiErrorMessage(startConversation.error, 'Unable to start conversation.')
    : null;
  const reportError = submitReport.error ? getReportErrorMessage(submitReport.error) : null;

  function refreshDetails() {
    void listingQuery.refetch();
    void imagesQuery.refetch();
    void favoriteQuery.refetch();
  }

  function handleFavoritePress() {
    if (!hasValidId) {
      return;
    }

    toggleFavorite.mutate(Boolean(favoriteQuery.data));
  }

  async function handleContactPress() {
    if (!hasValidId) {
      return;
    }

    try {
      const conversation = await startConversation.mutateAsync({ listingId });
      const conversationId = getConversationId(conversation);

      if (__DEV__) {
        // TODO: Safe to remove after conversation routing smoke tests pass.
        console.log('Contact conversation id:', conversationId);
      }

      if (!conversationId) {
        throw new Error('Conversation response did not include conversationId.');
      }

      router.push(`/conversations/${conversationId}`);
    } catch {
      // React Query exposes the error to the screen.
    }
  }

  function openReportModal() {
    setReportSuccessMessage(undefined);
    setReportReasonError(undefined);
    submitReport.reset();
    setIsReportModalOpen(true);
  }

  function closeReportModal() {
    if (submitReport.isPending) {
      return;
    }

    setIsReportModalOpen(false);
    setReportReason('');
    setReportReasonError(undefined);
    submitReport.reset();
  }

  async function handleSubmitReport() {
    const reason = reportReason.trim();

    setReportReasonError(undefined);
    setReportSuccessMessage(undefined);
    submitReport.reset();

    if (!reason) {
      setReportReasonError('Reason is required.');
      return;
    }

    if (reason.length < 10) {
      setReportReasonError('Reason must be at least 10 characters.');
      return;
    }

    try {
      await submitReport.mutateAsync({
        reportType: 'LISTING',
        reportedListingId: listingId,
        reason,
      });
      setIsReportModalOpen(false);
      setReportReason('');
      setReportSuccessMessage('Thanks. Your report has been submitted.');
    } catch {
      // React Query exposes the backend error to the modal.
    }
  }

  if (!hasValidId) {
    return (
      <Screen centered>
        <Card style={styles.stateCard}>
          <Text style={styles.stateTitle}>Listing not found.</Text>
          <Text style={styles.stateMessage}>This listing link is invalid.</Text>
          <Button title="Go back" variant="secondary" onPress={() => router.back()} />
        </Card>
      </Screen>
    );
  }

  if (isLoading && !listing) {
    return (
      <Screen centered>
        <LoadingSpinner size="large" />
      </Screen>
    );
  }

  if (errorMessage || !listing) {
    return (
      <Screen centered>
        <Card style={styles.stateCard}>
          <Text style={styles.stateTitle}>Listing could not load.</Text>
          <Text style={styles.stateMessage}>{errorMessage ?? 'Please try again.'}</Text>
          <Button title="Try again" variant="secondary" onPress={refreshDetails} />
        </Card>
      </Screen>
    );
  }

  return (
    <Screen>
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} tintColor={colors.primary} onRefresh={refreshDetails} />
        }
        showsVerticalScrollIndicator={false}
      >
        <ListingImageCarousel images={images} />

        <View style={styles.header}>
          <View style={styles.titleRow}>
            <Text style={styles.title}>{listing.title}</Text>
            <Badge label={listing.status} tone={listing.status === 'ACTIVE' ? 'success' : 'neutral'} />
          </View>
          <Text style={styles.rent}>{formatCurrency(listing.monthlyRent)}/mo</Text>
          <Text style={styles.location}>{getLocation(listing) || 'Location available soon'}</Text>
        </View>

        <Card style={styles.detailsCard}>
          <DetailRow label="Room type" value={formatEnumLabel(listing.roomType, 'Room')} />
          <DetailRow label="Preference" value={formatEnumLabel(listing.genderPreference)} />
          <DetailRow
            label="Available"
            value={listing.availableFrom ? formatDate(listing.availableFrom) : 'Available soon'}
          />
          <DetailRow
            label="Utilities"
            value={listing.utilitiesIncluded ? 'Included' : 'Not included or not specified'}
          />
        </Card>

        <Card style={styles.section}>
          <Text style={styles.sectionTitle}>Description</Text>
          <Text style={styles.description}>{listing.description || 'No description provided yet.'}</Text>
        </Card>

        {favoriteError ? <Text style={styles.error}>{favoriteError}</Text> : null}
        {conversationError ? <Text style={styles.error}>{conversationError}</Text> : null}
        {reportSuccessMessage ? <Text style={styles.success}>{reportSuccessMessage}</Text> : null}

        <View style={styles.actions}>
          <Button
            disabled={favoriteQuery.isLoading || toggleFavorite.isPending}
            loading={toggleFavorite.isPending}
            onPress={handleFavoritePress}
            title={favoriteQuery.data ? 'Saved' : 'Save listing'}
            variant={favoriteQuery.data ? 'secondary' : 'primary'}
          />
          <Button
            title="Contact"
            variant="secondary"
            loading={startConversation.isPending}
            disabled={startConversation.isPending}
            onPress={() => {
              void handleContactPress();
            }}
          />
          <Button
            title="Report listing"
            variant="ghost"
            onPress={openReportModal}
          />
        </View>
      </ScrollView>

      <ReportListingModal
        error={reportError}
        isSubmitting={submitReport.isPending}
        isVisible={isReportModalOpen}
        reason={reportReason}
        reasonError={reportReasonError}
        onCancel={closeReportModal}
        onChangeReason={(value) => {
          setReportReason(value);
          if (reportReasonError) {
            setReportReasonError(undefined);
          }
          if (submitReport.error) {
            submitReport.reset();
          }
        }}
        onSubmit={() => {
          void handleSubmitReport();
        }}
      />
    </Screen>
  );
}

function ReportListingModal({
  error,
  isSubmitting,
  isVisible,
  reason,
  reasonError,
  onCancel,
  onChangeReason,
  onSubmit,
}: {
  error?: string | null;
  isSubmitting: boolean;
  isVisible: boolean;
  reason: string;
  reasonError?: string;
  onCancel: () => void;
  onChangeReason: (value: string) => void;
  onSubmit: () => void;
}) {
  const trimmedReason = reason.trim();

  return (
    <Modal animationType="slide" transparent visible={isVisible} onRequestClose={onCancel}>
      <View style={styles.modalBackdrop}>
        <Pressable disabled={isSubmitting} style={styles.modalScrim} onPress={onCancel} />
        <View style={styles.sheet}>
          <View style={styles.sheetHandle} />
          <View style={styles.sheetCopy}>
            <Text style={styles.sheetTitle}>Report this listing</Text>
            <Text style={styles.sheetDescription}>
              Tell us what looks suspicious or inappropriate. Reports help keep the community safe.
            </Text>
          </View>

          <TextField
            error={reasonError}
            label="Reason"
            multiline
            onChangeText={onChangeReason}
            placeholder="Describe the issue"
            style={styles.reportInput}
            textAlignVertical="top"
            value={reason}
          />

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <View style={styles.sheetActions}>
            <Button
              disabled={trimmedReason.length < 10 || isSubmitting}
              loading={isSubmitting}
              title="Submit report"
              onPress={onSubmit}
            />
            <Button disabled={isSubmitting} title="Cancel" variant="secondary" onPress={onCancel} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: spacing.lg,
    paddingBottom: spacing.xl,
  },
  header: {
    gap: spacing.sm,
  },
  titleRow: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: spacing.sm,
    justifyContent: 'space-between',
  },
  title: {
    ...typography.title,
    color: colors.text,
    flex: 1,
  },
  rent: {
    ...typography.heading,
    color: colors.text,
    fontWeight: '700',
  },
  location: {
    ...typography.body,
    color: colors.secondaryText,
  },
  detailsCard: {
    gap: spacing.md,
  },
  detailRow: {
    flexDirection: 'row',
    gap: spacing.md,
    justifyContent: 'space-between',
  },
  detailLabel: {
    ...typography.caption,
    color: colors.secondaryText,
    flex: 1,
  },
  detailValue: {
    ...typography.caption,
    color: colors.text,
    flex: 1,
    fontWeight: '600',
    textAlign: 'right',
  },
  section: {
    gap: spacing.sm,
  },
  sectionTitle: {
    ...typography.heading,
    color: colors.text,
  },
  description: {
    ...typography.body,
    color: colors.secondaryText,
  },
  error: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
  success: {
    ...typography.caption,
    color: colors.success,
    textAlign: 'center',
  },
  actions: {
    gap: spacing.sm,
  },
  modalBackdrop: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  modalScrim: {
    backgroundColor: colors.overlay,
    bottom: 0,
    left: 0,
    position: 'absolute',
    right: 0,
    top: 0,
  },
  sheet: {
    ...shadows.md,
    backgroundColor: colors.surface,
    borderTopLeftRadius: radius.xl,
    borderTopRightRadius: radius.xl,
    gap: spacing.md,
    padding: spacing.lg,
    paddingBottom: spacing.xl,
  },
  sheetHandle: {
    alignSelf: 'center',
    backgroundColor: colors.border,
    borderRadius: radius.pill,
    height: 5,
    width: 44,
  },
  sheetCopy: {
    gap: spacing.sm,
  },
  sheetTitle: {
    ...typography.heading,
    color: colors.text,
    textAlign: 'center',
  },
  sheetDescription: {
    ...typography.body,
    color: colors.secondaryText,
    textAlign: 'center',
  },
  reportInput: {
    minHeight: 132,
    paddingTop: spacing.md,
  },
  sheetActions: {
    gap: spacing.sm,
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
