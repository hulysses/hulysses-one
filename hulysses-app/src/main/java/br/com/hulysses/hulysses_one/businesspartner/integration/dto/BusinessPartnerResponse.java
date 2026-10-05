package br.com.hulysses.hulysses_one.businesspartner.integration.dto;

import java.util.List;
import java.util.Set;

public record BusinessPartnerResponse(Long id, String name, String document, String email, String phone,
                                      Boolean active, String type, List<AddressResponse> addresses, Set<String> roles) {}