package application;

import domain.ServiceRequest;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for service requests.
 *
 * Implementations must preserve assignments, history,
 * evidence references and feedback when saving a request.
 *
 * Storage failures must be reported, not silently ignored.
 */
public interface ServiceRequestRepository {

    /**
     * Saves a new request or updates an existing request
     * with the same reference.
     */
    void save(ServiceRequest request);

    /**
     * Finds a request by its unique reference.
     * Returns Optional.empty() when it does not exist.
     */
    Optional<ServiceRequest> findByReference(String reference);

    /**
     * Returns all stored requests.
     * Application services must filter results according
     * to the authenticated user's permissions.
     */
    List<ServiceRequest> findAll();
}