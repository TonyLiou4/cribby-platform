import { useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useQueryClient } from '@tanstack/react-query';

import { ListingCard } from '@/components/listing';
import { Button, LoadingSpinner, Screen, TextField } from '@/components/ui';
import { getInfiniteListingsQueryKey, useInfiniteListings } from '@/hooks';
import { colors, radius, shadows, spacing, typography } from '@/theme';
import type { GenderPreference, Listing, RoomType } from '@/types';
import { getApiErrorMessage } from '@/utils';

const LISTINGS_PAGE_SIZE = 12;
const SEARCH_DEBOUNCE_MS = 300;

type SortOption = {
  label: string;
  value: string;
};

const sortOptions: SortOption[] = [
  { label: 'Newest', value: 'createdAt,desc' },
  { label: 'Rent: Low to high', value: 'monthlyRent,asc' },
  { label: 'Rent: High to low', value: 'monthlyRent,desc' },
];

type ListingFilters = {
  city: string;
  universityName: string;
  minRent: string;
  maxRent: string;
  roomType?: RoomType;
  genderPreference?: GenderPreference;
};

const emptyFilters: ListingFilters = {
  city: '',
  universityName: '',
  minRent: '',
  maxRent: '',
};

const roomTypes: { label: string; value: RoomType }[] = [
  { label: 'Private', value: 'PRIVATE_ROOM' },
  { label: 'Shared', value: 'SHARED_ROOM' },
  { label: 'Studio', value: 'STUDIO' },
  { label: 'Other', value: 'OTHER' },
];

const genderPreferences: { label: string; value: GenderPreference }[] = [
  { label: 'Any', value: 'ANY' },
  { label: 'Female', value: 'FEMALE' },
  { label: 'Male', value: 'MALE' },
];

function useDebouncedValue(value: string, delay: number) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => clearTimeout(timeoutId);
  }, [delay, value]);

  return debouncedValue;
}

