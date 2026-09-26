import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';

import { Button, Card, LoadingSpinner, Screen, TextField } from '@/components/ui';
import { useCurrentUser, useUpdateProfile } from '@/hooks';
import { colors, radius, spacing, typography } from '@/theme';
import type { Gender, UpdateProfileRequest, User, UserType } from '@/types';
import { getApiErrorMessage } from '@/utils';

const userTypes: { label: string; value: UserType }[] = [
  { label: 'Student', value: 'STUDENT' },
  { label: 'Alumni', value: 'ALUMNI' },
  { label: 'Incoming student', value: 'INCOMING_STUDENT' },
  { label: 'Professional / Other', value: 'OTHER' },
];

const genderOptions: { label: string; value: Gender }[] = [
  { label: 'Male', value: 'MALE' },
  { label: 'Female', value: 'FEMALE' },
  { label: 'Non-binary', value: 'NON_BINARY' },
  { label: 'Other', value: 'OTHER' },
  { label: 'Prefer not to say', value: 'PREFER_NOT_TO_SAY' },
];

const optionalUrl = z
  .string()
  .trim()
  .refine((value) => {
    if (!value) {
      return true;
    }

    try {
      new URL(value);
      return true;
    } catch {
      return false;
    }
  }, 'Enter a valid URL.');

const editProfileSchema = z.object({
  firstName: z.string().trim().min(1, 'First name is required.'),
  lastName: z.string().trim().min(1, 'Last name is required.'),
  phoneNumber: z.string().trim(),
  universityName: z.string().trim(),
  profilePhotoUrl: optionalUrl,
  bio: z.string().trim().max(500, 'Bio must be 500 characters or fewer.'),
  gender: z.enum(['MALE', 'FEMALE', 'NON_BINARY', 'OTHER', 'PREFER_NOT_TO_SAY']),
  userType: z.enum(['STUDENT', 'ALUMNI', 'INCOMING_STUDENT', 'OTHER']),
});

type EditProfileFormValues = z.infer<typeof editProfileSchema>;

function toFormValues(user: User): EditProfileFormValues {
  return {
    firstName: user.firstName ?? '',
    lastName: user.lastName ?? '',
    phoneNumber: user.phoneNumber ?? '',
    universityName: user.universityName ?? '',
    profilePhotoUrl: user.profilePhotoUrl ?? '',
    bio: user.bio ?? '',
    gender: user.gender ?? 'PREFER_NOT_TO_SAY',
    userType: user.userType ?? 'OTHER',
  };
}

function toRequest(values: EditProfileFormValues): UpdateProfileRequest {
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    phoneNumber: values.phoneNumber.trim() || null,
    universityName: values.universityName.trim() || null,
    profilePhotoUrl: values.profilePhotoUrl.trim() || null,
    bio: values.bio.trim() || null,
    gender: values.gender,
    userType: values.userType,
  };
}

export default function EditProfileScreen() {
  const userQuery = useCurrentUser();
  const updateProfile = useUpdateProfile();
  const user = userQuery.data;
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<EditProfileFormValues>({
    resolver: zodResolver(editProfileSchema),
    values: user ? toFormValues(user) : undefined,
  });

  async function onSubmit(values: EditProfileFormValues) {
    try {
      await updateProfile.mutateAsync(toRequest(values));
      router.back();
    } catch (error) {
      setError('root', {
        message: getApiErrorMessage(error, 'Unable to update profile.'),
      });
    }
  }

  if (userQuery.isLoading || !user) {
    return (
      <Screen centered>
        <LoadingSpinner size="large" />
      </Screen>
    );
  }

  if (userQuery.error) {
    return (
      <Screen centered>
        <Card style={styles.stateCard}>
          <Text style={styles.stateTitle}>Profile could not load.</Text>
          <Text style={styles.stateMessage}>
            {getApiErrorMessage(userQuery.error, 'Unable to load profile.')}
          </Text>
          <Button title="Try again" variant="secondary" onPress={() => userQuery.refetch()} />
        </Card>
      </Screen>
    );
  }

  const isLoading = isSubmitting || updateProfile.isPending;

  return (
    <Screen scroll contentContainerStyle={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>Profile</Text>
        <Text style={styles.title}>Edit Profile</Text>
        <Text style={styles.subtitle}>Keep your shared-housing profile accurate and trustworthy.</Text>
      </View>

      <Card style={styles.formCard}>
        <View style={styles.row}>
          <Controller
            control={control}
            name="firstName"
            render={({ field: { onBlur, onChange, value } }) => (
              <TextField
                containerStyle={styles.rowField}
                error={errors.firstName?.message}
                label="First name"
                onBlur={onBlur}
                onChangeText={onChange}
                value={value}
              />
            )}
          />
          <Controller
            control={control}
            name="lastName"
            render={({ field: { onBlur, onChange, value } }) => (
              <TextField
                containerStyle={styles.rowField}
                error={errors.lastName?.message}
                label="Last name"
                onBlur={onBlur}
                onChangeText={onChange}
                value={value}
              />
            )}
          />
        </View>

        <Controller
          control={control}
          name="phoneNumber"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              error={errors.phoneNumber?.message}
              keyboardType="phone-pad"
              label="Phone number"
              onBlur={onBlur}
              onChangeText={onChange}
              placeholder="Optional"
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="universityName"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              error={errors.universityName?.message}
              label="School or community"
              onBlur={onBlur}
              onChangeText={onChange}
              placeholder="Optional"
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="profilePhotoUrl"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              autoCapitalize="none"
              error={errors.profilePhotoUrl?.message}
              label="Profile photo URL"
              onBlur={onBlur}
              onChangeText={onChange}
              placeholder="https://..."
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="bio"
          render={({ field: { onBlur, onChange, value } }) => (
            <TextField
              error={errors.bio?.message}
              label="Bio"
              multiline
              onBlur={onBlur}
              onChangeText={onChange}
              placeholder="Tell potential roommates a little about you."
              style={styles.textArea}
              textAlignVertical="top"
              value={value}
            />
          )}
        />

        <Controller
          control={control}
          name="gender"
          render={({ field: { onChange, value } }) => (
            <OptionGroup label="Gender" options={genderOptions} value={value} onChange={onChange} />
          )}
        />

        <Controller
          control={control}
          name="userType"
          render={({ field: { onChange, value } }) => (
            <OptionGroup label="User type" options={userTypes} value={value} onChange={onChange} />
          )}
        />

        {errors.root?.message ? <Text style={styles.error}>{errors.root.message}</Text> : null}

        <Button disabled={isLoading} loading={isLoading} title="Save Profile" onPress={handleSubmit(onSubmit)} />
      </Card>
    </Screen>
  );
}

type Option<T extends string> = {
  label: string;
  value: T;
};

function OptionGroup<T extends string>({
  label,
  options,
  value,
  onChange,
}: {
  label: string;
  options: Option<T>[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <View style={styles.optionGroup}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.optionGrid}>
        {options.map((option) => {
          const selected = option.value === value;

          return (
            <Pressable
              key={option.value}
              onPress={() => onChange(option.value)}
              style={[styles.option, selected && styles.optionSelected]}
            >
              <Text style={[styles.optionText, selected && styles.optionTextSelected]}>{option.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
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
  formCard: {
    gap: spacing.md,
  },
  row: {
    flexDirection: 'row',
    gap: spacing.md,
  },
  rowField: {
    flex: 1,
    minWidth: 0,
  },
  textArea: {
    minHeight: 120,
    paddingTop: spacing.md,
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
  error: {
    ...typography.caption,
    color: colors.error,
    textAlign: 'center',
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
