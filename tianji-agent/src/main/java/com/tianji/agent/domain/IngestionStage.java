package com.tianji.agent.domain;

public enum IngestionStage {
    QUEUED, VALIDATING, SPLITTING, EMBEDDING, INDEXING, ACTIVATING, COMPLETED
}
