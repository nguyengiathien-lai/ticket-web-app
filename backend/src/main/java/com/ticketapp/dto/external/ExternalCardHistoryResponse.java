package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExternalCardHistoryResponse {

    @JsonAlias({"id", "cardID", "externalCardId"})
    private String cardId;

    private String cardUid;
    private String status;
    private String type;
    private LocalDateTime activatedAt;
    private LocalDateTime linkedAt;
}
