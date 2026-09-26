import { Image, ScrollView, StyleSheet, Text, View, useWindowDimensions } from 'react-native';

import { colors, radius, spacing, typography } from '@/theme';
import type { ListingImage } from '@/types';

type ListingImageCarouselProps = {
  images: ListingImage[];
  heightRatio?: number;
};

export function ListingImageCarousel({ images, heightRatio = 10 / 16 }: ListingImageCarouselProps) {
  const { width } = useWindowDimensions();
  const imageWidth = width - spacing.lg * 2;

  return (
    <ScrollView
      horizontal
      pagingEnabled
      showsHorizontalScrollIndicator={false}
      style={styles.carousel}
    >
      {images.length > 0 ? (
        images.map((image) => (
          <Image
            key={`${image.id ?? image.url}`}
            source={{ uri: image.url }}
            style={[styles.image, { height: imageWidth * heightRatio, width: imageWidth }]}
          />
        ))
      ) : (
        <View style={[styles.placeholder, { height: imageWidth * heightRatio, width: imageWidth }]}>
          <Text style={styles.placeholderText}>No photos yet</Text>
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  carousel: {
    borderRadius: radius.lg,
  },
  image: {
    backgroundColor: colors.border,
    borderRadius: radius.lg,
  },
  placeholder: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    justifyContent: 'center',
  },
  placeholderText: {
    ...typography.caption,
    color: colors.secondaryText,
    fontWeight: '600',
  },
});
