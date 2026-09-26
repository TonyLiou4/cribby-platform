package com.santosh.roommate_marketplace_api.message.dto;

import jakarta.validation.constraints.NotNull;

public record StartConversationRequest(
        @NotNull
        Long listingId
) {
}
