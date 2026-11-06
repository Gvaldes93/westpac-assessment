package com.sourcecodesolutions.techassesment.westpac.account.nickname;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Instantiated by Spring's constraint validator factory, so the checker is constructor injected. */
class NotOffensiveValidator implements ConstraintValidator<NotOffensive, String> {

    private final OffensiveNicknameChecker checker;

    NotOffensiveValidator(OffensiveNicknameChecker checker) {
        this.checker = checker;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !checker.isOffensive(value);
    }
}
