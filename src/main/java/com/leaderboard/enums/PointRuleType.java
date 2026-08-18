package com.leaderboard.enums;

public enum PointRuleType {
    COURSE_COMPLETION("course_completion"),
    LESSON_COMPLETION("lesson_completion"),
    DAILY_LOGIN("daily_login"),
    STREAK_BONUS("streak_bonus");

    private final String value;

    PointRuleType(String value) {
        this.value = value;
    }

    public String getValue(){
        return value; 
    }
}