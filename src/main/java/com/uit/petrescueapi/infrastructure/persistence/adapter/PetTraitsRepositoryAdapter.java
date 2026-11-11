package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.PetTraits;
import com.uit.petrescueapi.domain.repository.PetTraitsRepository;
import com.uit.petrescueapi.domain.valueobject.BiteHistory;
import com.uit.petrescueapi.domain.valueobject.CareLevel;
import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.domain.valueobject.TraitSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.uit.petrescueapi.infrastructure.persistence.adapter.AdopterProfileRepositoryAdapter.integer;

/** Plain JDBC: one flat row per pet. Runs inside the caller's transaction. */
@Component
@RequiredArgsConstructor
public class PetTraitsRepositoryAdapter implements PetTraitsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<PetTraits> findByPetId(UUID petId) {
        return jdbc.query("SELECT * FROM pet_traits WHERE pet_id = :petId", Map.of("petId", petId), MAPPER)
                .stream().findFirst();
    }

    @Override
    public Map<UUID, PetTraits> findByPetIds(Collection<UUID> petIds) {
        if (petIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("SELECT * FROM pet_traits WHERE pet_id IN (:petIds)", Map.of("petIds", petIds), MAPPER)
                .stream().collect(Collectors.toMap(PetTraits::getPetId, Function.identity()));
    }

    @Override
    public PetTraits save(PetTraits t) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("petId", t.getPetId())
                .addValue("friendlyPeople", t.getFriendlyPeople())
                .addValue("friendlyChildren", t.getFriendlyChildren())
                .addValue("fearStrangers", t.getFearStrangers())
                .addValue("fearNoise", t.getFearNoise())
                .addValue("energy", t.getEnergy())
                .addValue("goodWithDogs", t.getGoodWithDogs())
                .addValue("goodWithCats", t.getGoodWithCats())
                .addValue("aloneMinutes", t.getAloneMinutes())
                .addValue("houseTrained", t.getHouseTrained())
                .addValue("biteHistory", name(t.getBiteHistory()))
                .addValue("biteVerified", t.isBiteVerified())
                .addValue("size", name(t.getSize()))
                .addValue("noiseLevel", t.getNoiseLevel())
                .addValue("monthlyCost", t.getMonthlyCost())
                .addValue("careLevel", name(t.getCareLevel()))
                .addValue("sourceRole", t.getSourceRole().name())
                .addValue("note", t.getNote())
                .addValue("updatedBy", t.getUpdatedBy())
                .addValue("updatedAt", Timestamp.valueOf(t.getUpdatedAt()));
        jdbc.update("""
                INSERT INTO pet_traits (pet_id, friendly_people, friendly_children, fear_strangers, fear_noise, energy,
                    good_with_dogs, good_with_cats, alone_minutes, house_trained, bite_history, bite_verified, size,
                    noise_level, monthly_cost, care_level, source_role, note, updated_by, updated_at)
                VALUES (:petId, :friendlyPeople, :friendlyChildren, :fearStrangers, :fearNoise, :energy,
                    :goodWithDogs, :goodWithCats, :aloneMinutes, :houseTrained, :biteHistory, :biteVerified, :size,
                    :noiseLevel, :monthlyCost, :careLevel, :sourceRole, :note, :updatedBy, :updatedAt)
                ON CONFLICT (pet_id) DO UPDATE SET
                    friendly_people = EXCLUDED.friendly_people, friendly_children = EXCLUDED.friendly_children,
                    fear_strangers = EXCLUDED.fear_strangers, fear_noise = EXCLUDED.fear_noise, energy = EXCLUDED.energy,
                    good_with_dogs = EXCLUDED.good_with_dogs, good_with_cats = EXCLUDED.good_with_cats,
                    alone_minutes = EXCLUDED.alone_minutes, house_trained = EXCLUDED.house_trained,
                    bite_history = EXCLUDED.bite_history, bite_verified = EXCLUDED.bite_verified, size = EXCLUDED.size,
                    noise_level = EXCLUDED.noise_level, monthly_cost = EXCLUDED.monthly_cost,
                    care_level = EXCLUDED.care_level, source_role = EXCLUDED.source_role, note = EXCLUDED.note,
                    updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at
                """, params);
        return findByPetId(t.getPetId()).orElseThrow();
    }

    @Override
    public Optional<String> findSpeciesName(UUID petId) {
        return jdbc.queryForList("""
                SELECT ps.name FROM pets p JOIN pet_species ps ON ps.species_id = p.species_id
                WHERE p.pet_id = :petId AND p.is_deleted = FALSE
                """, Map.of("petId", petId), String.class).stream().findFirst();
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static final RowMapper<PetTraits> MAPPER = (rs, row) -> PetTraits.builder()
            .petId(rs.getObject("pet_id", UUID.class))
            .friendlyPeople(integer(rs, "friendly_people"))
            .friendlyChildren(integer(rs, "friendly_children"))
            .fearStrangers(integer(rs, "fear_strangers"))
            .fearNoise(integer(rs, "fear_noise"))
            .energy(integer(rs, "energy"))
            .goodWithDogs(integer(rs, "good_with_dogs"))
            .goodWithCats(integer(rs, "good_with_cats"))
            .aloneMinutes(integer(rs, "alone_minutes"))
            .houseTrained(integer(rs, "house_trained"))
            .biteHistory(parse(BiteHistory.class, rs.getString("bite_history")))
            .biteVerified(rs.getBoolean("bite_verified"))
            .size(parse(PetSize.class, rs.getString("size")))
            .noiseLevel(integer(rs, "noise_level"))
            .monthlyCost(integer(rs, "monthly_cost"))
            .careLevel(parse(CareLevel.class, rs.getString("care_level")))
            .sourceRole(parse(TraitSource.class, rs.getString("source_role")))
            .note(rs.getString("note"))
            .updatedBy(rs.getObject("updated_by", UUID.class))
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();
}
