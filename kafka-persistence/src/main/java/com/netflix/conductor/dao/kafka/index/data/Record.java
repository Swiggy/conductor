package com.netflix.conductor.dao.kafka.index.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Record {
    @JsonProperty
    String documentType;
    @JsonProperty
    Object payload;

    public String getDocumentType() {
        return this.documentType;
    }

    public Object getPayload() {
        return this.payload;
    }

    public Record(String documentType, Object payload) {
        this.documentType = documentType;
        this.payload = payload;
    }
}