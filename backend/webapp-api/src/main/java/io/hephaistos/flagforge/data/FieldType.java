package io.hephaistos.flagforge.data;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the data type of a template field.
 */
@Schema(enumAsRef = true)
public enum FieldType {
    STRING,
    NUMBER,
    BOOLEAN,
    ENUM
}
