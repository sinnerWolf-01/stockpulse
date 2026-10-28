package com.stockpulse.suggestion;

import jakarta.validation.constraints.NotNull;

public record SuggestionResolutionRequest(
        @NotNull Long expectedProductVersion,
        @NotNull SuggestionStatus status,
        String note
) { }