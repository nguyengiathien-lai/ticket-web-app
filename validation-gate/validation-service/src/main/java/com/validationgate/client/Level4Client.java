package com.validationgate.client;

import com.validationgate.dto.RecordRequestBatch;
import com.validationgate.dto.SubmitBatchResponse;

public interface Level4Client {
    SubmitBatchResponse sendBatch(RecordRequestBatch request);
}
