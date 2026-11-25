package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.application.dto.location.ProvinceDetailDto;
import com.uit.petrescueapi.application.dto.location.ProvinceSummaryDto;
import com.uit.petrescueapi.application.dto.location.WardDto;
import com.uit.petrescueapi.application.port.out.ProvinceQueryDataPort;
import com.uit.petrescueapi.application.support.CanonicalText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the way people write places ("q9", "sg", "P. Long Bình", "Bình Dương") into the
 * administrative catalogue the app uses: 34 provinces and their wards, the two-level map in force
 * since July 2025. There are no districts any more, so an old district only tells us the province;
 * it is kept as a label ("Quận 9 (cũ)") and never turned into a ward.
 *
 * <p>Rules only, no model: the same text always gives the same answer, and it still works when the
 * AI is off. When something cannot be pinned down it stays unknown instead of being guessed.
 */
@Component
public class VietnamPlaceResolver {

    /** What a place resolves to. Any part may be null. */
    public record Place(Integer provinceCode, String provinceName, Integer wardCode, String wardName, String areaLabel) {

        static final Place NONE = new Place(null, null, null, null, null);

        /** The area shown to people: the ward if we know it, else the old district label. */
        public String area() {
            return wardName != null ? wardName : areaLabel;
        }
    }

    private static final String HCM = "ho_chi_minh";
    private static final String HANOI = "ha_noi";
    private static final String DA_NANG = "da_nang";

    /** Ways people name a province, folded with {@link CanonicalText#key}, mapped to the catalogue name key. */
    private static final Map<String, String> PROVINCE_ALIASES = Map.ofEntries(
            Map.entry("hcm", HCM), Map.entry("tphcm", HCM), Map.entry("tp_hcm", HCM), Map.entry("hcmc", HCM),
            Map.entry("sg", HCM), Map.entry("sai_gon", HCM), Map.entry("saigon", HCM), Map.entry("ho_chi_minh", HCM),
            Map.entry("tp_ho_chi_minh", HCM),
            // merged into Ho Chi Minh City in July 2025
            Map.entry("binh_duong", HCM), Map.entry("ba_ria_vung_tau", HCM), Map.entry("vung_tau", HCM), Map.entry("brvt", HCM),
            Map.entry("hn", HANOI), Map.entry("ha_noi", HANOI), Map.entry("hanoi", HANOI), Map.entry("tp_ha_noi", HANOI),
            Map.entry("da_nang", DA_NANG), Map.entry("tp_da_nang", DA_NANG),
            // merged into Da Nang in July 2025
            Map.entry("quang_nam", DA_NANG));

    /** Districts that no longer exist; each one still tells us its province. Keys without "quan"/"huyen". */
    private static final Map<String, String> OLD_DISTRICTS = new HashMap<>();
    private static final Map<String, String> OLD_DISTRICT_LABELS = new HashMap<>();

    static {
        oldDistricts(HCM, "Bình Thạnh", "Gò Vấp", "Tân Bình", "Tân Phú", "Phú Nhuận", "Bình Tân", "Thủ Đức",
                "Bình Chánh", "Nhà Bè", "Hóc Môn", "Củ Chi", "Cần Giờ");
        oldDistricts(HANOI, "Hoàn Kiếm", "Ba Đình", "Đống Đa", "Hai Bà Trưng", "Cầu Giấy", "Thanh Xuân", "Hoàng Mai",
                "Long Biên", "Tây Hồ", "Nam Từ Liêm", "Bắc Từ Liêm", "Hà Đông", "Gia Lâm", "Đông Anh", "Sóc Sơn");
        // short forms people type; each is unambiguous about the province even when not about the district
        OLD_DISTRICTS.put("bt", HCM);   // Bình Thạnh or Bình Tân: both in HCMC
        OLD_DISTRICTS.put("gv", HCM);
        OLD_DISTRICTS.put("pn", HCM);
        OLD_DISTRICTS.put("td", HCM);
        OLD_DISTRICTS.put("hbt", HANOI);
    }

    private static void oldDistricts(String province, String... names) {
        for (String name : names) {
            String key = CanonicalText.key(name);
            OLD_DISTRICTS.put(key, province);
            OLD_DISTRICT_LABELS.put(key, name);
        }
    }

    /** "q9", "q.9", "Q 9", "quan 9", "quận 09" — numbered districts only ever existed in HCMC. */
    private static final Pattern NUMBERED_DISTRICT = Pattern.compile("(?:^|_)(?:q|quan)_?0*(\\d{1,2})(?:_|$)");
    /** An explicit ward or commune: "p long binh", "phuong long binh", "x tan phu", "xa ...". */
    private static final Pattern EXPLICIT_WARD = Pattern.compile("^(?:p|phuong|x|xa|dac_khu)_(.+)$");
    private static final Pattern PROVINCE_PREFIX = Pattern.compile("^(?:tinh|thanh_pho|tp)_");
    private static final Pattern WARD_PREFIX = Pattern.compile("^(?:phuong|xa|dac_khu)_");

    private final ProvinceQueryDataPort catalogue;
    private volatile Index index;

    public VietnamPlaceResolver(ProvinceQueryDataPort catalogue) {
        this.catalogue = catalogue;
    }

