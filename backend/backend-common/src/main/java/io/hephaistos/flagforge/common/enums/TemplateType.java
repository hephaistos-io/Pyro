package io.hephaistos.flagforge.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the type of template.
 *
 * <ul>
 *   <li>USER: Defines user attributes with editable flags for settings UI
 *   <li>SYSTEM: Static configurations per identifier (rate limits, feature configs)
 * </ul>
 */
@Schema(enumAsRef = true)
public enum TemplateType {
    USER,
    SYSTEM
}
