import { zodResolver } from '@hookform/resolvers/zod';
import DateTimePicker from '@react-native-community/datetimepicker';
import { Controller, useForm } from 'react-hook-form';
import { Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';

import { Button, Card, TextField } from '@/components/ui';
import { colors, radius, spacing, typography } from '@/theme';
import type { CreateListingRequest, GenderPreference, RoomType } from '@/types';

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

function startOfToday() {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return today;
}

function toDateValue(value: string) {
  if (!value) {
    return new Date();
  }

  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime()) ? new Date() : date;
}

function formatDateValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

const listingFormSchema = z.object({
  title: z.string().trim().min(1, 'Title is required.').min(4, 'Title must be at least 4 characters.'),
  description: z.string().trim().min(1, 'Description is required.').min(20, 'Description must be at least 20 characters.'),
  city: z.string().trim().min(1, 'City is required.'),
  neighborhood: z.string().trim().min(1, 'Neighborhood is required.'),
  universityName: z.string().trim().optional(),
  monthlyRent: z
    .string()
    .trim()
    .min(1, 'Monthly rent is required.')
    .refine((value) => Number.isFinite(Number(value)), 'Monthly rent must be a number.')
    .refine((value) => Number(value) > 0, 'Monthly rent must be greater than 0.'),
  utilitiesIncluded: z.boolean(),
  availableFrom: z
    .string()
    .trim()
    .min(1, 'Available from date is required.')
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Use YYYY-MM-DD format.')
    .refine((value) => new Date(`${value}T00:00:00`) >= startOfToday(), 'Available date cannot be in the past.'),
  roomType: z.enum(['PRIVATE_ROOM', 'SHARED_ROOM', 'STUDIO', 'OTHER']),
  genderPreference: z.enum(['MALE', 'FEMALE', 'ANY']),
  exactAddress: z.string().trim().optional(),
});

export type ListingFormValues = z.infer<typeof listingFormSchema>;

export const defaultListingFormValues: ListingFormValues = {
  title: '',
  description: '',
  city: '',
  neighborhood: '',
  universityName: '',
  monthlyRent: '',
  utilitiesIncluded: true,
  availableFrom: '',
  roomType: 'PRIVATE_ROOM',
  genderPreference: 'ANY',
  exactAddress: '',
};

export function toListingRequest(values: ListingFormValues): CreateListingRequest {
  return {
    title: values.title.trim(),
    description: values.description.trim(),
    city: values.city.trim(),
    neighborhood: values.neighborhood.trim(),
    universityName: values.universityName?.trim() || null,
    monthlyRent: Number(values.monthlyRent),
    utilitiesIncluded: values.utilitiesIncluded,
    availableFrom: values.availableFrom.trim(),
    roomType: values.roomType,
    genderPreference: values.genderPreference,
    exactAddress: values.exactAddress?.trim() || null,
  };
}

type ListingFormProps = {
  defaultValues?: ListingFormValues;
  error?: string;
  isLoading?: boolean;
  onSubmit: (values: ListingFormValues) => Promise<void>;
  showUniversityName?: boolean;
  submitLabel: string;
  successMessage?: string;
};

