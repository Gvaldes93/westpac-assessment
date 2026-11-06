package com.sourcecodesolutions.techassesment.westpac.account.nickname;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The annotated text must not contain terms from the offensive word list. {@code null} is valid. */
@Documented
@Constraint(validatedBy = NotOffensiveValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotOffensive {

    String message() default "must not contain offensive language";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
