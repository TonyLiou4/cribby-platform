import { router, type Href } from 'expo-router';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';

import { Avatar, Button, Card, LoadingSpinner, Screen } from '@/components/ui';
import { useConversations } from '@/hooks';
import { colors, spacing, typography } from '@/theme';
import type { Conversation, ConversationParticipant } from '@/types';
import { getApiErrorMessage, getConversationId } from '@/utils';

function getParticipantName(participant?: ConversationParticipant | null) {
  const fullName = [participant?.firstName, participant?.lastName].filter(Boolean).join(' ');
  return fullName || participant?.email || 'Roommate';
}

function getOtherParticipant(conversation: Conversation) {
  return conversation.otherParticipant ?? null;
}

function getMessageText(message?: string | null) {
  if (!message) {
    return 'No messages yet.';
  }

  return message;
}

function getListingTitle(conversation: Conversation) {
  return conversation.listing?.title ?? 'Listing conversation';
}

function formatUpdatedAt(value?: string | null) {
  if (!value) {
    return '';
  }

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}

function ConversationRow({
  conversation,
}: {
  conversation: Conversation;
}) {
  const otherParticipant = getOtherParticipant(conversation);
  const participantName = getParticipantName(otherParticipant);
  const updatedAt = conversation.lastUpdatedAt ?? conversation.createdAt ?? null;

  return (
    <Pressable
      onPress={() => {
        const conversationId = getConversationId(conversation);

        if (__DEV__) {
          // TODO: Safe to remove after conversation routing smoke tests pass.
          console.log('Messages conversation id:', conversationId);
        }

        if (conversationId) {
          router.push(`/conversations/${conversationId}` as Href);
        }
      }}
    >
      {({ pressed }) => (
        <Card style={[styles.rowCard, pressed && styles.pressed]}>
          <Avatar name={participantName} size={48} />
          <View style={styles.rowContent}>
            <View style={styles.rowHeader}>
              <Text numberOfLines={1} style={styles.participantName}>
                {participantName}
              </Text>
              <Text style={styles.updatedAt}>{formatUpdatedAt(updatedAt)}</Text>
            </View>
            <Text numberOfLines={1} style={styles.listingTitle}>
              {getListingTitle(conversation)}
            </Text>
            <Text numberOfLines={1} style={styles.preview}>
              {getMessageText(conversation.lastMessagePreview)}
            </Text>
          </View>
        </Card>
      )}
    </Pressable>
  );
}

export default function MessagesScreen() {
  const conversationsQuery = useConversations();
  const conversations = conversationsQuery.data ?? [];
  const errorMessage = conversationsQuery.error
    ? getApiErrorMessage(conversationsQuery.error, 'Unable to load conversations.')
    : undefined;

  return (
    <Screen>
      <FlatList
        ListEmptyComponent={
          conversationsQuery.isLoading ? (
            <LoadingSpinner size="large" />
          ) : errorMessage ? (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>Messages could not load.</Text>
              <Text style={styles.stateMessage}>{errorMessage}</Text>
              <Button title="Try again" variant="secondary" onPress={() => conversationsQuery.refetch()} />
            </View>
          ) : (
            <View style={styles.stateCard}>
              <Text style={styles.stateTitle}>No messages yet.</Text>
              <Text style={styles.stateMessage}>Start a conversation from a listing.</Text>
            </View>
          )
        }
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.eyebrow}>Messages</Text>
            <Text style={styles.title}>Conversations</Text>
            <Text style={styles.subtitle}>Chat with people connected to listings you care about.</Text>
          </View>
        }
        contentContainerStyle={styles.listContent}
        data={conversations}
        keyExtractor={(item) => item.conversationId.toString()}
        refreshControl={
          <RefreshControl
            refreshing={conversationsQuery.isRefetching}
            tintColor={colors.primary}
            onRefresh={conversationsQuery.refetch}
          />
        }
        renderItem={({ item }) => <ConversationRow conversation={item} />}
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
    gap: spacing.xs,
    paddingBottom: spacing.sm,
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
  rowCard: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.md,
  },
  pressed: {
    opacity: 0.86,
  },
  rowContent: {
    flex: 1,
    gap: spacing.xs,
  },
  rowHeader: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: spacing.sm,
    justifyContent: 'space-between',
  },
  participantName: {
    ...typography.body,
    color: colors.text,
    flex: 1,
    fontWeight: '700',
  },
  updatedAt: {
    ...typography.caption,
    color: colors.secondaryText,
  },
  listingTitle: {
    ...typography.caption,
    color: colors.text,
    fontWeight: '600',
  },
  preview: {
    ...typography.caption,
    color: colors.secondaryText,
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
});