function toNumber(value: string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function getActiveFilterCount(filters: ListingFilters) {
  return [
    filters.city,
    filters.universityName,
    filters.minRent,
    filters.maxRent,
    filters.roomType,
    filters.genderPreference,
  ].filter(Boolean).length;
}

function getUniqueListings(pages?: { content: Listing[] }[]) {
  const seenIds = new Set<number>();

  return (
    pages?.flatMap((page) =>
      page.content.filter((listing) => {
        if (seenIds.has(listing.id)) {
          return false;
        }

        seenIds.add(listing.id);
        return true;
      }),
    ) ?? []
  );
}

export default function HomeScreen() {
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [sort, setSort] = useState(sortOptions[0].value);
  const [filters, setFilters] = useState<ListingFilters>(emptyFilters);
  const [draftFilters, setDraftFilters] = useState<ListingFilters>(emptyFilters);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const debouncedKeyword = useDebouncedValue(keyword.trim(), SEARCH_DEBOUNCE_MS);
  const activeFilterCount = getActiveFilterCount(filters);

  const queryParams = useMemo(
    () => ({
      size: LISTINGS_PAGE_SIZE,
      keyword: debouncedKeyword || undefined,
      sort,
      city: filters.city.trim() || undefined,
      universityName: filters.universityName.trim() || undefined,
      minRent: toNumber(filters.minRent),
      maxRent: toNumber(filters.maxRent),
      roomType: filters.roomType,
      genderPreference: filters.genderPreference,
    }),
    [debouncedKeyword, filters, sort],
  );

  const listingsQuery = useInfiniteListings(queryParams);
  const infiniteListingsQueryKey = useMemo(() => getInfiniteListingsQueryKey(queryParams), [queryParams]);
  const listings = useMemo(() => getUniqueListings(listingsQuery.data?.pages), [listingsQuery.data?.pages]);
  const isInitialLoading = listingsQuery.isLoading && listings.length === 0;
  const errorMessage = listingsQuery.error
    ? getApiErrorMessage(listingsQuery.error, 'Unable to load listings.')
    : null;

  function openFilters() {
    setDraftFilters(filters);
    setIsFilterOpen(true);
  }

  function applyFilters() {
    setFilters(draftFilters);
    setIsFilterOpen(false);
  }

  function clearFilters() {
    setDraftFilters(emptyFilters);
    setFilters(emptyFilters);
    setIsFilterOpen(false);
  }

  function handleEndReached() {
    if (
      listings.length > 0 &&
      listingsQuery.hasNextPage &&
      !listingsQuery.isFetching &&
      !listingsQuery.isFetchingNextPage
    ) {
      void listingsQuery.fetchNextPage();
    }
  }

  function handleRefresh() {
    void queryClient.resetQueries({ exact: true, queryKey: infiniteListingsQueryKey });
  }

  return (
    <Screen>
      <FlatList
        ListEmptyComponent={
          isInitialLoading ? (
            <LoadingSpinner size="large" />
          ) : errorMessage ? (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>Listings could not load.</Text>
              <Text style={styles.stateMessage}>{errorMessage}</Text>
              <Button title="Retry" variant="secondary" onPress={() => listingsQuery.refetch()} />
            </View>
          ) : (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>No rooms found</Text>
              <Text style={styles.stateMessage}>Try changing your search or filters.</Text>
            </View>
          )
        }
        ListFooterComponent={
          listingsQuery.isFetchingNextPage ? (
            <View style={styles.footerLoader}>
              <ActivityIndicator color={colors.primary} />
            </View>
          ) : null
        }
        ListHeaderComponent={
          <View style={styles.header}>
            <View style={styles.copy}>
              <Text style={styles.eyebrow}>Roommate Marketplace</Text>
              <Text style={styles.title}>Find safe rooms and compatible roommates</Text>
              <Text style={styles.subtitle}>For students, alumni, interns, and early-career professionals.</Text>
            </View>

            <View style={styles.searchRow}>
              <TextField
                autoCapitalize="none"
                autoCorrect={false}
                containerStyle={styles.searchField}
                onChangeText={setKeyword}
                placeholder="Search city, community, or neighborhood"
                returnKeyType="search"
                value={keyword}
              />
              <Button
                title={activeFilterCount ? `Filters (${activeFilterCount})` : 'Filters'}
                variant="secondary"
                style={styles.filterButton}
                textStyle={styles.filterButtonText}
                onPress={openFilters}
              />
            </View>

            <View style={styles.sortRow}>
              {sortOptions.map((option) => {
                const selected = option.value === sort;

                return (
                  <Pressable
                    key={option.value}
                    onPress={() => setSort(option.value)}
                    style={[styles.sortChip, selected && styles.sortChipSelected]}
                  >
                    <Text style={[styles.sortText, selected && styles.sortTextSelected]}>
                      {option.label}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          </View>
        }
        contentContainerStyle={styles.listContent}
        data={listings}
        keyExtractor={(item: Listing) => String(item.id)}
        onEndReached={handleEndReached}
        onEndReachedThreshold={0.55}
        refreshControl={
          <RefreshControl
            refreshing={listingsQuery.isRefetching && !listingsQuery.isFetchingNextPage}
            tintColor={colors.primary}
            onRefresh={handleRefresh}
          />
        }
        renderItem={({ item }) => <ListingCard listing={item} />}
        showsVerticalScrollIndicator={false}
      />

      <FilterSheet
        draftFilters={draftFilters}
        isVisible={isFilterOpen}
        onApply={applyFilters}
        onClear={clearFilters}
        onClose={() => setIsFilterOpen(false)}
        onChange={setDraftFilters}
      />
    </Screen>
  );
}

function FilterSheet({
  draftFilters,
  isVisible,
  onApply,
  onClear,
  onClose,
  onChange,
}: {
  draftFilters: ListingFilters;
  isVisible: boolean;
  onApply: () => void;
  onClear: () => void;
  onClose: () => void;
  onChange: (filters: ListingFilters) => void;
}) {
  function updateFilter(nextFilters: Partial<ListingFilters>) {
    onChange({ ...draftFilters, ...nextFilters });
  }

  return (
    <Modal animationType="slide" transparent visible={isVisible} onRequestClose={onClose}>
      <View style={styles.modalBackdrop}>
        <Pressable style={styles.modalScrim} onPress={onClose} />
        <View style={styles.sheet}>
          <View style={styles.sheetHandle} />
          <Text style={styles.sheetTitle}>Filters</Text>

          <TextField
            label="City"
            onChangeText={(city) => updateFilter({ city })}
            placeholder="Optional"
            value={draftFilters.city}
          />
          <TextField
            label="School or community"
            onChangeText={(universityName) => updateFilter({ universityName })}
            placeholder="Optional"
            value={draftFilters.universityName}
          />

          <View style={styles.sheetRow}>
            <TextField
              containerStyle={styles.sheetRowField}
              keyboardType="numeric"
              label="Min rent"
              onChangeText={(minRent) => updateFilter({ minRent })}
              placeholder="Optional"
              value={draftFilters.minRent}
            />
            <TextField
              containerStyle={styles.sheetRowField}
              keyboardType="numeric"
              label="Max rent"
              onChangeText={(maxRent) => updateFilter({ maxRent })}
              placeholder="Optional"
              value={draftFilters.maxRent}
            />
          </View>

          <OptionSection
            label="Room type"
            options={roomTypes}
            value={draftFilters.roomType}
            onSelect={(roomType) => updateFilter({ roomType })}
          />
          <OptionSection
            label="Gender preference"
            options={genderPreferences}
            value={draftFilters.genderPreference}
            onSelect={(genderPreference) => updateFilter({ genderPreference })}
          />

          <View style={styles.sheetActions}>
            <Button title="Apply Filters" onPress={onApply} />
            <Button title="Clear Filters" variant="secondary" onPress={onClear} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

function OptionSection<T extends string>({
  label,
  options,
  value,
  onSelect,
}: {
  label: string;
  options: { label: string; value: T }[];
  value?: T;
  onSelect: (value: T | undefined) => void;
}) {
  return (
    <View style={styles.optionSection}>
      <Text style={styles.optionLabel}>{label}</Text>
      <View style={styles.optionRow}>
        {options.map((option) => {
          const selected = option.value === value;

          return (
            <Pressable
              key={option.value}
              onPress={() => onSelect(selected ? undefined : option.value)}
              style={[styles.optionChip, selected && styles.optionChipSelected]}
            >
              <Text style={[styles.optionText, selected && styles.optionTextSelected]}>
                {option.label}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  listContent: {
    gap: spacing.md,
    paddingBottom: spacing.xl,
  },
  header: {
    gap: spacing.lg,
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
    ...typography.display,
    color: colors.text,
  },
  subtitle: {
    ...typography.body,
    color: colors.secondaryText,
  },
  searchRow: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    gap: spacing.sm,
  },
  searchField: {
    flex: 1,
  },
  filterButton: {
    minHeight: 52,
    paddingHorizontal: spacing.md,
  },
  filterButtonText: {
    ...typography.caption,
    fontWeight: '700',
  },
  sortRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  sortChip: {
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  sortChipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  sortText: {
    ...typography.caption,
    color: colors.text,
    fontWeight: '600',
  },
  sortTextSelected: {
    color: colors.surface,
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
  footerLoader: {
    paddingVertical: spacing.lg,
  },
  modalBackdrop: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  modalScrim: {
    bottom: 0,
    backgroundColor: colors.overlay,
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
  sheetTitle: {
    ...typography.heading,
    color: colors.text,
    textAlign: 'center',
  },
  sheetRow: {
    flexDirection: 'row',
    gap: spacing.md,
  },
  sheetRowField: {
    flex: 1,
  },
  optionSection: {
    gap: spacing.sm,
  },
  optionLabel: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  optionRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  optionChip: {
    borderColor: colors.border,
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  optionChipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  optionText: {
    ...typography.caption,
    color: colors.text,
    fontWeight: '600',
  },
  optionTextSelected: {
    color: colors.surface,
  },
  sheetActions: {
    gap: spacing.sm,
  },
});
