package br.com.hulysses.hulysses_one.businesspartner.application;

import br.com.hulysses.hulysses_one.businesspartner.integration.BusinessPartnerClient;
import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerRequest;
import br.com.hulysses.hulysses_one.businesspartner.integration.dto.BusinessPartnerResponse;
import br.com.hulysses.hulysses_one.shared.exception.ExternalServiceUnavailableException;
import br.com.hulysses.hulysses_one.shared.exception.RemotePartnerRequestException;
import br.com.hulysses.hulysses_one.shared.exception.ApiError;
import feign.FeignException;
import feign.codec.DecodeException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.function.Supplier;

/** Coordinates the remote API; partner business rules exist only in the partner application. */
@Service
public class BusinessPartnerService {
    private final BusinessPartnerClient client;
    private final ObjectMapper mapper;

    public BusinessPartnerService(BusinessPartnerClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public BusinessPartnerResponse create(BusinessPartnerRequest request) {
        return invoke(() -> client.create(request));
    }

    public BusinessPartnerResponse update(Long id, BusinessPartnerRequest request) {
        return invoke(() -> client.update(id, request));
    }

    public void delete(Long id) {
        invoke(() -> { client.delete(id); return null; });
    }

    public BusinessPartnerResponse getById(Long id) { return invoke(() -> client.getById(id)); }
    public BusinessPartnerResponse findSupplierById(Long id) {
        return invoke(() -> client.eligibleById(id, "SUPPLIER"));
    }
    public BusinessPartnerResponse findCustomerById(Long id) {
        return invoke(() -> client.eligibleById(id, "CUSTOMER"));
    }
    public List<BusinessPartnerResponse> findAll() { return invoke(client::findAll); }
    public List<BusinessPartnerResponse> findCustomers() { return invoke(client::findCustomers); }
    public List<BusinessPartnerResponse> findSuppliers() { return invoke(client::findSuppliers); }
    public List<BusinessPartnerResponse> findByName(String name) { return invoke(() -> client.findByName(name)); }

    private <T> T invoke(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DecodeException exception) {
            throw new ExternalServiceUnavailableException(exception);
        } catch (FeignException exception) {
            // Only a recognized public ApiError can represent a business rejection.
            // A proxy's 404/HTML page or incompatible response is a communication failure.
            ApiError error = readError(exception);
            if (error != null && error.status() == exception.status()
                    && (error.status() == 400 || error.status() == 404 || error.status() == 409)) {
                throw new RemotePartnerRequestException(error.status(), error.fields());
            }
            throw new ExternalServiceUnavailableException(exception);
        }
    }

    private ApiError readError(FeignException exception) {
        try {
            return mapper.readValue(exception.contentUTF8(), ApiError.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
