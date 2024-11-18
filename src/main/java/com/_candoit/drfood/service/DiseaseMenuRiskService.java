package com._candoit.drfood.service;

import com._candoit.drfood.domain.Member;
import com._candoit.drfood.domain.Menu;
import com._candoit.drfood.domain.Nutrition;
import com._candoit.drfood.enums.RiskLevel;
import com._candoit.drfood.enums.UserDisease;
import com._candoit.drfood.global.enums.ReturnCode;
import com._candoit.drfood.global.exception.DrFoodLogicException;
import com._candoit.drfood.param.RiskCountParam;
import com._candoit.drfood.repository.MemberRepository;
import com._candoit.drfood.repository.NutritionRepository;
import com._candoit.drfood.validator.RiskLevelValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional
public class DiseaseMenuRiskService {

    private final NutritionRepository nutritionRepository;

    private final MenuService menuService;

    private final MemberRepository memberRepository;

    public RiskCountParam getRiskLevelCount(Long memberId, Page<Menu> menus) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new DrFoodLogicException(ReturnCode.NOT_FOUND_ENTITY));

        int safe = 0;
        int moderate = 0;
        int highRisk = 0;

        BigDecimal divisor = new BigDecimal("3");
        BigDecimal dailyEnergyPerMeal = member.getDailyEnergy().divide(divisor, 2, RoundingMode.HALF_UP);
        for (Menu menu : menus) {
            //퓨린 이외에는 영양성분으로 판단
            if (member.getUserDisease().equals(UserDisease.GOUT)) {
                BigDecimal totalPurineAmount = menuService.getPurineByIngredient(menu);
                RiskLevel riskLevel = RiskLevelValidator.validateGout(totalPurineAmount);
                if (riskLevel.equals(RiskLevel.SAFE)) {
                    safe++;
                } else if (riskLevel.equals(RiskLevel.MODERATE)) {
                    moderate++;
                } else {
                    highRisk++;
                }
            }
            if (member.getUserDisease().equals(UserDisease.HYPERTENSION)) {
                Nutrition nutrition = nutritionRepository.findByMenu(menu).orElseThrow(() -> new DrFoodLogicException(ReturnCode.NOT_FOUND_ENTITY));
                RiskLevel riskLevel = RiskLevelValidator.validateHyperTension(nutrition, dailyEnergyPerMeal);
                if (riskLevel.equals(RiskLevel.SAFE)) {
                    safe++;
                } else if (riskLevel.equals(RiskLevel.MODERATE)) {
                    moderate++;
                } else {
                    highRisk++;
                }
            }
            if (member.getUserDisease().equals(UserDisease.DIABETES)) {
                Nutrition nutrition = nutritionRepository.findByMenu(menu).orElseThrow(() -> new DrFoodLogicException(ReturnCode.NOT_FOUND_ENTITY));
                RiskLevel riskLevel = RiskLevelValidator.validateDiabetes(nutrition, dailyEnergyPerMeal);
                if (riskLevel.equals(RiskLevel.SAFE)) {
                    safe++;
                } else if (riskLevel.equals(RiskLevel.MODERATE)) {
                    moderate++;
                } else {
                    highRisk++;
                }
            }
        }

        return RiskCountParam.builder().safeCount(safe)
                .moderateCount(moderate)
                .highRiskCount(highRisk).build();
    }

    public RiskLevel getRiskLevel(Long memberId, Menu menu) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new DrFoodLogicException(ReturnCode.NOT_FOUND_ENTITY));

        BigDecimal divisor = new BigDecimal("3");
        BigDecimal dailyEnergyPerMeal = member.getDailyEnergy().divide(divisor, 2, RoundingMode.HALF_UP);

        //퓨린 이외에는 영양성분으로 판단
        if (member.getUserDisease().equals(UserDisease.GOUT)) {
            BigDecimal totalPurineAmount = menuService.getPurineByIngredient(menu);
            return RiskLevelValidator.validateGout(totalPurineAmount);
        }
        if (member.getUserDisease().equals(UserDisease.HYPERTENSION)) {
            Nutrition nutrition = nutritionRepository.findByMenu(menu).orElseThrow(() -> new DrFoodLogicException(ReturnCode.NOT_FOUND_ENTITY));
            return RiskLevelValidator.validateHyperTension(nutrition, dailyEnergyPerMeal);
        }
        if (member.getUserDisease().equals(UserDisease.DIABETES)) {
            Nutrition nutrition = nutritionRepository.findByMenu(menu).orElseThrow(() -> new DrFoodLogicException(ReturnCode.NOT_FOUND_ENTITY));
            return RiskLevelValidator.validateDiabetes(nutrition, dailyEnergyPerMeal);
        }
        return RiskLevel.HIGH_RISK;
    }
}