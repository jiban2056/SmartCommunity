package domain;

/**
 * Represents a council-defined service request category.
 */
public final class RequestCategory implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String categoryId;
    private final String name;

    public RequestCategory(String categoryId, String name) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException(
                    "Category ID must not be blank.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name must not be blank.");
        }

        this.categoryId = categoryId.trim();
        this.name = name.trim();
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }
}