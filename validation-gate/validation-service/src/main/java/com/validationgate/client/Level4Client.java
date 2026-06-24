package com.validationgate.client;

import com.validationgate.dto.ExternalGateEventBatchRequest;
import com.validationgate.dto.ValidationRecordResponse;

public interface Level4Client {
    ValidationRecordResponse sendBatch(ExternalGateEventBatchRequest request);
}
