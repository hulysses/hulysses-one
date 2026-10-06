package br.com.hulysses.business_partner_service.businesspartner.batch;

public record BusinessPartnerCsvRow(String name, String document, String email, String phone,
                                    String type, String roles) { }
