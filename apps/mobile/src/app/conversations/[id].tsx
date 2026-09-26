import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import {
  FlatList,
  KeyboardAvoidingView,
  Platform,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button, Card, LoadingSpinner, Screen } from '@/components/ui';
import { useConversationMessages, useSendMessage } from '@/hooks';
import { useAuthStore } from '@/store';
import { colors, radius, spacing, typography } from '@/theme';
import type { Message } from '@/types';
import { getApiErrorMessage } from '@/utils';

function getMessageText(message: Message) {
  return message.content;
}

function isMine(message: Message, currentUserId?: number) {
  return currentUserId != null && message.senderId === currentUserId;
}

function MessageBubble({ message, currentUserId }: { message: Message; currentUserId?: number }) {
  const mine = isMine(message, currentUserId);

  return (
    <View style={[styles.messageRow, mine && styles.myMessageRow]}>
      <View style={[styles.bubble, mine ? styles.myBubble : styles.otherBubble]}>
        <Text style={[styles.messageText, mine && styles.myMessageText]}>{getMessageText(message)}</Text>
      </View>
    </View>
  );
}

export default function ConversationDetailScreen() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const routeId = Array.isArray(id) ? id[0] : id;
  const conversationId = Number(routeId);
  const hasValidId = Number.isFinite(conversationId) && conversationId > 0;
  const currentUserId = useAuthStore((state) => state.user?.id);
  const messagesQuery = useConversationMessages(hasValidId ? conversationId : undefined);
  const sendMessage = useSendMessage(conversationId);
  const [content, setContent] = useState('');
  const messages = [...(messagesQuery.data ?? [])].sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
  );
  const trimmedContent = content.trim();
  const sendError = sendMessage.error
    ? getApiErrorMessage(sendMessage.error, 'Unable to send message.')
    : undefined;

  if (__DEV__) {
    // TODO: Safe to remove after conversation routing smoke tests pass.
    console.log('Conversation route id:', routeId, 'parsed:', conversationId);
  }

  async function handleSend() {
    if (!trimmedContent || !hasValidId) {
      return;
    }

    try {
      if (__DEV__) {
        // TODO: Safe to remove after messaging smoke tests pass.
        console.log('Sending message payload', {
          conversationId,
          contentLength: trimmedContent.length,
        });
      }

      await sendMessage.mutateAsync({ content: trimmedContent });
      setContent('');
    } catch {
      // React Query exposes the error to the screen.
    }
  }

  if (!hasValidId) {
    return (
      <Screen centered>
        <Card style={styles.stateCard}>
          <Text style={styles.stateTitle}>Conversation not found.</Text>
          <Button title="Go back" variant="secondary" onPress={() => router.back()} />
        </Card>
      </Screen>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.keyboardView}
      >
        <FlatList
          ListEmptyComponent={
            messagesQuery.isLoading ? (
              <LoadingSpinner size="large" />
            ) : messagesQuery.error ? (
              <View style={styles.stateCard}>
                <Text style={styles.stateTitle}>Messages could not load.</Text>
                <Text style={styles.stateMessage}>
                  {getApiErrorMessage(messagesQuery.error, 'Unable to load messages.')}
                </Text>
                <Button title="Try again" variant="secondary" onPress={() => messagesQuery.refetch()} />
              </View>
            ) : (
              <View style={styles.stateCard}>
                <Text style={styles.stateTitle}>No messages yet.</Text>
                <Text style={styles.stateMessage}>Send the first message to start the conversation.</Text>
              </View>
            )
          }
          contentContainerStyle={styles.messageList}
          data={messages}
          keyExtractor={(item) => String(item.id)}
          refreshControl={
            <RefreshControl
              refreshing={messagesQuery.isRefetching}
              tintColor={colors.primary}
              onRefresh={messagesQuery.refetch}
            />
          }
          renderItem={({ item }) => <MessageBubble message={item} currentUserId={currentUserId} />}
          showsVerticalScrollIndicator={false}
        />

        {sendError ? <Text style={styles.error}>{sendError}</Text> : null}

        <View style={styles.composer}>
          <TextInput
            multiline
            onChangeText={setContent}
            placeholder="Message"
            placeholderTextColor={colors.secondaryText}
            style={styles.input}
            value={content}
          />
          <Button
            disabled={!trimmedContent || sendMessage.isPending}
            loading={sendMessage.isPending}
            onPress={() => {
              void handleSend();
            }}
            style={styles.sendButton}
            title="Send"
          />
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    backgroundColor: colors.background,
    flex: 1,
  },
  keyboardView: {
    flex: 1,
  },
  messageList: {
    flexGrow: 1,
    gap: spacing.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
  },
  messageRow: {
    alignItems: 'flex-start',
  },
  myMessageRow: {
    alignItems: 'flex-end',
  },
  bubble: {
    borderRadius: radius.lg,
    maxWidth: '82%',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  myBubble: {
    backgroundColor: colors.primary,
    borderBottomRightRadius: radius.sm,
  },
  otherBubble: {
    backgroundColor: colors.surface,
    borderBottomLeftRadius: radius.sm,
    borderColor: colors.border,
    borderWidth: StyleSheet.hairlineWidth,
  },
  messageText: {
    ...typography.body,
    color: colors.text,
  },
  myMessageText: {
    color: colors.surface,
  },
  composer: {
    alignItems: 'flex-end',
    backgroundColor: colors.surface,
    borderTopColor: colors.border,
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: 'row',
    gap: spacing.sm,
    padding: spacing.md,
  },
  input: {
    ...typography.body,
    backgroundColor: colors.background,
    borderColor: colors.border,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    color: colors.text,
    flex: 1,
    maxHeight: 120,
    minHeight: 44,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  sendButton: {
    minHeight: 44,
    paddingHorizontal: spacing.md,
  },
  error: {
    ...typography.caption,
    color: colors.error,
    paddingHorizontal: spacing.lg,
    textAlign: 'center',
  },
  stateCard: {
    gap: spacing.md,
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
