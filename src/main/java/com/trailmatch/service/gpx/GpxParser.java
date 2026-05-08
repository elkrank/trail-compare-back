package com.trailmatch.service.gpx;

import com.trailmatch.exception.ApiException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GpxParser {
    public GpxTrack parse(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(inputStream);
            Element root = document.getDocumentElement();
            if (root == null || !"gpx".equals(localName(root))) {
                throw new ApiException(400, "invalid_gpx");
            }

            NodeList elements = root.getElementsByTagName("*");
            List<GpxPoint> points = new ArrayList<>();
            for (int i = 0; i < elements.getLength(); i++) {
                Node node = elements.item(i);
                if (node instanceof Element trkpt && "trkpt".equals(localName(trkpt))) {
                    parsePoint(trkpt).ifPresent(points::add);
                }
            }
            return new GpxTrack(points);
        } catch (ApiException ex) {
            throw ex;
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new ApiException(400, "invalid_gpx");
        }
    }

    private java.util.Optional<GpxPoint> parsePoint(Element trkpt) {
        try {
            double latitude = Double.parseDouble(trkpt.getAttribute("lat"));
            double longitude = Double.parseDouble(trkpt.getAttribute("lon"));
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                return java.util.Optional.empty();
            }

            Double elevation = optionalDouble(childText(trkpt, "ele"));
            Instant time = optionalInstant(childText(trkpt, "time"));
            return java.util.Optional.of(new GpxPoint(latitude, longitude, elevation, time));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }

    private String childText(Element parent, String targetLocalName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element child && targetLocalName.equals(localName(child))) {
                return child.getTextContent();
            }
        }
        return null;
    }

    private String localName(Element element) {
        String localName = element.getLocalName();
        return localName != null ? localName : element.getNodeName();
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
}
