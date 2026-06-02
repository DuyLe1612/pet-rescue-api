package com.uit.petrescueapi.infrastructure.persistence.projection;

public interface PetStatsProjection {
         long getTotal();
         long getAvailable();
         long getPending();
         long getAdopted();
}
