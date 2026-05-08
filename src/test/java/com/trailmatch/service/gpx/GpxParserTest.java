package com.trailmatch.service.gpx;

import com.trailmatch.exception.ApiException;
import com.trailmatch.service.gpx.GpxParser.GpxTrackPoint;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpxParserTest {
    private final GpxParser parser = new GpxParser();

    @Test
    void parsesOnlyTrackPointsWithElevationAndOptionalTime() {
        List<GpxTrackPoint> points = parser.parse(inputStream("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
                  <wpt lat="1.0" lon="2.0"><ele>999</ele></wpt>
                  <trk><name>route</name><trkseg>
                    <trkpt lat="45.123" lon="6.456">
                      <ele>1234.5</ele>
                      <time>2026-05-08T10:15:30Z</time>
                    </trkpt>
                    <trkpt lat="45.124" lon="6.457"><ele>1236</ele></trkpt>
                  </trkseg></trk>
                </gpx>
                """));

        assertEquals(2, points.size());
        assertEquals(45.123, points.getFirst().latitude());
        assertEquals(6.456, points.getFirst().longitude());
        assertEquals(1234.5, points.getFirst().elevationM());
        assertEquals(Instant.parse("2026-05-08T10:15:30Z"), points.getFirst().time());
        assertNull(points.get(1).time());
    }

    @Test
    void malformedXmlReturnsInvalidGpx() {
        ApiException exception = assertThrows(ApiException.class,
                () -> parser.parse(inputStream("<gpx><trk><trkseg><trkpt lat=\"45\" lon=\"6\"><ele>10</ele>")));

        assertEquals(400, exception.status);
        assertEquals("invalid_gpx", exception.getMessage());
    }

    @Test
    void gpxWithoutUsableTrackPointReturnsDedicatedError() {
        ApiException exception = assertThrows(ApiException.class, () -> parser.parse(inputStream("""
                <gpx>
                  <trk><trkseg>
                    <trkpt lat="91" lon="6"><ele>10</ele></trkpt>
                    <trkpt lat="45" lon="181"><ele>11</ele></trkpt>
                    <trkpt lon="6"><ele>12</ele></trkpt>
                  </trkseg></trk>
                </gpx>
                """)));

        assertEquals(400, exception.status);
        assertEquals("gpx_no_usable_point", exception.getMessage());
    }

    @Test
    void usableTrackPointsWithoutElevationReturnDedicatedError() {
        ApiException exception = assertThrows(ApiException.class, () -> parser.parse(inputStream("""
                <gpx><trk><trkseg>
                  <trkpt lat="45" lon="6" />
                  <trkpt lat="45.1" lon="6.1"><ele>not-a-number</ele></trkpt>
                </trkseg></trk></gpx>
                """)));

        assertEquals(400, exception.status);
        assertEquals("gpx_no_elevation_data", exception.getMessage());
    }

    @Test
    void invalidLatitudeOrLongitudeTrackPointsAreIgnored() {
        List<GpxTrackPoint> points = parser.parse(inputStream("""
                <gpx><trk><trkseg>
                  <trkpt lat="NaN" lon="6"><ele>10</ele></trkpt>
                  <trkpt lat="45" lon="6"><ele>20</ele></trkpt>
                </trkseg></trk></gpx>
                """));

        assertEquals(1, points.size());
        assertEquals(45.0, points.getFirst().latitude());
        assertEquals(6.0, points.getFirst().longitude());
        assertEquals(20.0, points.getFirst().elevationM());
    }

    @Test
    void dtdAndExternalEntityResolutionAreDisabled() {
        ApiException exception = assertThrows(ApiException.class, () -> parser.parse(inputStream("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE gpx [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <gpx><trk><trkseg><trkpt lat="45" lon="6"><ele>&xxe;</ele></trkpt></trkseg></trk></gpx>
                """)));

        assertEquals(400, exception.status);
        assertEquals("invalid_gpx", exception.getMessage());
    }

    private ByteArrayInputStream inputStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
