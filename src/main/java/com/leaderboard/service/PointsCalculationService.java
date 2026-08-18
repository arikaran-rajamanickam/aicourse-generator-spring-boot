package com.leaderboard.service;

import com.leaderboard.enums.PointRuleType;

public interface PointsCalculationService {
    void applyRule(Long userId, PointRuleType ruleType);
}
