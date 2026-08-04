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

    @Query(value = """
        SELECT 
            COUNT(DISTINCT o.organization_id) AS totalOrganizations,
            COUNT(DISTINCT CASE WHEN o.status = 'PENDING' THEN o.organization_id END) AS pendingOrganizations,
            COUNT(DISTINCT p.pet_id) AS totalPets,
            COUNT(DISTINCT CASE WHEN rc.status = 'IN_PROGRESS' THEN rc.case_id END) AS openRescueCases,
            COUNT(DISTINCT CASE WHEN aa.status = 'PENDING' THEN aa.application_id END) AS pendingAdoptions,
            COUNT(DISTINCT CASE WHEN aa.status = 'COMPLETED' THEN aa.application_id END) AS completedAdoptions,

            COUNT(DISTINCT prev_o.organization_id) AS prevTotalOrganizations,
            COUNT(DISTINCT CASE WHEN prev_o.status = 'PENDING' THEN prev_o.organization_id END) AS prevPendingOrganizations,
            COUNT(DISTINCT prev_p.pet_id) AS prevTotalPets,
            COUNT(DISTINCT CASE WHEN prev_rc.status = 'IN_PROGRESS' THEN prev_rc.case_id END) AS prevOpenRescueCases,
            COUNT(DISTINCT CASE WHEN prev_aa.status = 'PENDING' THEN prev_aa.application_id END) AS prevPendingAdoptions,
            COUNT(DISTINCT CASE WHEN prev_aa.status = 'COMPLETED' THEN prev_aa.application_id END) AS prevCompletedAdoptions
        FROM (SELECT 1 AS dummy) d
        LEFT JOIN organizations o ON o.is_deleted = false AND o.created_at >= :startDate AND o.created_at < :endDate
        LEFT JOIN pets p ON p.is_deleted = false AND p.created_at >= :startDate AND p.created_at < :endDate
        LEFT JOIN rescue_cases rc ON rc.is_deleted = false AND rc.created_at >= :startDate AND rc.created_at < :endDate
        LEFT JOIN adoption_applications aa ON aa.is_deleted = false AND aa.created_at >= :startDate AND aa.created_at < :endDate

        LEFT JOIN organizations prev_o ON prev_o.is_deleted = false AND prev_o.created_at >= :prevStartDate AND prev_o.created_at < :prevEndDate
        LEFT JOIN pets prev_p ON prev_p.is_deleted = false AND prev_p.created_at >= :prevStartDate AND prev_p.created_at < :prevEndDate
        LEFT JOIN rescue_cases prev_rc ON prev_rc.is_deleted = false AND prev_rc.created_at >= :prevStartDate AND prev_rc.created_at < :prevEndDate
        LEFT JOIN adoption_applications prev_aa ON prev_aa.is_deleted = false AND prev_aa.created_at >= :prevStartDate AND prev_aa.created_at < :prevEndDate
    """, nativeQuery = true)
    DashboardProjection getDashboardOverview(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("prevStartDate") LocalDateTime prevStartDate,
            @Param("prevEndDate") LocalDateTime prevEndDate);

    @Query(value = """
        SELECT 
            EXTRACT(MONTH FROM created_at) AS month,
            COUNT(CASE WHEN status = 'PENDING' THEN 1 END) AS pending,
            COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) AS approved,
            COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) AS rejected,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed
        FROM adoption_applications
        WHERE is_deleted = false AND created_at >= :yearStart AND created_at < :yearEnd
        GROUP BY EXTRACT(MONTH FROM created_at)
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

    @Query(value = """
        SELECT 
            o.organization_id AS organizationId,
            o.name AS organizationName,
            CAST(o.status AS VARCHAR) AS status,
            COUNT(DISTINCT p.pet_id) AS totalPets,
            COUNT(DISTINCT CASE WHEN rc.status = 'IN_PROGRESS' THEN rc.case_id END) AS openRescueCases,
            COUNT(DISTINCT CASE WHEN aa.status = 'PENDING' THEN aa.application_id END) AS pendingAdoptions,
            COUNT(DISTINCT CASE WHEN aa.status = 'COMPLETED' THEN aa.application_id END) AS completedAdoptions,
            0 AS healthAlerts
        FROM organizations o
        LEFT JOIN pets p ON p.shelter_id = o.organization_id AND p.is_deleted = false AND p.created_at >= :startDate AND p.created_at < :endDate
        LEFT JOIN rescue_cases rc ON rc.organization_id = o.organization_id AND rc.is_deleted = false AND rc.created_at >= :startDate AND rc.created_at < :endDate
        LEFT JOIN adoption_applications aa ON aa.organization_id = o.organization_id AND aa.is_deleted = false AND aa.created_at >= :startDate AND aa.created_at < :endDate
        WHERE o.is_deleted = false AND o.created_at >= :startDate AND o.created_at < :endDate
        GROUP BY o.organization_id, o.name, o.status
        ORDER BY totalPets DESC
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
        WHERE o.is_deleted = false AND o.status = 'PENDING' AND o.created_at >= :startDate AND o.created_at < :endDate
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
        WHERE aa.is_deleted = false AND aa.status = 'PENDING' AND aa.created_at >= :startDate AND aa.created_at < :endDate
        ORDER BY createdAt DESC
        LIMIT 100
    """, nativeQuery = true)
    List<PendingTaskProjection> getPendingTasks(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
