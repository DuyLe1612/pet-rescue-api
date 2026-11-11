package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.AdopterProfile;
import com.uit.petrescueapi.domain.repository.AdopterProfileRepository;
import com.uit.petrescueapi.domain.valueobject.BuildingPetRule;
import com.uit.petrescueapi.domain.valueobject.HousingCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

/** Plain JDBC: one flat row per user, no relations to load. Runs inside the domain service's transaction. */
@Component
@RequiredArgsConstructor
public class AdopterProfileRepositoryAdapter implements AdopterProfileRepository {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<AdopterProfile> findByUserId(UUID userId) {
        return jdbc.query("SELECT * FROM adopter_profiles WHERE user_id = ?", MAPPER, userId).stream().findFirst();
    }

    @Override
    public AdopterProfile save(AdopterProfile p) {
        jdbc.update("""
                INSERT INTO adopter_profiles (user_id, housing_type, building_rule, has_children_under_5, has_children_5_to_8,
                    has_children_over_8, allergy_dog, allergy_cat, has_dog, has_cat, hours_away_per_day, activity_level,
                    monthly_budget, emergency_vet_budget, adjustment_weeks_expected, intolerable_behaviors,
                    return_circumstances, relocation_plan, experience_level, willing_training, province_code,
                    consent_at, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET
                    housing_type = EXCLUDED.housing_type, building_rule = EXCLUDED.building_rule,
                    has_children_under_5 = EXCLUDED.has_children_under_5, has_children_5_to_8 = EXCLUDED.has_children_5_to_8,
                    has_children_over_8 = EXCLUDED.has_children_over_8, allergy_dog = EXCLUDED.allergy_dog,
                    allergy_cat = EXCLUDED.allergy_cat, has_dog = EXCLUDED.has_dog, has_cat = EXCLUDED.has_cat,
                    hours_away_per_day = EXCLUDED.hours_away_per_day, activity_level = EXCLUDED.activity_level,
                    monthly_budget = EXCLUDED.monthly_budget, emergency_vet_budget = EXCLUDED.emergency_vet_budget,
                    adjustment_weeks_expected = EXCLUDED.adjustment_weeks_expected,
                    intolerable_behaviors = EXCLUDED.intolerable_behaviors,
                    return_circumstances = EXCLUDED.return_circumstances, relocation_plan = EXCLUDED.relocation_plan,
                    experience_level = EXCLUDED.experience_level, willing_training = EXCLUDED.willing_training,
                    province_code = EXCLUDED.province_code, updated_at = EXCLUDED.updated_at,
                    version = adopter_profiles.version + 1
                """,
                p.getUserId(), p.getHousingType().name(), p.getBuildingRule().name(), p.isHasChildrenUnder5(),
                p.isHasChildren5To8(), p.isHasChildrenOver8(), p.isAllergyDog(), p.isAllergyCat(), p.isHasDog(), p.isHasCat(),
                p.getHoursAwayPerDay(), p.getActivityLevel(), p.getMonthlyBudget(), p.getEmergencyVetBudget(),
                p.getAdjustmentWeeksExpected(), p.getIntolerableBehaviors(), p.getReturnCircumstances(),
                p.getRelocationPlan(), p.getExperienceLevel(), p.getWillingTraining(), p.getProvinceCode(),
                Timestamp.valueOf(p.getConsentAt()), Timestamp.valueOf(p.getUpdatedAt()), Timestamp.valueOf(p.getUpdatedAt()));
        return findByUserId(p.getUserId()).orElseThrow();
    }

    private static final RowMapper<AdopterProfile> MAPPER = (rs, row) -> AdopterProfile.builder()
            .userId(rs.getObject("user_id", UUID.class))
            .housingType(HousingCondition.valueOf(rs.getString("housing_type")))
            .buildingRule(BuildingPetRule.valueOf(rs.getString("building_rule")))
            .hasChildrenUnder5(rs.getBoolean("has_children_under_5"))
            .hasChildren5To8(rs.getBoolean("has_children_5_to_8"))
            .hasChildrenOver8(rs.getBoolean("has_children_over_8"))
            .allergyDog(rs.getBoolean("allergy_dog"))
            .allergyCat(rs.getBoolean("allergy_cat"))
            .hasDog(rs.getBoolean("has_dog"))
            .hasCat(rs.getBoolean("has_cat"))
            .hoursAwayPerDay(integer(rs, "hours_away_per_day"))
            .activityLevel(integer(rs, "activity_level"))
            .monthlyBudget(integer(rs, "monthly_budget"))
            .emergencyVetBudget(integer(rs, "emergency_vet_budget"))
            .adjustmentWeeksExpected(integer(rs, "adjustment_weeks_expected"))
            .intolerableBehaviors(rs.getString("intolerable_behaviors"))
            .returnCircumstances(rs.getString("return_circumstances"))
            .relocationPlan(rs.getString("relocation_plan"))
            .experienceLevel(integer(rs, "experience_level"))
            .willingTraining(rs.getObject("willing_training", Boolean.class))
            .provinceCode(rs.getString("province_code"))
            .consentAt(rs.getTimestamp("consent_at").toLocalDateTime())
            .version(rs.getInt("version"))
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    static Integer integer(ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
