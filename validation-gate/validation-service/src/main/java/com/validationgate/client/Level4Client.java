package com.validationgate.client;

import com.validationgate.dto.RecordRequestBatch;
import com.validationgate.dto.ValidationRecordResponse;

public interface Level4Client {
    ValidationRecordResponse sendBatch(RecordRequestBatch request);
}
