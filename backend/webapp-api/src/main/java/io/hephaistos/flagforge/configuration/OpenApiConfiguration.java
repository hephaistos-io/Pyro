package io.hephaistos.flagforge.configuration;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAPI customizer to fix circular references in polymorphic type definitions.
 * <p>
 * When using sealed interfaces with Jackson's @JsonTypeInfo/@JsonSubTypes annotations,
 * springdoc-openapi generates schemas where subtypes use "allOf" to reference the parent type. This
 * creates circular references that TypeScript code generators cannot handle.
 * <p>
 * This customizer removes the parent reference from subtype schemas, flattening them into
 * standalone types while preserving the discriminated union pattern in the parent.
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * Template field subtypes that should not have allOf references to TemplateField.
     */
    private static final Set<String> TEMPLATE_FIELD_SUBTYPES =
            Set.of("StringTemplateField", "NumberTemplateField", "BooleanTemplateField",
                    "EnumTemplateField");

    @Bean
    public OpenApiCustomizer templateFieldSchemaCustomizer() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null) {
                return;
            }

            for (String subtypeName : TEMPLATE_FIELD_SUBTYPES) {
                Schema<?> schema = schemas.get(subtypeName);
                if (schema instanceof ComposedSchema composedSchema) {
                    flattenAllOfSchema(composedSchema, schemas);
                }
            }
        };
    }

    /**
     * Flattens a ComposedSchema that uses allOf with a parent reference. Removes the parent $ref
     * and merges properties from the inline schema.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void flattenAllOfSchema(ComposedSchema composedSchema, Map<String, Schema> allSchemas) {
        List<Schema> allOf = composedSchema.getAllOf();
        if (allOf == null || allOf.size() < 2) {
            return;
        }

        // Find the inline object schema (not the $ref to parent)
        Schema inlineSchema = null;
        for (Schema schema : allOf) {
            if (schema.get$ref() == null && schema.getProperties() != null) {
                inlineSchema = schema;
                break;
            }
        }

        if (inlineSchema == null) {
            return;
        }

        // Copy properties from inline schema to the composed schema
        composedSchema.setProperties(inlineSchema.getProperties());
        composedSchema.setRequired(inlineSchema.getRequired());

        // Clear allOf to make it a plain object schema
        composedSchema.setAllOf(null);
    }
}
