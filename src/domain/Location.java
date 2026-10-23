package domain;

/**
 * Stores an address and optional geographic coordinates.
 */
public final class Location implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String address;
    private final Double latitude;
    private final Double longitude;

    public Location(String address) {
        this(address, null, null);
    }

    public Location(
            String address,
            Double latitude,
            Double longitude) {

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(
                    "Address must not be blank.");
        }

        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException(
                    "Provide both latitude and longitude, or neither.");
        }

        if (latitude != null) {
            if (!Double.isFinite(latitude)
                    || latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException(
                        "Latitude must be between -90 and 90.");
            }

            if (!Double.isFinite(longitude)
                    || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException(
                        "Longitude must be between -180 and 180.");
            }
        }

        this.address = address.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}