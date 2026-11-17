package com.uit.petrescueapi.domain.valueobject;

/** Constraint warnings. They flag an application for the organization; they do not reject it. */
public enum MatchWarningCode {
    BUILDING_RULE("Nội quy nơi ở của bạn không cho nuôi loài hoặc kích cỡ này"),
    ALLERGY("Nhà bạn có người dị ứng với loài này"),
    NOT_GOOD_WITH_CHILDREN("Bé không hợp với trẻ em, trong khi nhà bạn có trẻ nhỏ"),
    NOT_GOOD_WITH_DOGS("Bé không hợp với chó, trong khi bạn đang nuôi chó"),
    NOT_GOOD_WITH_CATS("Bé không hợp với mèo, trong khi bạn đang nuôi mèo"),
    BITE_HISTORY_WITH_CHILD("Bé từng cắn gây thương tích (đã xác minh), trong khi nhà bạn có trẻ nhỏ");

    private final String text;

    MatchWarningCode(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
