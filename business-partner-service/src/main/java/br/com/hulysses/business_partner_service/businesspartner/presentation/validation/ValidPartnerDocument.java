package br.com.hulysses.business_partner_service.businesspartner.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PartnerDocumentValidator.class)
public @interface ValidPartnerDocument {
    String message() default "Document must contain 11 digits for INDIVIDUAL or 14 digits for COMPANY";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
