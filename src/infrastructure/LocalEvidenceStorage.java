package infrastructure;

import application.EvidenceStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Stores evidence files for the local prototype.
 * Extension checks do not verify actual file content.
 */
public final class LocalEvidenceStorage implements EvidenceStorage {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".pdf", ".txt");

    private final Path directory;

    public LocalEvidenceStorage(Path directory) {
        this.directory = Objects.requireNonNull(
                directory, "Evidence directory is required.")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public String store(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Evidence file path is required.");
        }

        Path source = Path.of(sourcePath.trim())
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new IllegalArgumentException(
                    "Evidence must be a readable file.");
        }

        String originalName = source.getFileName().toString();
        int dot = originalName.lastIndexOf('.');

        String extension = dot < 0
                ? ""
                : originalName.substring(dot).toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Use JPG, JPEG, PNG, PDF or TXT evidence.");
        }

        Path destination = null;
        boolean created = false;

        try {
            long size = Files.size(source);

            if (size == 0 || size > MAX_BYTES) {
                throw new IllegalArgumentException(
                        "Evidence must be non-empty and no larger than 5 MiB.");
            }

            Files.createDirectories(directory);

            String storedName = UUID.randomUUID() + extension;
            destination = directory.resolve(storedName);

            try (InputStream input = Files.newInputStream(source);
                 OutputStream output = Files.newOutputStream(
                         destination,
                         StandardOpenOption.CREATE_NEW,
                         StandardOpenOption.WRITE)) {

                created = true;
                byte[] buffer = new byte[8192];
                long total = 0;
                int count;

                while ((count = input.read(buffer)) != -1) {
                    total += count;

                    if (total > MAX_BYTES) {
                        throw new IOException(
                                "Evidence exceeded the size limit during copying.");
                    }

                    output.write(buffer, 0, count);
                }

                if (total == 0) {
                    throw new IOException("Evidence file became empty.");
                }
            }

            return storedName;

        } catch (IOException exception) {
            if (created && destination != null) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }

            throw new IllegalStateException(
                    "Evidence could not be stored.", exception);
        }
    }
}