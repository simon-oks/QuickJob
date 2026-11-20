package ro.oks.quickjob.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import ro.oks.quickjob.common.geo.GeoUtils;

class GeoUtilsTest {

    @Test
    void point_stores_coordinates_correctly() {
        double lat = 6.1319;
        double lon = 1.2228;

        Point point = GeoUtils.point(lat, lon);

        assertThat(GeoUtils.latitude(point)).isEqualTo(lat);
        assertThat(GeoUtils.longitude(point)).isEqualTo(lon);
        assertThat(point.getSRID()).isEqualTo(4326);
    }

    @Test
    void point_does_not_swap_lat_lon() {
        Point point = GeoUtils.point(6.0, 1.0);
        // JTS : X = longitude, Y = latitude
        assertThat(point.getX()).isEqualTo(1.0);
        assertThat(point.getY()).isEqualTo(6.0);
    }
}
