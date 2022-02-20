package parser;

public class Position {

    private String latitude;
    private String latHemisphere;
    private String longitude;
    private String lonHemisphere;

    public Position(String latitude, String latHemisphere, String longitude, String lonHemisphere) {
        this.latitude = latitude;
        this.latHemisphere = latHemisphere;
        this.longitude = longitude;
        this.lonHemisphere = lonHemisphere;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatHemisphere() {
        return latHemisphere;
    }

    public void setLatHemisphere(String latHemisphere) {
        this.latHemisphere = latHemisphere;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLonHemisphere() {
        return lonHemisphere;
    }

    public void setLonHemisphere(String lonHemisphere) {
        this.lonHemisphere = lonHemisphere;
    }
}
