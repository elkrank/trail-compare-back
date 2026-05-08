package com.trailmatch.service.gpx;

import com.trailmatch.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GpxParserTest {
    private final GpxParser parser = new GpxParser();

    @Test
    void parsesValidGpxTrackWithElevationAndTime() throws Exception {
        try (InputStream gpx = getClass().getResourceAsStream("/gpx/valid-track.gpx")) {
            assertNotNull(gpx);

            GpxTrack track = parser.parse(gpx);

            assertEquals(2, track.points().size());
            GpxPoint first = track.points().getFirst();
            assertEquals(45.0, first.latitude());
            assertEquals(6.0, first.longitude());
            assertEquals(1000.0, first.elevationM());
            assertEquals(Instant.parse("2026-05-08T08:00:00Z"), first.time());
        }
    }

    @Test
    void parsesEmptyGpxAsEmptyTrack() {
        GpxTrack track = parser.parse(xml("<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\"/>"));

        assertTrue(track.points().isEmpty());
    }

    @Test
    void rejectsMalformedXml() {
        ApiException exception = assertThrows(ApiException.class, () -> parser.parse(xml("<gpx><trk>")));

        assertEquals(400, exception.status);
        assertEquals("invalid_gpx", exception.getMessage());
    }

    @Test
    void rejectsNonGpxRootElement() {
        ApiException exception = assertThrows(ApiException.class, () -> parser.parse(xml("<not-gpx/>")));

        assertEquals(400, exception.status);
        assertEquals("invalid_gpx", exception.getMessage());
    }

    @Test
    void ignoresTrackPointsOutsideTrackSegments() {
        GpxTrack track = parser.parse(xml("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <trkpt lat="1.0" lon="2.0"><ele>10</ele></trkpt>
                  <trk><trkseg><trkpt lat="45.0" lon="6.0"><ele>1000</ele></trkpt></trkseg></trk>
                </gpx>
                """));

        assertEquals(1, track.points().size());
        assertEquals(45.0, track.points().getFirst().latitude());
        assertEquals(6.0, track.points().getFirst().longitude());
        assertEquals(1000.0, track.points().getFirst().elevationM());
    }

    @Test
    void doesNotExpandExternalEntities(@TempDir Path tempDir) throws Exception {
        Path externalEntity = tempDir.resolve("external-entity.txt");
        Files.writeString(externalEntity, "1234.5", StandardCharsets.UTF_8);

        GpxTrack track = parser.parse(xml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE gpx [<!ENTITY xxe SYSTEM "%s">]>
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><trkseg><trkpt lat="45.0" lon="6.0"><ele>&xxe;</ele></trkpt></trkseg></trk>
                </gpx>
                """.formatted(externalEntity.toUri())));

        assertEquals(1, track.points().size());
        assertNull(track.points().getFirst().elevationM());
    }

    @Test
    void parsesTrackPointWithoutElevation() {
        GpxTrack track = parser.parse(xml("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><trkseg><trkpt lat="45.0" lon="6.0"><time>2026-05-08T08:00:00Z</time></trkpt></trkseg></trk>
                </gpx>
                """));

        assertEquals(1, track.points().size());
        assertNull(track.points().getFirst().elevationM());
        assertEquals(Instant.parse("2026-05-08T08:00:00Z"), track.points().getFirst().time());
    }

    @Test
    void ignoresTrackPointsWithoutUsableCoordinates() {
        GpxTrack track = parser.parse(xml("""
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><trkseg>
                    <trkpt><ele>1000</ele></trkpt>
                    <trkpt lat="95.0" lon="6.0"><ele>1000</ele></trkpt>
                    <trkpt lat="45.0" lon="invalid"><ele>1000</ele></trkpt>
                  </trkseg></trk>
                </gpx>
                """));

        assertTrue(track.points().isEmpty());
    }

    private InputStream xml(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
