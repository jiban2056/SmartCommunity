package application;

/**
 * Copies a local evidence file into application-managed storage.
 * Returns a portable reference relative to the evidence directory.
 */
public interface EvidenceStorage {

    String store(String sourcePath);
}