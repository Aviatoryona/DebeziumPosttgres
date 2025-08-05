package debezium.enums;

import lombok.Getter;

@Getter
public enum MonthEnum {
    JAN("JANUARY", 1),
    FEB("FEBRUARY", 2),
    MAR("MARCH", 3),
    APR("APRIL", 4),
    MAY("MAY", 5),
    JUN("JUNE", 6),
    JUL("JULY", 7),
    AUG("AUGUST", 8),
    SEP("SEPTEMBER", 9),
    OCT("OCTOBER", 10),
    NOV("NOVEMBER", 11),
    DEC("DECEMBER", 12);

    private final String name;
    private final int monthNumber;
    MonthEnum(String name, int i) {
        this.monthNumber = i;
        this.name = name;
    }
}
