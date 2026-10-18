package com.uit.petrescueapi.domain.valueobject;

/**
 * Lifecycle of the AI analysis of a post. The AI only suggests; facets used for search and the
 * Rescue Case workflow exist only after the author CONFIRMED (DA2: "AI chỉ đóng vai trò phân tích và đề xuất").
 */
public enum PostAnalysisStatus {
    PENDING,
    ANALYZED,
    CONFIRMED,
    REJECTED,
    FAILED;

    /** Confirmed / rejected posts carry a human decision and are never re-analysed. */
    public boolean isFinal() {
        return this == CONFIRMED || this == REJECTED;
    }
}