    /**
     * @param province what was given as the province, e.g. "TP HCM", "sg"
     * @param area     what was given as the ward or district, e.g. "Q9", "P. Long Bình", "Gò Vấp"
     * @param text     the place as written in the post, used when the other two are empty
     */
    public Place resolve(String province, String area, String text) {
        Index idx = index();
        String provinceKey = idx.provinceKey(province)
                .or(() -> provinceFromArea(area))
                .or(() -> idx.provinceKey(text))
                .or(() -> provinceFromArea(text))
                .orElse(null);

        String areaKey = CanonicalText.key(area);
        String label = areaLabel(areaKey, area);
        WardDto ward = provinceKey == null ? null : idx.explicitWard(provinceKey, areaKey);
        if (ward == null && provinceKey != null && areaKey == null) {
            ward = idx.explicitWard(provinceKey, CanonicalText.key(text));
        }

        ProvinceDetailDto p = provinceKey == null ? null : idx.provinces.get(provinceKey);
        if (p == null && label == null) {
            return Place.NONE;
        }
        return new Place(p == null ? null : p.getCode(), p == null ? null : p.getName(),
                ward == null ? null : ward.getCode(), ward == null ? null : ward.getName(), label);
    }

    private static Optional<String> provinceFromArea(String area) {
        String key = CanonicalText.key(area);
        if (key == null) {
            return Optional.empty();
        }
        if (NUMBERED_DISTRICT.matcher(key).find()) {
            return Optional.of(HCM);
        }
        String bare = key.replaceFirst("^(?:quan|huyen|q|h)_", "");
        for (Map.Entry<String, String> entry : OLD_DISTRICTS.entrySet()) {
            if (bare.equals(entry.getKey()) || key.equals(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /** A readable label for an area that is not a current ward: "Quận 9 (cũ)", "Gò Vấp (cũ)". */
    private static String areaLabel(String key, String original) {
        if (key == null) {
            return null;
        }
        Matcher numbered = NUMBERED_DISTRICT.matcher(key);
        if (numbered.find()) {
            return "Quận " + Integer.parseInt(numbered.group(1)) + " (cũ)";
        }
        String bare = key.replaceFirst("^(?:quan|huyen|q|h)_", "");
        String name = OLD_DISTRICT_LABELS.get(bare);
        if (name != null) {
            return name + " (cũ)";
        }
        return original.trim();
    }

    private Index index() {
        Index current = index;
        if (current == null) {
            synchronized (this) {
                current = index;
                if (current == null) {
                    current = Index.build(catalogue);
                    index = current;
                }
            }
        }
        return current;
    }

    /** The catalogue, keyed for matching. Built once: the file ships with the app. */
    private record Index(Map<String, ProvinceDetailDto> provinces, Map<String, Map<String, WardDto>> wards) {

        static Index build(ProvinceQueryDataPort catalogue) {
            Map<String, ProvinceDetailDto> provinces = new HashMap<>();
            Map<String, Map<String, WardDto>> wards = new HashMap<>();
            for (ProvinceSummaryDto summary : catalogue.listProvinces(null, 0, 1000).getContent()) {
                ProvinceDetailDto detail = catalogue.getProvinceDetail(summary.getCode(), 2);
                String key = PROVINCE_PREFIX.matcher(CanonicalText.key(detail.getName())).replaceFirst("");
                provinces.put(key, detail);
                Map<String, WardDto> byName = new HashMap<>();
                for (WardDto ward : detail.getWards() == null ? List.<WardDto>of() : detail.getWards()) {
                    byName.put(WARD_PREFIX.matcher(CanonicalText.key(ward.getName())).replaceFirst(""), ward);
                }
                wards.put(key, byName);
            }
            return new Index(provinces, wards);
        }

        Optional<String> provinceKey(String text) {
            String key = CanonicalText.key(text);
            if (key == null) {
                return Optional.empty();
            }
            if (key.chars().allMatch(Character::isDigit)) {
                int code = Integer.parseInt(key);
                return provinces.entrySet().stream()
                        .filter(e -> e.getValue().getCode() != null && e.getValue().getCode() == code)
                        .map(Map.Entry::getKey).findFirst();
            }
            String bare = PROVINCE_PREFIX.matcher(key).replaceFirst("");
            if (provinces.containsKey(bare)) {
                return Optional.of(bare);
            }
            String alias = PROVINCE_ALIASES.get(key);
            if (alias == null) alias = PROVINCE_ALIASES.get(bare);
            if (alias != null && provinces.containsKey(alias)) {
                return Optional.of(alias);
            }
            // a longer text: look for a province name or alias as a whole word run
            List<String> candidates = new ArrayList<>(provinces.keySet());
            candidates.addAll(PROVINCE_ALIASES.keySet());
            String padded = "_" + key + "_";
            String found = null;
            for (String candidate : candidates) {
                if (candidate.length() >= 3 && padded.contains("_" + candidate + "_")) {
                    String target = provinces.containsKey(candidate) ? candidate : PROVINCE_ALIASES.get(candidate);
                    if (found != null && !found.equals(target)) {
                        return Optional.empty(); // two provinces named: do not pick one
                    }
                    found = target;
                }
            }
            return Optional.ofNullable(found).filter(provinces::containsKey);
        }

        /** Only a ward the text names explicitly ("P. Long Bình"), inside the given province. */
        WardDto explicitWard(String provinceKey, String areaKey) {
            if (areaKey == null) {
                return null;
            }
            Matcher m = EXPLICIT_WARD.matcher(areaKey);
            if (!m.find()) {
                return null;
            }
            return wards.getOrDefault(provinceKey, Map.of()).get(m.group(1));
        }
    }
}
