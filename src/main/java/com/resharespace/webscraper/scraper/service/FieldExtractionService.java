package com.browzwi.webscraper.scraper.service;

import com.browzwi.webscraper.scraper.model.DataSourceType;
import com.browzwi.webscraper.scraper.model.FieldConfig;
import com.browzwi.webscraper.scraper.model.PageConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class FieldExtractionService {

    public Map<String, Object> extractFields(Document document, PageConfig pageConfig) {
        Map<String, Object> results = new HashMap<>();
        if (pageConfig == null || pageConfig.getFields() == null) {
            return results;
        }
        for (FieldConfig field : pageConfig.getFields()) {
            Object value = field.isMultiple()
                    ? extractMultiple(document, field)
                    : extractSingle(document, field);
            results.put(field.getName(), value);
        }
        return results;
    }

    private Object extractSingle(Document document, FieldConfig field) {
        for (String selector : field.getSelectors()) {
            Element element = document.select(selector).stream().findFirst().orElse(null);
            if (element != null) {
                String value = extractValue(element, field);
                if (value != null && !value.isBlank()) {
                    return applyPattern(value, field.getDataPattern());
                }
            }
        }
        return null;
    }

    private List<String> extractMultiple(Document document, FieldConfig field) {
        List<String> values = new ArrayList<>();
        for (String selector : field.getSelectors()) {
            document.select(selector).forEach(element -> {
                String value = extractValue(element, field);
                if (value != null && !value.isBlank()) {
                    values.add(applyPattern(value, field.getDataPattern()));
                }
            });
            if (!values.isEmpty()) {
                break;
            }
        }
        return values;
    }

    private String extractValue(Element element, FieldConfig field) {
        DataSourceType source = field.getSource() == null ? DataSourceType.TEXT : field.getSource();
        return switch (source) {
            case TEXT -> element.text();
            case HTML -> element.html();
            case ATTR -> element.attr(field.getAttributeName());
        };
    }

    private String applyPattern(String value, String dataPattern) {
        if (dataPattern == null || dataPattern.isBlank()) {
            return value;
        }
        Pattern pattern = Pattern.compile(dataPattern);
        var matcher = pattern.matcher(value);
        if (matcher.find()) {
            if (matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
            return matcher.group();
        }
        return value;
    }
}
