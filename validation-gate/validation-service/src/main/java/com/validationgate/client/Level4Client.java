package com.validationgate.client;

import com.validationgate.dto.SubmitBatchRequest;
import com.validationgate.dto.SubmitBatchResponse;

public interface Level4Client {
    SubmitBatchResponse sendBatch(SubmitBatchRequest request);
}
