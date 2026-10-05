package br.com.hulysses.hulysses_one.businesspartner.integration;

import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerRequest;
import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "business-partner-service", url = "${services.business-partner.url}")
public interface BusinessPartnerClient {
    @PostMapping("/business-partners")
    BusinessPartnerResponse create(@RequestBody BusinessPartnerRequest request);

    @PutMapping("/business-partners/{id}")
    BusinessPartnerResponse update(@PathVariable("id") Long id, @RequestBody BusinessPartnerRequest request);

    @DeleteMapping("/business-partners/{id}")
    void delete(@PathVariable("id") Long id);

    @GetMapping("/business-partners/{id}")
    BusinessPartnerResponse getById(@PathVariable("id") Long id);

    @GetMapping("/business-partners/{id}/eligibility")
    BusinessPartnerResponse eligibleById(@PathVariable("id") Long id, @RequestParam("role") String role);

    @GetMapping("/business-partners")
    List<BusinessPartnerResponse> findAll();

    @GetMapping("/business-partners/customers")
    List<BusinessPartnerResponse> findCustomers();

    @GetMapping("/business-partners/suppliers")
    List<BusinessPartnerResponse> findSuppliers();

    @GetMapping("/business-partners/search")
    List<BusinessPartnerResponse> findByName(@RequestParam("name") String name);
}
