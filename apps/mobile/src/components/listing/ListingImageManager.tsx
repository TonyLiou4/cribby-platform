import { useState } from 'react';
import { Alert, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { Card, LoadingSpinner } from '@/components/ui';
import {
  useDeleteListingImage,
  useListingImages,
  useReorderListingImages,
  useUploadListingImage,
} from '@/hooks';
import { colors, radius, spacing, typography } from '@/theme';
import type { ListingImage, PickedListingImage } from '@/types';
import { getApiErrorMessage } from '@/utils';

import { ImagePickerButton, MAX_LISTING_IMAGES } from './ImagePickerButton';
import { UploadProgressIndicator } from './UploadProgressIndicator';

type ListingImageManagerProps = {
  listingId: number;
};

function getImageId(image: ListingImage) {
  return typeof image.id === 'number' ? image.id : undefined;
}

export function ListingImageManager({ listingId }: ListingImageManagerProps) {
  const imagesQuery = useListingImages(listingId);
  const uploadImage = useUploadListingImage(listingId);
  const deleteImage = useDeleteListingImage(listingId);
  const reorderImages = useReorderListingImages(listingId);
  const images = imagesQuery.data ?? [];
  const [uploadProgress, setUploadProgress] = useState(0);
  const remainingSlots = Math.max(MAX_LISTING_IMAGES - images.length, 0);
  const error = imagesQuery.error ?? uploadImage.error ?? deleteImage.error ?? reorderImages.error;

  async function uploadImages(pickedImages: PickedListingImage[]) {
    try {
      for (const image of pickedImages) {
        setUploadProgress(0);
        await uploadImage.mutateAsync({
          image,
          onUploadProgress: setUploadProgress,
        });
      }
    } finally {
      setUploadProgress(0);
    }
  }

  function confirmDelete(image: ListingImage) {
    const imageId = getImageId(image);

    if (imageId == null) {
      Alert.alert('Image cannot be removed', 'This image does not include an image id.');
      return;
    }

    Alert.alert('Remove photo?', 'This photo will be removed from your listing.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Remove',
        style: 'destructive',
        onPress: () => deleteImage.mutate(imageId),
      },
    ]);
  }

  function moveImage(fromIndex: number, direction: -1 | 1) {
    const toIndex = fromIndex + direction;

    if (toIndex < 0 || toIndex >= images.length) {
      return;
    }

    const nextImages = [...images];
    const [movedImage] = nextImages.splice(fromIndex, 1);
    nextImages.splice(toIndex, 0, movedImage);
    const imageIds = nextImages.map(getImageId).filter((id): id is number => id != null);

    if (imageIds.length !== nextImages.length) {
      Alert.alert('Cannot reorder yet', 'Every image needs an id before ordering can be saved.');
      return;
    }

    reorderImages.mutate(imageIds);
  }

  const isBusy = uploadImage.isPending || deleteImage.isPending || reorderImages.isPending;

  return (
    <Card style={styles.card}>
      <View style={styles.header}>
        <View style={styles.copy}>
          <Text style={styles.title}>Listing photos</Text>
          <Text style={styles.subtitle}>{images.length} of {MAX_LISTING_IMAGES} photos added</Text>
        </View>
        {imagesQuery.isLoading ? <LoadingSpinner /> : null}
      </View>

      {error ? <Text style={styles.error}>{getApiErrorMessage(error, 'Image action failed.')}</Text> : null}
      {uploadImage.isPending ? <UploadProgressIndicator progress={uploadProgress} /> : null}

      <View style={styles.gallery}>
        {images.length === 0 ? (
          <View style={styles.emptyImage}>
            <Text style={styles.emptyText}>Add real photos to help your listing stand out.</Text>
          </View>
        ) : (
          images.map((image, index) => (
            <View key={`${image.id ?? image.url}`} style={styles.imageItem}>
              <Image source={{ uri: image.url }} style={styles.image} />
              <View style={styles.imageActions}>
                <Pressable style={styles.iconButton} onPress={() => moveImage(index, -1)}>
                  <Text style={styles.iconButtonText}>‹</Text>
                </Pressable>
                <Pressable style={styles.iconButton} onPress={() => moveImage(index, 1)}>
                  <Text style={styles.iconButtonText}>›</Text>
                </Pressable>
                <Pressable style={styles.removeButton} onPress={() => confirmDelete(image)}>
                  <Text style={styles.removeButtonText}>Remove</Text>
                </Pressable>
              </View>
            </View>
          ))
        )}
      </View>

      <ImagePickerButton
        disabled={isBusy}
        remainingSlots={remainingSlots}
        onImagesPicked={(pickedImages) => {
          void uploadImages(pickedImages);
        }}
      />
    </Card>
  );
}

const styles = StyleSheet.create({
  card: {
    gap: spacing.md,
  },
  header: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  copy: {
    gap: spacing.xs,
  },
  title: {
    ...typography.heading,
    color: colors.text,
  },
  subtitle: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  gallery: {
    gap: spacing.md,
  },
  emptyImage: {
    alignItems: 'center',
    aspectRatio: 16 / 9,
    backgroundColor: colors.background,
    borderColor: colors.border,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    justifyContent: 'center',
    padding: spacing.lg,
  },
  emptyText: {
    ...typography.body,
    color: colors.secondaryText,
    textAlign: 'center',
  },
  imageItem: {
    gap: spacing.sm,
  },
  image: {
    aspectRatio: 16 / 9,
    backgroundColor: colors.border,
    borderRadius: radius.lg,
    width: '100%',
  },
  imageActions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  iconButton: {
    alignItems: 'center',
    backgroundColor: colors.background,
    borderRadius: radius.pill,
    height: 36,
    justifyContent: 'center',
    width: 44,
  },
  iconButtonText: {
    ...typography.heading,
    color: colors.text,
  },
  removeButton: {
    alignItems: 'center',
    backgroundColor: colors.background,
    borderRadius: radius.pill,
    flex: 1,
    justifyContent: 'center',
  },
  removeButtonText: {
    ...typography.caption,
    color: colors.error,
    fontWeight: '700',
  },
  error: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
  },
});