export function ListingForm({
  defaultValues = defaultListingFormValues,
  error,
  isLoading = false,
  onSubmit,
  showUniversityName = true,
  submitLabel,
  successMessage,
}: ListingFormProps) {
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ListingFormValues>({
    resolver: zodResolver(listingFormSchema),
    defaultValues,
    values: defaultValues,
  });

  const disabled = isLoading || isSubmitting;

  return (
    <Card style={styles.formCard}>
      <Controller
        control={control}
        name="title"
        render={({ field: { onBlur, onChange, value } }) => (
          <TextField
            error={errors.title?.message}
            label="Title"
            onBlur={onBlur}
            onChangeText={onChange}
            value={value}
          />
        )}
      />

      <Controller
        control={control}
        name="description"
        render={({ field: { onBlur, onChange, value } }) => (
          <TextField
            error={errors.description?.message}
            label="Description"
            multiline
            onBlur={onBlur}
            onChangeText={onChange}
            style={styles.textArea}
            textAlignVertical="top"
            value={value}
          />
        )}
      />

      <View style={styles.row}>
        <Controller
          control={control}
          name="city"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              containerStyle={styles.rowField}
              error={errors.city?.message}
              label="City"
              onBlur={onBlur}
              onChangeText={onChange}
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="neighborhood"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              containerStyle={styles.rowField}
              error={errors.neighborhood?.message}
              label="Neighborhood"
              onBlur={onBlur}
              onChangeText={onChange}
              value={value}
            />
          )}
        />
      </View>

      {showUniversityName ? (
        <Controller
          control={control}
          name="universityName"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              error={errors.universityName?.message}
              label="School or community (optional)"
              onBlur={onBlur}
              onChangeText={onChange}
              value={value}
            />
          )}
        />
      ) : null}

      <View style={styles.row}>
        <Controller
          control={control}
          name="monthlyRent"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              containerStyle={styles.rowField}
              error={errors.monthlyRent?.message}
              keyboardType="numeric"
              label="Monthly rent"
              onBlur={onBlur}
              onChangeText={onChange}
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="availableFrom"
          render={({ field: { onChange, value } }) => (
            <View style={[styles.rowField, styles.dateField]}>
              <Text style={styles.label}>Available from</Text>
              <DateTimePicker
                display={Platform.OS === 'ios' ? 'compact' : 'default'}
                minimumDate={startOfToday()}
                mode="date"
                onChange={(_event, selectedDate) => {
                  if (selectedDate) {
                    onChange(formatDateValue(selectedDate));
                  }
                }}
                value={toDateValue(value)}
              />
              {errors.availableFrom?.message ? (
                <Text style={styles.fieldError}>{errors.availableFrom.message}</Text>
              ) : null}
            </View>
          )}
        />
      </View>

      <Controller
        control={control}
        name="roomType"
        render={({ field: { onChange, value } }) => (
          <OptionGroup
            error={errors.roomType?.message}
            label="Room type"
            onChange={onChange}
            options={roomTypes}
            value={value}
          />
        )}
      />

      <Controller
        control={control}
        name="genderPreference"
        render={({ field: { onChange, value } }) => (
          <OptionGroup
            error={errors.genderPreference?.message}
            label="Gender preference"
            onChange={onChange}
            options={genderPreferences}
            value={value}
          />
        )}
      />

      <Controller
        control={control}
        name="utilitiesIncluded"
        render={({ field: { onChange, value } }) => (
          <OptionGroup
            label="Utilities"
            onChange={onChange}
            options={[
              { label: 'Included', value: true },
              { label: 'Not included', value: false },
            ]}
            value={value}
          />
        )}
      />

      <Controller
        control={control}
        name="exactAddress"
        render={({ field: { onBlur, onChange, value } }) => (
          <TextField
            error={errors.exactAddress?.message}
            label="Exact address (private, optional)"
            onBlur={onBlur}
            onChangeText={onChange}
            value={value}
          />
        )}
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}
      {successMessage ? <Text style={styles.success}>{successMessage}</Text> : null}

      <Button disabled={disabled} loading={disabled} onPress={handleSubmit(onSubmit)} title={submitLabel} />
    </Card>
  );
}

type Option<T extends string | boolean> = {
  label: string;
  value: T;
};

type OptionGroupProps<T extends string | boolean> = {
  label: string;
  options: Option<T>[];
  value: T;
  onChange: (value: T) => void;
  error?: string;
};

function OptionGroup<T extends string | boolean>({
  label,
  options,
  value,
  onChange,
  error,
}: OptionGroupProps<T>) {
  return (
    <View style={styles.optionGroup}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.optionGrid}>
        {options.map((option) => {
          const isSelected = option.value === value;

          return (
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: isSelected }}
              key={String(option.value)}
              onPress={() => onChange(option.value)}
              style={[styles.option, isSelected && styles.optionSelected]}
            >
              <Text style={[styles.optionText, isSelected && styles.optionTextSelected]}>
                {option.label}
              </Text>
            </Pressable>
          );
        })}
      </View>
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  formCard: {
    gap: spacing.md,
  },
  textArea: {
    minHeight: 128,
    paddingTop: spacing.md,
  },
  row: {
    flexDirection: 'row',
    gap: spacing.md,
  },
  rowField: {
    flex: 1,
    minWidth: 0,
  },
  dateField: {
    gap: spacing.sm,
  },
  optionGroup: {
    gap: spacing.sm,
  },
  label: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  optionGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  option: {
    borderColor: colors.border,
    borderRadius: radius.pill,
    borderWidth: StyleSheet.hairlineWidth,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  optionSelected: {
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
  fieldError: {
    ...typography.caption,
    color: colors.error,
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
});
