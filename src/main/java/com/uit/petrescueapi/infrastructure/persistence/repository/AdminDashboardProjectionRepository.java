package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PetJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AdminDashboardProjectionRepository extends Repository<PetJpaEntity, UUID> {

    /**
     * Each table is aggregated on its own: joining them multiplies the rows.
     * Totals and open work are as at the end of the period. No status history is kept, so the
     * previous value of a status count is today's status of what existed back then, not a snapshot.
     * Completed adoptions are the ones completed during the period.
     */
    @Query(value = """
        WITH o AS (
            SELECT COUNT(*) FILTER (WHERE created_at < :endDate) AS total,
                   COUNT(*) FILTER (WHERE created_at < :endDate AND status = 'PENDING') AS pending,
                   COUNT(*) FILTER (WHERE created_at < :prevEndDate) AS prev_total,
                   COUNT(*) FILTER (WHERE created_at < :prevEndDate AND status = 'PENDING') AS prev_pending
            FROM organizations WHERE is_deleted = false
        ), p AS (
            SELECT COUNT(*) FILTER (WHERE created_at < :endDate) AS total,
                   COUNT(*) FILTER (WHERE created_at < :prevEndDate) AS prev_total
            FROM pets WHERE is_deleted = false
        ), rc AS (
            SELECT COUNT(*) FILTER (WHERE created_at < :endDate) AS open,
                   COUNT(*) FILTER (WHERE created_at < :prevEndDate) AS prev_open
            FROM rescue_cases WHERE is_deleted = false AND status IN ('REPORTED', 'IN_PROGRESS')
        ), aa AS (
            SELECT COUNT(*) FILTER (WHERE status = 'PENDING' AND created_at < :endDate) AS pending,
                   COUNT(*) FILTER (WHERE status = 'PENDING' AND created_at < :prevEndDate) AS prev_pending,
                   COUNT(*) FILTER (WHERE status = 'COMPLETED' AND COALESCE(updated_at, created_at) >= :startDate
                                      AND COALESCE(updated_at, created_at) < :endDate) AS completed,
                   COUNT(*) FILTER (WHERE status = 'COMPLETED' AND COALESCE(updated_at, created_at) >= :prevStartDate
                                      AND COALESCE(updated_at, created_at) < :prevEndDate) AS prev_completed
            FROM adoption_applications WHERE is_deleted = false
        )
        SELECT o.total AS totalOrganizations,
               o.pending AS pendingOrganizations,
               p.total AS totalPets,
               rc.open AS openRescueCases,
               aa.pending AS pendingAdoptions,
               aa.completed AS completedAdoptions,
               o.prev_total AS prevTotalOrganizations,
               o.prev_pending AS prevPendingOrganizations,
               p.prev_total AS prevTotalPets,
               rc.prev_open AS prevOpenRescueCases,
               aa.prev_pending AS prevPendingAdoptions,
               aa.prev_completed AS prevCompletedAdoptions
        FROM o, p, rc, aa
    """, nativeQuery = true)
    DashboardProjection getDashboardOverview(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("prevStartDate") LocalDateTime prevStartDate,
            @Param("prevEndDate") LocalDateTime prevEndDate);

    @Query(value = """
        SELECT 
            EXTRACT(MONTH FROM created_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS month,
            COUNT(CASE WHEN status = 'PENDING' THEN 1 END) AS pending,
            COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) AS approved,
            COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) AS rejected,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed
        FROM adoption_applications
        WHERE is_deleted = false AND created_at >= :yearStart AND created_at < :yearEnd
        GROUP BY EXTRACT(MONTH FROM created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')
        ORDER BY month
    """, nativeQuery = true)
    List<AdoptionTrendProjection> getAdoptionTrend(
            @Param("yearStart") LocalDateTime yearStart,
            @Param("yearEnd") LocalDateTime yearEnd);

    @Query(value = """
        SELECT status AS status, COUNT(*) AS count
        FROM rescue_cases
        WHERE is_deleted = false AND created_at >= :startDate AND created_at < :endDate
        GROUP BY status
    """, nativeQuery = true)
    List<StatusSummaryProjection> getRescueSummaryByStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT priority AS priority, COUNT(*) AS count
        FROM rescue_cases
        WHERE is_deleted = false AND created_at >= :startDate AND created_at < :endDate
        GROUP BY priority
    """, nativeQuery = true)
    List<PrioritySummaryProjection> getRescueSummaryByPriority(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Pets and open work as at the end of the month, adoptions completed during it. */
    @Query(value = """
        SELECT
            o.organization_id AS organizationId,
            o.name AS organizationName,
            CAST(o.status AS VARCHAR) AS status,
            COALESCE(p.total, 0) AS totalPets,
            COALESCE(rc.open, 0) AS openRescueCases,
            COALESCE(aa.pending, 0) AS pendingAdoptions,
            COALESCE(aa.completed, 0) AS completedAdoptions,
            0 AS healthAlerts
        FROM organizations o
        LEFT JOIN (
            SELECT shelter_id, COUNT(*) AS total FROM pets
            WHERE is_deleted = false AND created_at < :endDate
            GROUP BY shelter_id
        ) p ON p.shelter_id = o.organization_id
        LEFT JOIN (
            SELECT organization_id, COUNT(*) AS open FROM rescue_cases
            WHERE is_deleted = false AND status IN ('REPORTED', 'IN_PROGRESS') AND created_at < :endDate
            GROUP BY organization_id
        ) rc ON rc.organization_id = o.organization_id
        LEFT JOIN (
            SELECT organization_id,
                   COUNT(*) FILTER (WHERE status = 'PENDING' AND created_at < :endDate) AS pending,
                   COUNT(*) FILTER (WHERE status = 'COMPLETED' AND COALESCE(updated_at, created_at) >= :startDate
                                      AND COALESCE(updated_at, created_at) < :endDate) AS completed
            FROM adoption_applications
            WHERE is_deleted = false
            GROUP BY organization_id
        ) aa ON aa.organization_id = o.organization_id
        WHERE o.is_deleted = false AND o.created_at < :endDate
        ORDER BY totalPets DESC, organizationName
        LIMIT :limit
    """, nativeQuery = true)
    List<TopOrganizationProjection> getTopOrganizations(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit);

    @Query(value = """
        SELECT 
            CAST(o.organization_id AS VARCHAR) AS taskId,
            'ORGANIZATION_APPROVAL' AS type,
            CONCAT('Duyệt tổ chức ', o.name) AS title,
            o.organization_code AS referenceId,
            CAST(o.status AS VARCHAR) AS status,
            'HIGH' AS priority,
            o.created_at AS createdAt,
            CONCAT('/quan-ly-trung-tam/', o.organization_id) AS actionUrl
        FROM organizations o
        WHERE o.is_deleted = false AND o.status = 'PENDING' AND o.created_at < :endDate
        UNION ALL
        SELECT 
            CAST(aa.application_id AS VARCHAR) AS taskId,
            'ADOPTION_REVIEW' AS type,
            CONCAT('Đơn nhận nuôi ', COALESCE(p.name, '')) AS title,
            aa.adoption_code AS referenceId,
            CAST(aa.status AS VARCHAR) AS status,
            'MEDIUM' AS priority,
            aa.created_at AS createdAt,
            CONCAT('/quan-ly-nhan-nuoi/', aa.application_id) AS actionUrl
        FROM adoption_applications aa
        LEFT JOIN pets p ON p.pet_id = aa.pet_id
        WHERE aa.is_deleted = false AND aa.status = 'PENDING' AND aa.created_at < :endDate
        ORDER BY createdAt ASC
        LIMIT 100
    """, nativeQuery = true)
    List<PendingTaskProjection> getPendingTasks(@Param("endDate") LocalDateTime endDate);
}
