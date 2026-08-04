package com.uit.petrescueapi.infrastructure.persistence.projection;

public interface AdoptionTrendProjection {
    Integer getMonth();
    Long getPending();
    Long getApproved();
    Long getRejected();
    Long getCompleted();
}
