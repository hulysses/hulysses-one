package br.com.hulysses.business_partner_service;

import org.springframework.boot.SpringApplication;

/** Runs the real service in a separate JVM for the principal application's HTTP tests. */
public class StandaloneTestService {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BusinessPartnerServiceApplication.class);
        app.setAdditionalProfiles("test");
        app.addInitializers(new TestDatabaseInitializer());
        app.run(args);
    }
}
