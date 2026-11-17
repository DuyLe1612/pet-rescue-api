package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdopterProfile;
import com.uit.petrescueapi.domain.entity.PetTraits;
import com.uit.petrescueapi.domain.valueobject.BiteHistory;
import com.uit.petrescueapi.domain.valueobject.BuildingPetRule;
import com.uit.petrescueapi.domain.valueobject.CareLevel;
import com.uit.petrescueapi.domain.valueobject.HousingCondition;
import com.uit.petrescueapi.domain.valueobject.MatchCertainty;
import com.uit.petrescueapi.domain.valueobject.MatchCriterion;
import com.uit.petrescueapi.domain.valueobject.MatchLevel;
import com.uit.petrescueapi.domain.valueobject.MatchReasonStatus;
import com.uit.petrescueapi.domain.valueobject.MatchResult;
import com.uit.petrescueapi.domain.valueobject.MatchWarningCode;
import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.domain.valueobject.SpeciesKind;
import com.uit.petrescueapi.domain.valueobject.TraitSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Explainable compatibility matching: explicit rules, no model call, no I/O. The same input always
 * gives the same result.
 *
 * <ol>
 *   <li>Constraint warnings. Any warning makes the level NOT_SUITABLE and leaves no score. A warning
 *       never blocks an application; the organization decides.</li>
 *   <li>Five weighted criteria. Each score is pulled towards neutral by how sure we are about the
 *       pet: {@code adjusted = 0.5 + c * (s - 0.5)}. A pet nobody described lands near the middle
 *       instead of at the bottom.</li>
 * </ol>
 *
 * The thresholds and certainty values are starting assumptions (agent/04-design-matching.md), not
 * measured ones. Change them together with {@link #WEIGHTS_VERSION}.
 */
@Service
public class MatchingService {

    public static final int WEIGHTS_VERSION = 1;

    private static final double HIGH_FROM = 75;
    private static final double MEDIUM_FROM = 50;
    /** A criterion reads as fine from this score up. */
    private static final double OK_FROM = 0.7;
    /** "Clearly not good with": the lowest step of the 1-5 scale. */
    private static final int CLEARLY_NOT = 1;

    public MatchResult evaluate(AdopterProfile adopter, SpeciesKind species, PetTraits traits) {
        PetTraits pet = traits == null ? PetTraits.unknown(null) : traits;
        double source = sourceCertainty(pet);

        List<MatchResult.Warning> warnings = warnings(adopter, species, pet);

        List<Criterion> criteria = List.of(
                new Criterion(MatchCriterion.LIFESTYLE, lifestyle(adopter, pet, source)),
                new Criterion(MatchCriterion.HOUSING, housing(adopter, pet, source)),
                new Criterion(MatchCriterion.FAMILY, family(adopter, pet, source)),
                new Criterion(MatchCriterion.EXISTING_PETS, existingPets(adopter, pet, source)),
                new Criterion(MatchCriterion.READINESS, readiness(adopter, pet, source)));

        double weighted = 0;
        double certainty = 0;
        int weights = 0;
        List<MatchResult.Reason> reasons = new ArrayList<>();
        for (Criterion criterion : criteria) {
            int weight = criterion.code().weight();
            weighted += weight * adjust(criterion.score(), criterion.certainty());
            certainty += weight * criterion.certainty();
            weights += weight;
            reasons.add(reason(criterion));
        }
        double score = round1(100 * weighted / weights);
        MatchCertainty overall = MatchCertainty.of(certainty / weights);
        if (overall == MatchCertainty.NONE) {
            overall = MatchCertainty.LOW;
        }

        boolean warned = !warnings.isEmpty();
        return new MatchResult(
                warned ? MatchLevel.NOT_SUITABLE : level(score),
                warned ? null : score,
                overall,
                WEIGHTS_VERSION,
                List.copyOf(reasons),
                List.copyOf(warnings),
                List.copyOf(preparation(adopter, pet)));
    }

    /** {@code adjusted = 0.5 + c * (s - 0.5)}: no certainty gives exactly 0.5. */
    static double adjust(double score, double certainty) {
        return 0.5 + certainty * (score - 0.5);
    }

    // ── Step 1: constraint warnings ─────────────────

    private List<MatchResult.Warning> warnings(AdopterProfile adopter, SpeciesKind species, PetTraits pet) {
        List<MatchWarningCode> codes = new ArrayList<>();
        if (violatesBuildingRule(adopter.getBuildingRule(), species, pet.getSize())) {
            codes.add(MatchWarningCode.BUILDING_RULE);
        }
        if ((species == SpeciesKind.DOG && adopter.isAllergyDog()) || (species == SpeciesKind.CAT && adopter.isAllergyCat())) {
            codes.add(MatchWarningCode.ALLERGY);
        }
        if (adopter.hasYoungChildren() && isClearlyNot(pet.getFriendlyChildren())) {
            codes.add(MatchWarningCode.NOT_GOOD_WITH_CHILDREN);
        }
        if (adopter.isHasDog() && isClearlyNot(pet.getGoodWithDogs())) {
            codes.add(MatchWarningCode.NOT_GOOD_WITH_DOGS);
        }
        if (adopter.isHasCat() && isClearlyNot(pet.getGoodWithCats())) {
            codes.add(MatchWarningCode.NOT_GOOD_WITH_CATS);
        }
        if (adopter.hasYoungChildren() && pet.getBiteHistory() == BiteHistory.INJURY && pet.isBiteVerified()) {
            codes.add(MatchWarningCode.BITE_HISTORY_WITH_CHILD);
        }
        return codes.stream().map(code -> new MatchResult.Warning(code, code.text())).toList();
    }

    private static boolean violatesBuildingRule(BuildingPetRule rule, SpeciesKind species, PetSize size) {
        if (rule == null) {
            return false;
        }
        return switch (rule) {
            case NO_PETS -> true;
            case NO_DOGS -> species == SpeciesKind.DOG;
            case NO_CATS -> species == SpeciesKind.CAT;
            case SMALL_ONLY -> size == PetSize.MEDIUM || size == PetSize.LARGE;
            case ALLOWED, UNKNOWN -> false;
        };
    }

    /** Unknown never counts as a violation. */
    private static boolean isClearlyNot(Integer scale) {
        return scale != null && scale <= CLEARLY_NOT;
    }

    // ── Step 2: weighted criteria ───────────────────

    private List<Part> lifestyle(AdopterProfile adopter, PetTraits pet, double source) {
        List<Part> parts = new ArrayList<>();
        parts.add(pet.getEnergy() == null || adopter.getActivityLevel() == null
                ? Part.unknown()
                : Part.fromPet(1 - Math.abs(pet.getEnergy() - adopter.getActivityLevel()) / 4.0, source));
        parts.add(pet.getAloneMinutes() == null || adopter.getHoursAwayPerDay() == null
                ? Part.unknown()
                : Part.fromPet(aloneFit(pet.getAloneMinutes(), adopter.getHoursAwayPerDay()), source));
        return parts;
    }

    private static double aloneFit(int aloneMinutes, int hoursAway) {
        int needed = hoursAway * 60;
        return needed == 0 ? 1 : Math.min(1, (double) aloneMinutes / needed);
    }

    private List<Part> housing(AdopterProfile adopter, PetTraits pet, double source) {
        Space space = Space.of(adopter.getHousingType());
        List<Part> parts = new ArrayList<>();
        parts.add(pet.getSize() == null ? Part.unknown() : Part.fromPet(space.sizeFit(pet.getSize()), source));
        parts.add(pet.getNoiseLevel() == null
                ? Part.unknown()
                : Part.fromPet(1 - downScale(pet.getNoiseLevel()) * space.noiseSensitivity, source));
        return parts;
    }

    private List<Part> family(AdopterProfile adopter, PetTraits pet, double source) {
        List<Part> parts = new ArrayList<>();
        parts.add(higherIsBetter(pet.getFriendlyPeople(), source));
        parts.add(lowerIsBetter(pet.getFearStrangers(), source));
        if (adopter.hasChildren()) {
            parts.add(higherIsBetter(pet.getFriendlyChildren(), source));
        }
        if (adopter.hasYoungChildren()) {
            parts.add(lowerIsBetter(pet.getFearNoise(), source));
        }
        return parts;
    }

    private List<Part> existingPets(AdopterProfile adopter, PetTraits pet, double source) {
        if (!adopter.isHasDog() && !adopter.isHasCat()) {
            return List.of(Part.fromAdopter(1));        // nothing at home to clash with
        }
        List<Part> parts = new ArrayList<>();
        if (adopter.isHasDog()) {
            parts.add(higherIsBetter(pet.getGoodWithDogs(), source));
        }
        if (adopter.isHasCat()) {
            parts.add(higherIsBetter(pet.getGoodWithCats(), source));
        }
        return parts;
    }

    /** Experience is deliberately absent: a first-time adopter is not a worse one. */
    private List<Part> readiness(AdopterProfile adopter, PetTraits pet, double source) {
        List<Part> parts = new ArrayList<>();
        parts.add(pet.getMonthlyCost() == null || adopter.getMonthlyBudget() == null
                ? Part.unknown()
                : Part.fromPet(budgetFit(adopter.getMonthlyBudget(), pet.getMonthlyCost()), source));
        parts.add(pet.getCareLevel() == null || adopter.getHoursAwayPerDay() == null
                ? Part.unknown()
                : Part.fromPet(careFit(pet.getCareLevel(), adopter.getHoursAwayPerDay()), source));
        if (adopter.getAdjustmentWeeksExpected() != null) {
            parts.add(Part.fromAdopter(expectationFit(adopter.getAdjustmentWeeksExpected())));
        }
        return parts;
    }

    private static double budgetFit(int budget, int cost) {
        return cost == 0 ? 1 : Math.min(1, (double) budget / cost);
    }

    private static double careFit(CareLevel care, int hoursAway) {
        int band = hoursAway <= 4 ? 0 : hoursAway <= 8 ? 1 : 2;
        return switch (care) {
            case LOW -> 1;
            case MEDIUM -> new double[]{1, 0.85, 0.6}[band];
            case HIGH -> new double[]{1, 0.7, 0.4}[band];
        };
    }

    /** Most pets need weeks to settle; expecting days is the mismatch worth talking about. */
    private static double expectationFit(int weeks) {
        return weeks >= 4 ? 1 : weeks >= 2 ? 0.8 : 0.5;
    }

    private static Part higherIsBetter(Integer scale, double source) {
        return scale == null ? Part.unknown() : Part.fromPet(1 - downScale(6 - scale), source);
    }

    private static Part lowerIsBetter(Integer scale, double source) {
        return scale == null ? Part.unknown() : Part.fromPet(1 - downScale(scale), source);
    }

    /** 1..5 to 0..1. */
    private static double downScale(int scale) {
        return (scale - 1) / 4.0;
    }

    private static double sourceCertainty(PetTraits pet) {
        TraitSource source = pet.getSourceRole() == null ? TraitSource.STAFF : pet.getSourceRole();
        return source.certainty();
    }

    private static MatchLevel level(double score) {
        return score >= HIGH_FROM ? MatchLevel.HIGH : score >= MEDIUM_FROM ? MatchLevel.MEDIUM : MatchLevel.LOW;
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    // ── Reasons ─────────────────────────────────────

    private static MatchResult.Reason reason(Criterion criterion) {
        MatchReasonStatus status = !criterion.hasKnownPart()
                ? MatchReasonStatus.UNKNOWN
                : criterion.score() >= OK_FROM ? MatchReasonStatus.OK : MatchReasonStatus.WARN;
        MatchCertainty certainty = status == MatchReasonStatus.UNKNOWN
                ? MatchCertainty.NONE
                : MatchCertainty.of(criterion.certainty());
        String text = text(criterion.code(), status);
        if (certainty == MatchCertainty.LOW) {
            text += "; thông tin về bé mới từ một lời kể";
        }
        return new MatchResult.Reason(criterion.code(), criterion.code().weight(), status, certainty, text);
    }

    private static String text(MatchCriterion code, MatchReasonStatus status) {
        return switch (code) {
            case LIFESTYLE -> switch (status) {
                case OK -> "Mức vận động và thời gian vắng nhà của bạn hợp với bé";
                case WARN -> "Nếp sinh hoạt của bạn chưa khớp với bé: cần xem lại mức vận động hoặc thời gian bé ở một mình";
                case UNKNOWN -> "Tổ chức chưa ghi mức năng lượng và khả năng ở một mình của bé";
            };
            case HOUSING -> switch (status) {
                case OK -> "Kích cỡ và độ ồn của bé hợp với nơi ở của bạn";
                case WARN -> "Nơi ở của bạn hơi chật hoặc dễ bị ảnh hưởng bởi tiếng ồn so với bé";
                case UNKNOWN -> "Tổ chức chưa ghi kích cỡ và độ ồn của bé";
            };
            case FAMILY -> switch (status) {
                case OK -> "Bé thân thiện, hợp với các thành viên trong nhà bạn";
                case WARN -> "Bé còn nhút nhát hoặc chưa quen với người trong nhà, nhất là trẻ em";
                case UNKNOWN -> "Tổ chức chưa ghi bé có thân thiện với người và trẻ em không";
            };
            case EXISTING_PETS -> switch (status) {
                case OK -> "Bé hợp với hoàn cảnh vật nuôi hiện có của nhà bạn";
                case WARN -> "Cần cân nhắc vì bé chưa hợp với vật nuôi bạn đang có";
                case UNKNOWN -> "Tổ chức chưa ghi bé có hợp với chó, mèo khác không";
            };
            case READINESS -> switch (status) {
                case OK -> "Ngân sách, thời gian và kỳ vọng của bạn phù hợp với công chăm sóc bé";
                case WARN -> "Bé cần nhiều chi phí, công chăm hoặc thời gian thích nghi hơn bạn dự tính";
                case UNKNOWN -> "Chưa đủ thông tin về chi phí và công chăm sóc bé";
            };
        };
    }

    // ── Preparation: fixed templates, one per gap ───

    private List<String> preparation(AdopterProfile adopter, PetTraits pet) {
        List<String> steps = new ArrayList<>();
        if (adopter.isHasCat()) {
            steps.add("Giới thiệu bé với mèo nhà bạn dần dần trong 1–2 tuần đầu");
        }
        if (adopter.isHasDog()) {
            steps.add("Cho bé làm quen với chó nhà bạn ở nơi trung lập trước khi về chung nhà");
        }
        if (adopter.hasYoungChildren()) {
            steps.add("Luôn có người lớn trông khi bé ở cùng trẻ nhỏ");
        }
        if (pet.getAloneMinutes() != null && adopter.getHoursAwayPerDay() != null
                && pet.getAloneMinutes() < adopter.getHoursAwayPerDay() * 60) {
            steps.add("Bé chưa quen ở một mình lâu: tập tăng dần thời gian vắng nhà hoặc nhờ người ghé qua");
        }
        if (pet.getEnergy() != null && adopter.getActivityLevel() != null
                && pet.getEnergy() - adopter.getActivityLevel() >= 2) {
            steps.add("Bé nhiều năng lượng: chuẩn bị lịch đi dạo và đồ chơi mỗi ngày");
        }
        if (pet.getNoiseLevel() != null && pet.getNoiseLevel() >= 4 && Space.of(adopter.getHousingType()) == Space.COMPACT) {
            steps.add("Bé khá ồn: báo trước với hàng xóm và tập cho bé yên khi ở nhà");
        }
        if (pet.getFearStrangers() != null && pet.getFearStrangers() >= 4) {
            steps.add("Bé nhút nhát với người lạ: dành cho bé một góc yên tĩnh và đừng ép tiếp xúc");
        }
        if (pet.getMonthlyCost() != null && adopter.getMonthlyBudget() != null
                && pet.getMonthlyCost() > adopter.getMonthlyBudget()) {
            steps.add("Chi phí chăm bé mỗi tháng cao hơn ngân sách bạn khai: cân nhắc trước khi nhận");
        }
        if (adopter.getAdjustmentWeeksExpected() == null || adopter.getAdjustmentWeeksExpected() < 4) {
            steps.add("Bé thường cần vài tuần để quen nhà, chuyện đi vệ sinh sai chỗ lúc đầu là bình thường");
        }
        return steps;
    }

    // ── Small types ─────────────────────────────────

    /** One comparison inside a criterion. A null score means the needed fact is missing. */
    private record Part(Double score, double certainty) {
        static Part unknown() {
            return new Part(null, 0);
        }

        static Part fromPet(double score, double sourceCertainty) {
            return new Part(score, sourceCertainty);
        }

        /** Rests only on what the adopter declared, so there is no doubt about the pet in it. */
        static Part fromAdopter(double score) {
            return new Part(score, 1);
        }
    }

    private record Criterion(MatchCriterion code, List<Part> parts) {
        boolean hasKnownPart() {
            return parts.stream().anyMatch(part -> part.score() != null);
        }

        /** Mean of what is known; neutral when nothing is. */
        double score() {
            return parts.stream().filter(part -> part.score() != null).mapToDouble(Part::score).average().orElse(0.5);
        }

        /** Missing parts count as zero, so half-described pets are only half trusted. */
        double certainty() {
            return parts.stream().mapToDouble(Part::certainty).average().orElse(0);
        }
    }

    private enum Space {
        COMPACT(0.6), MIDDLE(0.4), ROOMY(0.2);

        final double noiseSensitivity;

        Space(double noiseSensitivity) {
            this.noiseSensitivity = noiseSensitivity;
        }

        static Space of(HousingCondition housing) {
            if (housing == null) {
                return MIDDLE;
            }
            return switch (housing) {
                case APARTMENT, DORMITORY -> COMPACT;
                case HOUSE, FARM -> ROOMY;
                case OTHER -> MIDDLE;
            };
        }

        double sizeFit(PetSize size) {
            return switch (this) {
                case COMPACT -> size == PetSize.SMALL ? 1 : size == PetSize.MEDIUM ? 0.7 : 0.4;
                case MIDDLE -> size == PetSize.SMALL ? 1 : size == PetSize.MEDIUM ? 0.85 : 0.6;
                case ROOMY -> size == PetSize.LARGE ? 0.9 : 1;
            };
        }
    }
}
