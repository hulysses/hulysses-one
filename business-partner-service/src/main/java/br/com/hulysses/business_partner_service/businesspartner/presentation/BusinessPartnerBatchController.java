package br.com.hulysses.business_partner_service.businesspartner.presentation;

import br.com.hulysses.business_partner_service.businesspartner.application.BusinessPartnerImportService;
import br.com.hulysses.business_partner_service.businesspartner.presentation.dto.BusinessPartnerImportResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/batch/business-partners")
public class BusinessPartnerBatchController {
    private final BusinessPartnerImportService service;

    public BusinessPartnerBatchController(BusinessPartnerImportService service) {
        this.service = service;
    }

    @PostMapping("/import")
    public BusinessPartnerImportResponse importCsv(@RequestPart(required = false) MultipartFile file)
            throws Exception {
        return service.importCsv(file);
    }
}
