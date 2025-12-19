package io.hephaistos.flagforge.controller.dto;

import java.util.List;

public record AllTemplateOverridesResponse(List<TemplateValuesResponse> userOverrides,
                                           List<TemplateValuesResponse> systemOverrides) {
}
