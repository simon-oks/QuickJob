package ro.oks.quickjob.common.geo;

import java.util.Random;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Fabrique unique pour les objets géométriques JTS.
 * Ordre PostGIS : longitude d'abord, latitude ensuite.
 */
public final class GeoUtils {

    private static final int SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID);

    private GeoUtils() {
    }

    public static Point point(double latitude, double longitude) {
        Point p = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        p.setSRID(SRID);
        return p;
    }

    public static double latitude(Point point) {
        return point.getY();
    }

    public static double longitude(Point point) {
        return point.getX();
    }

    /**
     * Retourne des coordonnees approximatives (bruitees de +-200m) pour l'affichage sur carte.
     * Deterministe : la meme mission produit toujours les memes coordonnees approximatives.
     *
     * @param latitude  latitude exacte
     * @param longitude longitude exacte
     * @param seed      identifiant utilise comme graine (typiquement l'UUID de la mission)
     * @return tableau [latitudeApprox, longitudeApprox]
     */
    public static double[] approximateCoordinates(double latitude, double longitude, UUID seed) {
        long seedValue = seed.getMostSignificantBits() ^ seed.getLeastSignificantBits();
        Random random = new Random(seedValue);
        // ~0.002 degre ≈ 200m a l'equateur
        double latNoise = (random.nextDouble() - 0.5) * 0.004;
        double lonNoise = (random.nextDouble() - 0.5) * 0.004;
        return new double[]{latitude + latNoise, longitude + lonNoise};
    }
}
