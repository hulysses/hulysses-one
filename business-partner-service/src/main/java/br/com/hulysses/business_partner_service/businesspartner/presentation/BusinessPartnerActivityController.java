package br.com.hulysses.business_partner_service.businesspartner.presentation;

import br.com.hulysses.business_partner_service.businesspartner.application.BusinessPartnerActivityService;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerActivityResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/business-partners/{id}/activities")
public class BusinessPartnerActivityController {
    private final BusinessPartnerActivityService service;

    public BusinessPartnerActivityController(BusinessPartnerActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<BusinessPartnerActivityResponse> findByPartner(@PathVariable Long id) {
        return service.findByPartner(id);
    }
}
