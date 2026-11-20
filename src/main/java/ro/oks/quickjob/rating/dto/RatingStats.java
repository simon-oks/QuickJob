package ro.oks.quickjob.rating.dto;

public record RatingStats(
        double averageScore,
        long totalRatings
) {
    public static RatingStats empty() {
        return new RatingStats(0.0, 0);
    }
}
