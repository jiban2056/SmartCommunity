package domain;

import java.time.LocalDateTime;

/**
 * Records a reference to supporting evidence.
 * File copying and existence checks belong to infrastructure.
 */
public final class Evidence implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final String fileReference;
    private final String description;
    private final UserAccount uploadedBy;
    private final LocalDateTime uploadedAt;

    public Evidence(
            String fileReference,
            String description,
            UserAccount uploadedBy,
            LocalDateTime uploadedAt) {

        if (fileReference == null || fileReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Evidence file reference must not be blank.");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Evidence description must not be blank.");
        }

        if (uploadedBy == null || uploadedAt == null) {
            throw new IllegalArgumentException(
                    "Uploader and upload time are required.");
        }

        this.fileReference = fileReference.trim();
        this.description = description.trim();
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public String getFileReference() {
        return fileReference;
    }

    public String getDescription() {
        return description;
    }

    public UserAccount getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}