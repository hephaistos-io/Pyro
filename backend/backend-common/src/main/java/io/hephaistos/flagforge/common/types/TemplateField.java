package io.hephaistos.flagforge.common.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hephaistos.flagforge.common.enums.FieldType;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sealed interface for template field definitions.
 * <p>
 * Template fields define the structure of a template schema. Each field has a type
 * that determines its constraints and allowed values:
 * <ul>
 *   <li>STRING: Text values with minLength/maxLength constraints</li>
 *   <li>NUMBER: Numeric values with minValue/maxValue/incrementAmount constraints</li>
 *   <li>BOOLEAN: True/false values with no additional constraints</li>
 *   <li>ENUM: Selection from a predefined list of options</li>
 * </ul>
 * <p>
 * Polymorphic serialization uses the {@code type} field as discriminator.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = StringTemplateField.class, name = "STRING"),
        @JsonSubTypes.Type(value = NumberTemplateField.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = BooleanTemplateField.class, name = "BOOLEAN"),
        @JsonSubTypes.Type(value = EnumTemplateField.class, name = "ENUM")})
@Schema(oneOf = {StringTemplateField.class, NumberTemplateField.class, BooleanTemplateField.class,
        EnumTemplateField.class}, discriminatorProperty = "type", discriminatorMapping = {
        @DiscriminatorMapping(value = "STRING", schema = StringTemplateField.class),
        @DiscriminatorMapping(value = "NUMBER", schema = NumberTemplateField.class),
        @DiscriminatorMapping(value = "BOOLEAN", schema = BooleanTemplateField.class),
        @DiscriminatorMapping(value = "ENUM", schema = EnumTemplateField.class)})
public sealed interface TemplateField
        permits StringTemplateField, NumberTemplateField, BooleanTemplateField, EnumTemplateField {

    /**
     * @return Unique field identifier within the template
     */
    String key();

    /**
     * @return Field data type (discriminator for polymorphic serialization)
     */
    FieldType type();

    /**
     * @return Human-readable description of the field
     */
    String description();

    /**
     * @return Whether users can modify this field (relevant for USER templates)
     */
    boolean editable();
}
