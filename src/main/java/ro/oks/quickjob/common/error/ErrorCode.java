package ro.oks.quickjob.common.error;

public final class ErrorCode {

    private ErrorCode() {
    }

    // Mission
    public static final String MISSION_NOT_FOUND = "MISSION_NOT_FOUND";
    public static final String MISSION_INVALID_DURATION = "MISSION_INVALID_DURATION";
    public static final String MISSION_INVALID_START_TIME = "MISSION_INVALID_START_TIME";
    public static final String MISSION_OUTSIDE_SERVICE_ZONE = "MISSION_OUTSIDE_SERVICE_ZONE";
    public static final String MISSION_INVALID_REMUNERATION = "MISSION_INVALID_REMUNERATION";
    public static final String MISSION_INVALID_TRANSITION = "MISSION_INVALID_TRANSITION";
    public static final String MISSION_INVALID_REQUIRED_WORKERS = "MISSION_INVALID_REQUIRED_WORKERS";
    public static final String MISSION_WORKER_ALREADY_ASSIGNED = "MISSION_WORKER_ALREADY_ASSIGNED";
    public static final String MISSION_FULLY_ASSIGNED = "MISSION_FULLY_ASSIGNED";
    public static final String MISSION_INVALID_NOTIFICATION_RADIUS = "MISSION_INVALID_NOTIFICATION_RADIUS";
    public static final String MISSION_INCOMPLETE_DELIVERY_COORDS = "MISSION_INCOMPLETE_DELIVERY_COORDS";

    // User
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_PHONE_NOT_VERIFIED = "USER_PHONE_NOT_VERIFIED";

    // Auth
    public static final String AUTH_INVALID_TOKEN = "AUTH_INVALID_TOKEN";
    public static final String AUTH_INVALID_OTP = "AUTH_INVALID_OTP";
    public static final String AUTH_USER_NOT_REGISTERED = "AUTH_USER_NOT_REGISTERED";
    public static final String AUTH_PHONE_ALREADY_REGISTERED = "AUTH_PHONE_ALREADY_REGISTERED";
    public static final String AUTH_EMAIL_ALREADY_REGISTERED = "AUTH_EMAIL_ALREADY_REGISTERED";
    public static final String AUTH_IDENTIFIER_REQUIRED = "AUTH_IDENTIFIER_REQUIRED";
    public static final String AUTH_ACCESS_DENIED = "AUTH_ACCESS_DENIED";

    // Application
    public static final String APPLICATION_NOT_FOUND = "APPLICATION_NOT_FOUND";
    public static final String APPLICATION_MISSION_NOT_PUBLISHED = "APPLICATION_MISSION_NOT_PUBLISHED";
    public static final String APPLICATION_OWN_MISSION = "APPLICATION_OWN_MISSION";
    public static final String APPLICATION_ALREADY_EXISTS = "APPLICATION_ALREADY_EXISTS";
    public static final String APPLICATION_ACCESS_DENIED = "APPLICATION_ACCESS_DENIED";
    public static final String APPLICATION_INVALID_TRANSITION = "APPLICATION_INVALID_TRANSITION";

    // Validation
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    // Notification
    public static final String NOTIFICATION_NOT_FOUND = "NOTIFICATION_NOT_FOUND";

    // Rating
    public static final String RATING_WINDOW_EXPIRED = "RATING_WINDOW_EXPIRED";
    public static final String RATING_MISSION_NOT_COMPLETED = "RATING_MISSION_NOT_COMPLETED";
    public static final String RATING_ALREADY_EXISTS = "RATING_ALREADY_EXISTS";
    public static final String RATING_UNAUTHORIZED = "RATING_UNAUTHORIZED";
    public static final String RATING_SELF_RATING = "RATING_SELF_RATING";

    // Concurrence
    public static final String CONFLICT = "CONFLICT";
    public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
}
