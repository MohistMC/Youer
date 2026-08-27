package com.mohistmc.youer.ai.history;

import com.mohistmc.youer.ai.model.AiMessage;
import com.mohistmc.youer.ai.model.AiRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class AiConversationStore {

    private final ConcurrentHashMap<UUID, Conversation> conversations = new ConcurrentHashMap<>();
    private final AtomicLong versions = new AtomicLong();
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();

    public AiConversationSnapshot snapshot(UUID playerId) {
        lifecycleLock.readLock().lock();
        try {
            Conversation conversation = conversations.computeIfAbsent(
                    playerId, ignored -> new Conversation(versions.incrementAndGet()));
            synchronized (conversation) {
                return new AiConversationSnapshot(conversation.version, conversation.messages);
            }
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    public boolean appendIfVersion(
            UUID playerId,
            long expectedVersion,
            AiMessage userMessage,
            AiMessage assistantMessage,
            int maxHistory) {
        requireTurn(userMessage, assistantMessage);
        lifecycleLock.readLock().lock();
        try {
            Conversation conversation = conversations.get(playerId);
            if (conversation == null) {
                return false;
            }
            synchronized (conversation) {
                if (conversation.version != expectedVersion) {
                    return false;
                }
                conversation.messages.add(userMessage);
                conversation.messages.add(assistantMessage);
                int limit = evenLimit(maxHistory);
                while (conversation.messages.size() > limit) {
                    conversation.messages.remove(0);
                    conversation.messages.remove(0);
                }
                return true;
            }
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    public boolean appendIfVersion(
            UUID playerId, long expectedVersion, AiConversationTurn turn, int maxHistory) {
        lifecycleLock.readLock().lock();
        try {
            Conversation conversation = conversations.get(playerId);
            if (conversation == null) return false;
            synchronized (conversation) {
                if (conversation.version != expectedVersion) return false;
                conversation.messages.addAll(turn.messages());
                int limit = Math.max(2, maxHistory);
                while (conversation.messages.size() > limit) {
                    conversation.messages.remove(0);
                    while (!conversation.messages.isEmpty()
                            && conversation.messages.getFirst().role() != AiRole.USER) {
                        conversation.messages.remove(0);
                    }
                    if (conversation.messages.isEmpty()) {
                        conversation.messages.addAll(turn.messages());
                        break;
                    }
                }
                return true;
            }
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    public void clear(UUID playerId) {
        lifecycleLock.readLock().lock();
        try {
            Conversation conversation = conversations.computeIfAbsent(
                    playerId, ignored -> new Conversation(versions.incrementAndGet()));
            synchronized (conversation) {
                conversation.messages.clear();
                conversation.version = versions.incrementAndGet();
            }
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    public void clearAll() {
        lifecycleLock.writeLock().lock();
        try {
            versions.incrementAndGet();
            conversations.clear();
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    public Map<UUID, AiConversationSnapshot> snapshots() {
        lifecycleLock.readLock().lock();
        try {
            Map<UUID, AiConversationSnapshot> result = new LinkedHashMap<>();
            conversations.forEach((playerId, conversation) -> {
                synchronized (conversation) {
                    if (!conversation.messages.isEmpty()) {
                        result.put(playerId, new AiConversationSnapshot(conversation.version, conversation.messages));
                    }
                }
            });
            return Map.copyOf(result);
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    private static void requireTurn(AiMessage userMessage, AiMessage assistantMessage) {
        if (userMessage.role() != AiRole.USER || assistantMessage.role() != AiRole.ASSISTANT) {
            throw new IllegalArgumentException("Conversation history accepts only complete USER/ASSISTANT turns");
        }
    }

    private static int evenLimit(int maxHistory) {
        int limit = Math.max(2, maxHistory);
        return limit - limit % 2;
    }

    private static final class Conversation {
        private long version;
        private final List<AiMessage> messages = new ArrayList<>();

        private Conversation(long version) {
            this.version = version;
        }
    }
}
