package com.ticketapp.dto.external;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ExternalCardResponse {
    private String externalCardId;
    private String cardUid;
    private String maskedCardNumber;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}
