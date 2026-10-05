package br.com.hulysses.hulysses_one.businesspartner.integration.dto;

import java.util.List;
import java.util.Set;

/** HTTP contract. All partner business validation belongs to the remote application. */
public record BusinessPartnerRequest(String name, String document, String email, String phone,
                                     String type, Set<String> roles, List<AddressRequest> addresses, Boolean active) {}