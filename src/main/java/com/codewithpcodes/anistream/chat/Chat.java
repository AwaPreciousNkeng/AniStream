package com.codewithpcodes.anistream.chat;

import com.codewithpcodes.anistream.common.BaseAuditingEntity;
import com.codewithpcodes.anistream.message.Message;
import com.codewithpcodes.anistream.message.MessageState;
import com.codewithpcodes.anistream.message.MessageType;
import com.codewithpcodes.anistream.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chat")
@NamedQuery(name = ChatConstants.FIND_CHAT_BY_SENDER_ID,
        query = "SELECT DISTINCT c FROM Chat c WHERE c.sender.id = :senderId OR c.recipient.id = :senderId ORDER BY createdDate DESC"
)
@NamedQuery(name = ChatConstants.FIND_CHAT_BY_SENDER_ID_AND_RECEIVER,
        query = "SELECT DISTINCT c FROM Chat c WHERE (c.sender.id = :senderId AND c.recipient.id = :recipientId) OR (c.sender.id = :recipientId AND c.recipient.id = :senderId) ORDER BY createdDate DESC"
)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @OneToMany(mappedBy = "chat", fetch = FetchType.EAGER)
    @OrderBy("createdDate DESC ")
    private List<Message> messages;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type", nullable = false)
    private ChatType type;


    @OneToMany(
            mappedBy = "chat",
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ChatMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    public String getChatName(final UUID senderId) {
        if (recipient.getId().equals(senderId)) {
            return sender.getFirstName() + " " + sender.getLastName();
        }
        return recipient.getFirstName() + " " + recipient.getLastName();
    }

    @Transient
    public long getUnreadMessagesCount(final UUID senderId) {
        return messages
                .stream()
                .filter(m -> m.getReceiverId().equals(senderId))
                .filter(m -> MessageState.SENT == m.getState())
                .count();
    }

    @Transient
    public String getLastMessage() {
        if (messages != null && !messages.isEmpty()) {
            if (messages.getFirst().getType() != MessageType.TEXT) {
                return "Attachment";
            }
            return messages.getFirst().getContent();
        }
        return null;
    }

    @Transient
    public LocalDateTime getLastMessageTime() {
        if (messages != null && !messages.isEmpty()) {
            return messages.getFirst().getCreatedDate();
        }
        return null;
    }
}
