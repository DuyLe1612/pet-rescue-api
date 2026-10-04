

## 15. Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-06-27 | Initial documentation generated from backend source. |
| 1.1.0 | 2026-07-17 | **DTO Separation by Role:** Created new Admin response DTOs (`PetAdminResponseDto`, `UserAdminResponseDto`, `OrganizationAdminResponseDto`) with all internal fields. Admin/org-owner endpoints now return these enhanced DTOs with additional fields (userCode, reputation, organizationCode, shelterId, caretaker info, etc.). **Endpoint Merging:** Unified Pet list endpoints (`GET /api/v1/pets` now supports `availableOnly`, `organizationId`, `userId` filters). Unified RescueCase list endpoints (`GET /api/v1/rescue-cases` now supports geo filters: `lat`, `lng`, `distance`, `minLat`, `minLng`, `maxLat`, `maxLng`). Deprecated `/available`, `/by-organization/{id}`, `/by-user/{id}` for pets and `/nearby`, `/map/bounding-box` for rescue cases. |

