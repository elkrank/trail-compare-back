package com.trailmatch.service;

import com.trailmatch.dto.RaceGpxUploadResponse;
import com.trailmatch.entity.Race;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.RaceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RaceGpxService {
    private static final int MAX_PROFILE_POINTS = 500;
    private static final double MIN_ELEVATION_DELTA_M = 3.0;
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final RaceRepository raceRepository;

    @Transactional
    public RaceGpxUploadResponse importGpx(Long raceId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "gpx_file_required");
        }

        String fileName = file.getOriginalFilename();
        try {
            return importGpx(raceId, file.getInputStream(), fileName);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, "invalid_gpx_file");
        }
    }

    @Transactional
    public RaceGpxUploadResponse importGpx(Long raceId, InputStream gpxInputStream, String fileName) {
        Race race = raceRepository.findById(raceId).orElseThrow(() -> new ApiException(404, "race_not_found"));
        List<GpxPoint> rawPoints = parsePoints(gpxInputStream);
        if (rawPoints.isEmpty()) {
            throw new ApiException(400, "gpx_points_required");
        }

        GpxMetrics metrics = calculateMetrics(rawPoints);
        int profilePointsCount = downsample(rawPoints, MAX_PROFILE_POINTS).size();

        race.setDistanceKm(metrics.distanceKm());
        race.setElevationGainM(metrics.elevationGainM());
        race.setElevationLossM(metrics.elevationLossM());
        race.setMinElevationM(metrics.minElevationM());
        race.setMaxElevationM(metrics.maxElevationM());
        Race saved = raceRepository.save(race);

        return new RaceGpxUploadResponse(
                saved.getId(),
                fileName,
                profilePointsCount,
                saved.getDistanceKm(),
                saved.getElevationGainM(),
                saved.getElevationLossM(),
                saved.getMinElevationM(),
                saved.getMaxElevationM()
        );
    }

    private List<GpxPoint> parsePoints(InputStream gpxInputStream) {
        if (gpxInputStream == null) {
            throw new ApiException(400, "gpx_file_required");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(gpxInputStream);
            document.getDocumentElement().normalize();

            List<GpxPoint> points = new ArrayList<>();
            addPoints(points, document.getElementsByTagNameNS("*", "trkpt"));
            if (points.isEmpty()) {
                addPoints(points, document.getElementsByTagNameNS("*", "rtept"));
            }
            if (points.isEmpty()) {
                addPoints(points, document.getElementsByTagNameNS("*", "wpt"));
            }
            return points;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, "invalid_gpx_file");
        }
    }

    private void addPoints(List<GpxPoint> points, NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            double latitude = Double.parseDouble(element.getAttribute("lat"));
            double longitude = Double.parseDouble(element.getAttribute("lon"));
            Double elevation = readElevation(element);
            points.add(new GpxPoint(latitude, longitude, elevation));
        }
    }

    private Double readElevation(Element pointElement) {
        NodeList elevationNodes = pointElement.getElementsByTagNameNS("*", "ele");
        if (elevationNodes.getLength() == 0) {
            return null;
        }
        return Double.parseDouble(elevationNodes.item(0).getTextContent());
    }

    private GpxMetrics calculateMetrics(List<GpxPoint> points) {
        double distanceKm = 0.0;
        double elevationGainM = 0.0;
        double elevationLossM = 0.0;
        Double minElevationM = null;
        Double maxElevationM = null;

        GpxPoint previous = null;
        for (GpxPoint point : points) {
            if (previous != null) {
                distanceKm += haversineDistanceKm(previous, point);
                if (previous.elevationM() != null && point.elevationM() != null) {
                    double delta = point.elevationM() - previous.elevationM();
                    if (delta >= MIN_ELEVATION_DELTA_M) {
                        elevationGainM += delta;
                    } else if (delta <= -MIN_ELEVATION_DELTA_M) {
                        elevationLossM += Math.abs(delta);
                    }
                }
            }

            if (point.elevationM() != null) {
                minElevationM = minElevationM == null ? point.elevationM() : Math.min(minElevationM, point.elevationM());
                maxElevationM = maxElevationM == null ? point.elevationM() : Math.max(maxElevationM, point.elevationM());
            }
            previous = point;
        }

        return new GpxMetrics(
                round(distanceKm, 2),
                (int) Math.round(elevationGainM),
                (int) Math.round(elevationLossM),
                minElevationM == null ? null : (int) Math.round(minElevationM),
                maxElevationM == null ? null : (int) Math.round(maxElevationM)
        );
    }

    private List<GpxPoint> downsample(List<GpxPoint> points, int maxPoints) {
        if (points.size() <= maxPoints) {
            return points;
        }

        List<GpxPoint> sampled = new ArrayList<>(maxPoints);
        for (int i = 0; i < maxPoints; i++) {
            int sourceIndex = (int) Math.round(i * (points.size() - 1.0) / (maxPoints - 1.0));
            sampled.add(points.get(sourceIndex));
        }
        return sampled;
    }

    private double haversineDistanceKm(GpxPoint first, GpxPoint second) {
        double lat1 = Math.toRadians(first.latitude());
        double lat2 = Math.toRadians(second.latitude());
        double deltaLat = Math.toRadians(second.latitude() - first.latitude());
        double deltaLon = Math.toRadians(second.longitude() - first.longitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    private record GpxPoint(double latitude, double longitude, Double elevationM) {}

    private record GpxMetrics(Double distanceKm, Integer elevationGainM, Integer elevationLossM,
                              Integer minElevationM, Integer maxElevationM) {}
}
