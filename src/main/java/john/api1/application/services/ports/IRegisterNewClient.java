package john.api1.application.services.ports;

import john.api1.application.components.DomainResponse;
import john.api1.application.dto.request.RegisterRequestDTO;

public interface IRegisterNewClient {
    DomainResponse<String> registerNewClient(RegisterRequestDTO request);
}
