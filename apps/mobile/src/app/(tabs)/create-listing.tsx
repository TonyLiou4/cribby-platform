import { router, type Href } from 'expo-router';
import { useState } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import {
  ImagePickerButton,
  ListingForm,
  MAX_LISTING_IMAGES,
  UploadProgressIndicator,
  type ListingFormValues,
  toListingRequest,
} from '@/components/listing';
import { Card, Screen } from '@/components/ui';
import { useCreateListing, useUploadListingImages } from '@/hooks';
import { colors, radius, spacing, typography } from '@/theme';
import type { PickedListingImage } from '@/types';
import { getApiErrorMessage } from '@/utils';

export default function CreateListingScreen() {
  const createListing = useCreateListing();
  const uploadImages = useUploadListingImages();
  const [formError, setFormError] = useState<string | undefined>();
  const [selectedImages, setSelectedImages] = useState<PickedListingImage[]>([]);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [formKey, setFormKey] = useState(0);
  const isSubmitting = createListing.isPending || uploadImages.isPending;

  async function handleSubmit(values: ListingFormValues) {
    setFormError(undefined);

    try {
      const listing = await createListing.mutateAsync(toListingRequest(values));

      if (selectedImages.length > 0) {
        await uploadImages.mutateAsync({
          listingId: listing.id,
          images: selectedImages,
          onUploadProgress: setUploadProgress,
        });
      }

      setSelectedImages([]);
      setUploadProgress(0);
      setFormKey((currentKey) => currentKey + 1);
      router.replace(`/listings/${listing.id}` as Href);
    } catch (error) {
      setFormError(getApiErrorMessage(error, 'Unable to create this listing. Please try again.'));
    }
  }

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>New listing</Text>
        <Text style={styles.title}>Create a room listing</Text>
        <Text style={styles.subtitle}>
          Share the key details people need to decide whether your room and roommate fit are right.
        </Text>
      </View>

      <ListingForm
        error={formError}
        isLoading={isSubmitting}
        key={formKey}
        onSubmit={handleSubmit}
        showUniversityName={false}
        submitLabel="Publish listing"
      />

      <Card style={styles.imageCard}>
        <View style={styles.imageHeader}>
          <Text style={styles.imageTitle}>Photos</Text>
          <Text style={styles.imageSubtitle}>
            {selectedImages.length} of {MAX_LISTING_IMAGES} selected
          </Text>
        </View>

        {selectedImages.length > 0 ? (
          <View style={styles.previewGrid}>
            {selectedImages.map((image) => (
              <Image key={image.uri} source={{ uri: image.uri }} style={styles.previewImage} />
            ))}
          </View>
        ) : (
          <Text style={styles.imageHelp}>Add photos now, or manage them later from Edit Listing.</Text>
        )}

        {uploadImages.isPending ? <UploadProgressIndicator progress={uploadProgress} /> : null}

        <ImagePickerButton
          disabled={isSubmitting}
          remainingSlots={MAX_LISTING_IMAGES - selectedImages.length}
          onImagesPicked={(images) =>
            setSelectedImages((currentImages) =>
              [...currentImages, ...images].slice(0, MAX_LISTING_IMAGES),
            )
          }
        />
      </Card>
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
  imageCard: {
    gap: spacing.md,
  },
  imageHeader: {
    gap: spacing.xs,
  },
  imageTitle: {
    ...typography.heading,
    color: colors.text,
  },
  imageSubtitle: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  imageHelp: {
    ...typography.body,
    color: colors.secondaryText,
  },
  previewGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  previewImage: {
    backgroundColor: colors.border,
    borderRadius: radius.md,
    height: 74,
    width: 74,
  },
});
