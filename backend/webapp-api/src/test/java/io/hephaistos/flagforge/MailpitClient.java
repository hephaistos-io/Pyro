package io.hephaistos.flagforge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Client for interacting with Mailpit's REST API for email verification in tests.
 */
public class MailpitClient {

    private final RestClient restClient;

    public MailpitClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * Gets all messages from Mailpit.
     */
    public MessageList getMessages() {
        return restClient.get().uri("/messages").retrieve().body(MessageList.class);
    }

    /**
     * Gets a specific message by ID with full content.
     */
    public MessageContent getMessage(String id) {
        return restClient.get().uri("/message/{id}", id).retrieve().body(MessageContent.class);
    }

    /**
     * Searches for messages by recipient email.
     */
    public MessageList searchByRecipient(String email) {
        return restClient.get()
                .uri(builder -> builder.path("/search").queryParam("query", "to:" + email).build())
                .retrieve()
                .body(MessageList.class);
    }

    /**
     * Gets the latest message sent to a specific recipient.
     */
    public MessageContent getLatestMessageForRecipient(String email) {
        MessageList list = searchByRecipient(email);
        if (list.messages() == null || list.messages().isEmpty()) {
            return null;
        }
        return getMessage(list.messages().get(0).id());
    }

    /**
     * Clears all messages from Mailpit.
     */
    public void clearMailbox() {
        restClient.delete().uri("/messages").retrieve().toBodilessEntity();
    }

    /**
     * Gets the total count of messages.
     */
    public int getMessageCount() {
        MessageList list = getMessages();
        return list.total();
    }

    // DTOs for Mailpit API responses


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageList(List<Message> messages, int count, int start, int total) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(@JsonProperty("ID") String id,
                          @JsonProperty("MessageID") String messageId,
                          @JsonProperty("From") Address from, @JsonProperty("To") List<Address> to,
                          @JsonProperty("Subject") String subject,
                          @JsonProperty("Date") String date, @JsonProperty("Size") int size,
                          @JsonProperty("Attachments") int attachments) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageContent(@JsonProperty("ID") String id,
                                 @JsonProperty("MessageID") String messageId,
                                 @JsonProperty("From") Address from,
                                 @JsonProperty("To") List<Address> to,
                                 @JsonProperty("Subject") String subject,
                                 @JsonProperty("HTML") String html,
                                 @JsonProperty("Text") String text) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(@JsonProperty("Name") String name,
                          @JsonProperty("Address") String address) {
    }
}
