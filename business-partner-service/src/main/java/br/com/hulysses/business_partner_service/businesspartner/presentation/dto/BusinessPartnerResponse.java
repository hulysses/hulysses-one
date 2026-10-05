package br.com.hulysses.business_partner_service.businesspartner.presentation.dto;

import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartner;
import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerRole;
import br.com.hulysses.business_partner_service.businesspartner.domain.BusinessPartnerType;
import java.util.List;
import java.util.Set;

public record BusinessPartnerResponse(Long id, String name, String document, String email, String phone,
                                      Boolean active, BusinessPartnerType type, List<AddressResponse> addresses,
                                      Set<BusinessPartnerRole> roles) {
    public static BusinessPartnerResponse from(BusinessPartner partner) {
        return new BusinessPartnerResponse(partner.getId(), partner.getName(), partner.getDocument(),
                partner.getEmail(), partner.getPhone(), partner.getActive(), partner.getType(),
                partner.getAddresses().stream().map(AddressResponse::from).toList(), Set.copyOf(partner.getRoles()));
    }
}
