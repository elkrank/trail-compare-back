package com.trailmatch.service.gpx;

import com.trailmatch.exception.ApiException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Streaming GPX parser dedicated to extracting usable track points only.
 *
 * <p>Invalid latitude/longitude values make a {@code trkpt} unusable; those
 * points are ignored so a partially valid GPX file can still be processed.</p>
 */
public class GpxParser {
    private static final String TRKPT = "trkpt";
    private static final String ELE = "ele";
    private static final String TIME = "time";

    public List<GpxTrackPoint> parse(InputStream inputStream) {
        XMLInputFactory factory = secureXmlInputFactory();
        List<GpxTrackPoint> points = new ArrayList<>();
        boolean hasUsableCoordinatePoint = false;

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.DTD) {
                        throw new XMLStreamException("DTD declarations are not allowed in GPX files");
                    }
                    if (event == XMLStreamConstants.START_ELEMENT && TRKPT.equals(reader.getLocalName())) {
                        ParsedTrackPoint parsedPoint = readTrackPoint(reader);
                        if (parsedPoint.hasUsableCoordinates()) {
                            hasUsableCoordinatePoint = true;
                        }
                        if (parsedPoint.trackPoint() != null) {
                            points.add(parsedPoint.trackPoint());
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException ex) {
            throw new ApiException(400, "invalid_gpx");
        }

        if (!hasUsableCoordinatePoint) {
            throw new ApiException(400, "gpx_no_usable_point");
        }
        if (points.isEmpty()) {
            throw new ApiException(400, "gpx_no_elevation_data");
        }

        return List.copyOf(points);
    }

    private XMLInputFactory secureXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        setPropertyIfSupported(factory, XMLInputFactory.SUPPORT_DTD, false);
        setPropertyIfSupported(factory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        setPropertyIfSupported(factory, XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        setPropertyIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setPropertyIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXMLResolver((publicId, systemId, baseURI, namespace) -> null);
        return factory;
    }

    private void setPropertyIfSupported(XMLInputFactory factory, String propertyName, Object value) {
        if (factory.isPropertySupported(propertyName)) {
            factory.setProperty(propertyName, value);
        }
    }

    private ParsedTrackPoint readTrackPoint(XMLStreamReader reader) throws XMLStreamException {
        Double latitude = parseCoordinate(reader.getAttributeValue(null, "lat"), -90.0, 90.0);
        Double longitude = parseCoordinate(reader.getAttributeValue(null, "lon"), -180.0, 180.0);
        Double elevationM = null;
        Instant time = null;
        int depth = 0;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
                if (depth == 1 && ELE.equals(reader.getLocalName())) {
                    elevationM = parseElevation(reader.getElementText());
                    depth--;
                } else if (depth == 1 && TIME.equals(reader.getLocalName())) {
                    time = parseTime(reader.getElementText());
                    depth--;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (depth == 0 && TRKPT.equals(reader.getLocalName())) {
                    break;
                }
                depth--;
            }
        }

        boolean hasUsableCoordinates = latitude != null && longitude != null;
        if (!hasUsableCoordinates || elevationM == null) {
            return new ParsedTrackPoint(hasUsableCoordinates, null);
        }

        return new ParsedTrackPoint(true, new GpxTrackPoint(latitude, longitude, elevationM, time));
    }

    private Double parseCoordinate(String rawValue, double min, double max) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            double value = Double.parseDouble(rawValue.trim());
            if (!Double.isFinite(value) || value < min || value > max) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double parseElevation(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            double elevation = Double.parseDouble(rawValue.trim());
            return Double.isFinite(elevation) ? elevation : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Instant parseTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(rawValue.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public record GpxTrackPoint(double latitude, double longitude, double elevationM, Instant time) {
    }

    private record ParsedTrackPoint(boolean hasUsableCoordinates, GpxTrackPoint trackPoint) {
    }
}
