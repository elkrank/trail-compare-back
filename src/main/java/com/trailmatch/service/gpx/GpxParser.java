package com.trailmatch.service.gpx;

import com.trailmatch.exception.ApiException;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GpxParser {
    private static final String INVALID_GPX = "invalid_gpx";

    public GpxTrack parse(InputStream inputStream) {
        XMLStreamReader reader = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            configureSecureXmlInputFactory(factory);
            reader = factory.createXMLStreamReader(inputStream);

            List<GpxPoint> points = new ArrayList<>();
            CurrentPoint currentPoint = null;
            String currentPointChild = null;
            StringBuilder currentPointChildText = null;
            boolean rootSeen = false;
            boolean rootClosed = false;
            int trkDepth = 0;
            int trksegDepth = 0;
            int trkptDepth = 0;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    if (!rootSeen) {
                        if (!"gpx".equals(localName)) {
                            throw invalidGpx();
                        }
                        rootSeen = true;
                        continue;
                    }
                    if (rootClosed) {
                        throw invalidGpx();
                    }

                    if ("trk".equals(localName) && trkptDepth == 0) {
                        trkDepth++;
                    } else if ("trkseg".equals(localName) && trkDepth > 0 && trkptDepth == 0) {
                        trksegDepth++;
                    } else if ("trkpt".equals(localName) && trksegDepth > 0 && trkptDepth == 0) {
                        trkptDepth = 1;
                        currentPoint = new CurrentPoint(reader.getAttributeValue(null, "lat"), reader.getAttributeValue(null, "lon"));
                    } else if (trkptDepth > 0) {
                        trkptDepth++;
                        if (trkptDepth == 2 && ("ele".equals(localName) || "time".equals(localName))) {
                            currentPointChild = localName;
                            currentPointChildText = new StringBuilder();
                        }
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (currentPointChildText != null) {
                        currentPointChildText.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();
                    if (!rootSeen) {
                        throw invalidGpx();
                    }

                    if (currentPointChild != null && trkptDepth == 2 && currentPointChild.equals(localName)) {
                        if ("ele".equals(currentPointChild)) {
                            currentPoint.elevationText = currentPointChildText.toString();
                        } else {
                            currentPoint.timeText = currentPointChildText.toString();
                        }
                        currentPointChild = null;
                        currentPointChildText = null;
                    }

                    if (trkptDepth > 0) {
                        if (trkptDepth == 1 && "trkpt".equals(localName)) {
                            parsePoint(currentPoint).ifPresent(points::add);
                            currentPoint = null;
                            trkptDepth = 0;
                        } else {
                            trkptDepth--;
                        }
                    } else if ("trkseg".equals(localName) && trksegDepth > 0) {
                        trksegDepth--;
                    } else if ("trk".equals(localName) && trkDepth > 0) {
                        trkDepth--;
                    } else if ("gpx".equals(localName)) {
                        rootClosed = true;
                    }
                }
            }

            if (!rootSeen || !rootClosed) {
                throw invalidGpx();
            }
            return new GpxTrack(points);
        } catch (ApiException ex) {
            throw ex;
        } catch (XMLStreamException ex) {
            throw invalidGpx();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                    // Nothing useful can be done while returning a parsed track or an invalid GPX response.
                }
            }
        }
    }

    private void configureSecureXmlInputFactory(XMLInputFactory factory) {
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    }

    private Optional<GpxPoint> parsePoint(CurrentPoint point) {
        try {
            double latitude = Double.parseDouble(point.latitudeText);
            double longitude = Double.parseDouble(point.longitudeText);
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return Optional.empty();
            }

            Double elevation = optionalDouble(point.elevationText);
            Instant time = optionalInstant(point.timeText);
            return Optional.of(new GpxPoint(latitude, longitude, elevation, time));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private ApiException invalidGpx() {
        return new ApiException(400, INVALID_GPX);
    }

    private Double optionalDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value.trim());
    }

    private Instant optionalInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static final class CurrentPoint {
        private final String latitudeText;
        private final String longitudeText;
        private String elevationText;
        private String timeText;

        private CurrentPoint(String latitudeText, String longitudeText) {
            this.latitudeText = latitudeText == null ? "" : latitudeText;
            this.longitudeText = longitudeText == null ? "" : longitudeText;
        }
    }
}
