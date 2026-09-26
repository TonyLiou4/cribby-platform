import * as ImagePicker from 'expo-image-picker';
import { Alert, StyleSheet, View } from 'react-native';

import { Button } from '@/components/ui';
import { spacing } from '@/theme';
import type { PickedListingImage } from '@/types';

const SUPPORTED_IMAGE_TYPES = new Set([
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/webp',
  'image/heic',
  'image/heif',
]);

export const MAX_LISTING_IMAGES = 10;
export const MAX_LISTING_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

type ImagePickerButtonProps = {
  disabled?: boolean;
  remainingSlots: number;
  onImagesPicked: (images: PickedListingImage[]) => void;
};

function normalizeAsset(asset: ImagePicker.ImagePickerAsset): PickedListingImage | null {
  const mimeType = asset.mimeType ?? 'image/jpeg';

  if (!SUPPORTED_IMAGE_TYPES.has(mimeType)) {
    return null;
  }

  if (asset.fileSize && asset.fileSize > MAX_LISTING_IMAGE_SIZE_BYTES) {
    return null;
  }

  return {
    uri: asset.uri,
    fileName: asset.fileName ?? `listing-image-${Date.now()}.jpg`,
    mimeType,
    fileSize: asset.fileSize,
  };
}

function normalizeAssets(assets: ImagePicker.ImagePickerAsset[]) {
  return assets.map(normalizeAsset).filter((asset): asset is PickedListingImage => Boolean(asset));
}

export function ImagePickerButton({ disabled, remainingSlots, onImagesPicked }: ImagePickerButtonProps) {
  async function pickFromLibrary() {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (!permission.granted) {
      Alert.alert('Permission needed', 'Please allow photo access to add listing images.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      allowsMultipleSelection: true,
      mediaTypes: ['images'],
      quality: 0.9,
      selectionLimit: remainingSlots,
    });

    if (result.canceled) {
      return;
    }

    const images = normalizeAssets(result.assets).slice(0, remainingSlots);

    if (images.length === 0) {
      Alert.alert('Unsupported image', 'Please choose JPG, PNG, WebP, HEIC, or HEIF images under 10 MB.');
      return;
    }

    onImagesPicked(images);
  }

  async function captureWithCamera() {
    const permission = await ImagePicker.requestCameraPermissionsAsync();

    if (!permission.granted) {
      Alert.alert('Permission needed', 'Please allow camera access to take listing photos.');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      allowsEditing: false,
      mediaTypes: ['images'],
      quality: 0.9,
    });

    if (result.canceled) {
      return;
    }

    const images = normalizeAssets(result.assets).slice(0, remainingSlots);

    if (images.length === 0) {
      Alert.alert('Unsupported image', 'Please use JPG, PNG, WebP, HEIC, or HEIF images under 10 MB.');
      return;
    }

    onImagesPicked(images);
  }

  const isDisabled = disabled || remainingSlots <= 0;

  return (
    <View style={styles.actions}>
      <Button disabled={isDisabled} title="Choose Photos" variant="secondary" onPress={pickFromLibrary} />
      <Button disabled={isDisabled} title="Take Photo" variant="secondary" onPress={captureWithCamera} />
    </View>
  );
}

const styles = StyleSheet.create({
  actions: {
    gap: spacing.sm,
  },
});
