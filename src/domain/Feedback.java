package domain;

import java.time.LocalDateTime;

/**
 * Stores resident feedback.
 * ServiceRequest enforces completion, ownership and uniqueness.
 */
public final class Feedback implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final UserAccount author;
    private final String comment;
    private final LocalDateTime submittedAt;

    public Feedback(
            UserAccount author,
            String comment,
            LocalDateTime submittedAt) {

        if (author == null
                || !author.isVerified()
                || !author.hasRole(Role.RESIDENT)) {
            throw new IllegalArgumentException(
                    "Feedback requires a verified resident.");
        }

        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException(
                    "Feedback comment must not be blank.");
        }

        if (submittedAt == null) {
            throw new IllegalArgumentException(
                    "Feedback submission time is required.");
        }

        this.author = author;
        this.comment = comment.trim();
        this.submittedAt = submittedAt;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}