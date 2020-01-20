package com.qdesrame.openapi.diff.output;

import com.qdesrame.openapi.diff.model.ChangedSchema;
import com.qdesrame.openapi.diff.model.schema.ChangedEnum;
import com.qdesrame.openapi.diff.model.schema.ChangedMaxLength;
import com.qdesrame.openapi.diff.model.schema.ChangedRequired;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

public class ContentChanges {
    private final ChangedSchema schema;
    private Map<String, String> newProperties = new HashMap<>();
    private Map<String, String> removedProperties = new HashMap<>();
    private Map<String, String> changedProperties = new HashMap<>();


    public ContentChanges(final ChangedSchema schema) {
        this.schema = schema;
    }

    private void calculateChanges(String prefix, final ChangedSchema schema) {
        if (schema == null || !schema.isDifferent()) {
            return;
        }

        if (!schema.getChangedProperties().isEmpty()) {
            schema.getChangedProperties().forEach((key, value) -> {
                calculateChanges(attribute(prefix, key, value.getType()), value);
            });
        }

        if (!schema.getIncreasedProperties().isEmpty()) {
            if (!schema.getIncreasedProperties().isEmpty()) {
                schema.getIncreasedProperties().forEach((key, value) -> {
                    newProperties.put(attribute(prefix, key, value.getType()),
                            Optional.ofNullable(value.getDescription()).map(s -> " - " + s).orElse(""));
                });
            }
        }

        if (!schema.getMissingProperties().isEmpty()) {
            if (!schema.getMissingProperties().isEmpty()) {
                schema.getMissingProperties().forEach((key, value) -> {
                    removedProperties.put(attribute(prefix, key, value.getType()), "");
                });
            }
        }

        if (schema.getEnumeration() != null) {
            enumerationChange(prefix, schema.getEnumeration());
        }

        if(schema.getMaxLength() != null && schema.getMaxLength().isDifferent()) {
            maxLengthChanged(prefix, schema.getMaxLength());
        }

        if(schema.getRequired() != null && schema.getRequired().isDifferent()) {
            requiredChanged(prefix, schema.getRequired());
        }

        calculateChanges(prefix, schema.getItems());
    }

    private void requiredChanged(final String attribute, final ChangedRequired changedRequired) {
        if(!changedRequired.getIncreased().isEmpty()) {
            changedProperties.put(attribute, " added required attribute " + String.join(", ", changedRequired.getIncreased()));
        }
        if(!changedRequired.getMissing().isEmpty()) {
            changedProperties.put(attribute, " removed required attribute " + String.join(", ", changedRequired.getMissing()));
        }
    }

    private void maxLengthChanged(final String attribute, final ChangedMaxLength maxLength) {
        changedProperties.put(attribute, String.format(" maxLength changed from %s to %s", maxLength.getOldValue(), maxLength.getNewValue()));
    }

    private String attribute(String prefix, String name, String type) {
        if ("array".equals(type)) {
            name = name + "[]";
        }
        if (prefix.length() == 0) {
            return name;
        }
        return prefix + "/" + name;
    }

    private void enumerationChange(String attribute,
            final ChangedEnum<?> enumeration) {

        if (!enumeration.isDifferent()) {
            return;
        }

        if (!enumeration.getIncreased().isEmpty()) {
            enumerationAddedProperties(attribute, enumeration.getIncreased());
        }
        if (!enumeration.getMissing().isEmpty()) {
            enumerationRemovedProperties(attribute, enumeration.getMissing());
        }
    }

    private String changeToString(String text, Map<String, String> properties, int indent) {
        StringBuilder sb = new StringBuilder();
        if (!properties.isEmpty()) {
            sb.append(StringUtils.repeat(' ', indent)).append(text).append(lineSeparator());
            properties.forEach((key, value) -> {
                sb.append(StringUtils.repeat(' ', indent + 2))
                        .append("- ")
                        .append(key)
                        .append(value)
                        .append(lineSeparator());
            });
        }

        return sb.toString();
    }

    private String toString(int indent) {
        String s = changeToString("Changes:", changedProperties, indent) +
                changeToString("New properties:", newProperties, indent) +
                changeToString("Removed properties:", removedProperties, indent);
        return s;
    }

    private void enumerationRemovedProperties(final String attribute, final List<?> removed) {
        changedProperties.put(attribute,
                " removed enum value " + removed
                        .stream()
                        .map(Object::toString)
                        .collect(joining(", ")));
    }

    private void enumerationAddedProperties(final String attribute, final List<?> increased) {
        changedProperties.put(attribute,
                " new enum value " + increased
                        .stream()
                        .map(Object::toString)
                        .collect(joining(", ")));
    }

    public String changes(final int indent) {
        calculateChanges("", schema);
        return toString(indent);
    }
}
