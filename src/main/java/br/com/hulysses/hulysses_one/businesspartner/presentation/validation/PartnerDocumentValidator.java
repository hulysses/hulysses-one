package br.com.hulysses.hulysses_one.businesspartner.presentation.validation;

import br.com.hulysses.hulysses_one.businesspartner.domain.BusinessPartnerType;
import br.com.hulysses.hulysses_one.businesspartner.presentation.dto.BusinessPartnerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PartnerDocumentValidator implements ConstraintValidator<ValidPartnerDocument, BusinessPartnerRequest> {
    @Override
    public boolean isValid(BusinessPartnerRequest request, ConstraintValidatorContext context) {
        if (request == null || request.type() == null || request.document() == null || request.document().isBlank()) {
            return true; // Campos obrigatórios são tratados por @NotNull e @NotBlank.
        }
        String format = request.type() == BusinessPartnerType.INDIVIDUAL ? "\\d{11}" : "\\d{14}";
        if (request.document().matches(format)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("document").addConstraintViolation();
        return false;
    }
}
