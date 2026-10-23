package infrastructure;

import application.ServiceRequestRepository;
import domain.ServiceRequest;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * File storage for a single running prototype instance.
 * Reads fresh objects and commits updates through an atomic file move.
 * Stored files are local application data, not user-uploaded imports.
 */
public final class FileServiceRequestRepository
        implements ServiceRequestRepository {

    private static final long MAX_FILE_BYTES = 10_000_000;

    private static final Set<String> ALLOWED_CLASSES = Set.of(
            "domain.ServiceRequest",
            "domain.UserAccount",
            "domain.Role",
            "domain.RequestStatus",
            "domain.Priority",
            "domain.Location",
            "domain.RequestCategory",
            "domain.MaintenanceTeam",
            "domain.TaskAssignment",
            "domain.RequestHistoryEntry",
            "domain.Evidence",
            "domain.Feedback",
            "domain.Notification",
            "domain.NotificationChannel",
            "domain.DeliveryStatus",
            "java.lang.Object",
            "java.lang.String",
            "java.lang.Enum",
            "java.lang.Number",
            "java.lang.Double",
            "java.util.ArrayList",
            "java.util.HashSet",
            "java.util.Map$Entry",
            "java.util.CollSer",
            "java.util.ImmutableCollections$Set12",
            "java.util.ImmutableCollections$SetN",
            "java.time.Ser",
            "java.time.LocalDateTime"
    );

    private final Path file;

    public FileServiceRequestRepository(Path file) {
        this.file = Objects.requireNonNull(
                file, "Storage path is required.")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public synchronized void save(ServiceRequest request) {
        Objects.requireNonNull(request, "Request is required.");

        List<ServiceRequest> requests = readRequests();
        boolean replaced = false;

        for (int index = 0; index < requests.size(); index++) {
            if (requests.get(index).getReference()
                    .equals(request.getReference())) {
                requests.set(index, request);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            requests.add(request);
        }

        writeRequests(requests);
    }

    @Override
    public synchronized Optional<ServiceRequest> findByReference(
            String reference) {

        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }

        return readRequests().stream()
                .filter(request -> request.getReference()
                        .equals(reference.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<ServiceRequest> findAll() {
        return List.copyOf(readRequests());
    }

    private List<ServiceRequest> readRequests() {
        try {
            if (Files.size(file) > MAX_FILE_BYTES) {
                throw new IOException("Storage file exceeds size limit.");
            }

            try (ObjectInputStream input = new ObjectInputStream(
                    Files.newInputStream(file))) {

                input.setObjectInputFilter(
                        FileServiceRequestRepository::filterClass);

                Object stored = input.readObject();

                if (!(stored instanceof ArrayList<?> entries)) {
                    throw new IOException("Unexpected storage format.");
                }

                List<ServiceRequest> requests = new ArrayList<>();
                Set<String> references = new HashSet<>();

                for (Object entry : entries) {
                    if (!(entry instanceof ServiceRequest request)) {
                        throw new IOException(
                                "Storage contains an invalid entry.");
                    }

                    String reference = request.getReference();

                    if (reference == null || reference.isBlank()
                            || !references.add(reference)) {
                        throw new IOException(
                                "Storage contains invalid or duplicate references.");
                    }

                    requests.add(request);
                }

                return requests;
            }

        } catch (NoSuchFileException exception) {
            // A missing file is normal before the first successful save.
            return new ArrayList<>();

        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Could not load requests. Existing data was not reset.",
                    exception);
        }
    }

    private void writeRequests(List<ServiceRequest> requests) {
        Path temporaryFile = null;

        try {
            Files.createDirectories(file.getParent());

            temporaryFile = Files.createTempFile(
                    file.getParent(), "requests-", ".tmp");

            try (ObjectOutputStream output = new ObjectOutputStream(
                    Files.newOutputStream(temporaryFile))) {
                output.writeObject(new ArrayList<>(requests));
            }

            if (Files.size(temporaryFile) > MAX_FILE_BYTES) {
                throw new IOException("Storage file exceeds size limit.");
            }

            // Commit only after the complete replacement file is written.
            // Unsupported atomic moves are reported as a save failure.
            Files.move(
                    temporaryFile,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not save requests. Do not assume changes were saved.",
                    exception);

        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    // A leftover temporary file is never used as saved data.
                }
            }
        }
    }

    private static ObjectInputFilter.Status filterClass(
            ObjectInputFilter.FilterInfo information) {

        if (information.depth() > 50
                || information.references() > 100_000
                || information.streamBytes() > MAX_FILE_BYTES
                || information.arrayLength() > 100_000) {
            return ObjectInputFilter.Status.REJECTED;
        }

        Class<?> type = information.serialClass();

        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }

        while (type.isArray()) {
            type = type.getComponentType();
        }

        if (type.isPrimitive()
                || ALLOWED_CLASSES.contains(type.getName())) {
            return ObjectInputFilter.Status.ALLOWED;
        }

        return ObjectInputFilter.Status.REJECTED;
    }
}