package com.ticketapp.service;

import com.ticketapp.entity.AuditLog;
import com.ticketapp.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditServiceTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsAuthenticationActivityWithRequestMetadata() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditService(repository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("User-Agent", "test-browser");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.recordAuthenticationActivity(
                "user-1",
                "user@example.com",
                AuditService.LOGIN_SUCCESS,
                "Credentials authenticated successfully");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getAccountId()).isEqualTo("user-1");
        assertThat(saved.getAction()).isEqualTo(AuditService.LOGIN_SUCCESS);
        assertThat(saved.getResourceType()).isEqualTo("ACCOUNT");
        assertThat(saved.getIpAddress()).isEqualTo("192.0.2.10");
        assertThat(saved.getUserAgent()).isEqualTo("test-browser");
    }

    @Test
    void failedUnknownLoginUsesNormalizedEmailAsResourceIdentifier() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditService(repository);

        service.recordAuthenticationActivity(
                null,
                "  USER@Example.COM ",
                AuditService.LOGIN_FAILURE,
                "Account was not found or is inactive");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getResourceId()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getAccountId()).isNull();
    }
}
