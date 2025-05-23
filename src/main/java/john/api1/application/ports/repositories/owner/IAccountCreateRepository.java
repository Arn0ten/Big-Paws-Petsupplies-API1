package john.api1.application.ports.repositories.owner;

import john.api1.application.domain.models.ClientAccountDomain;
import john.api1.application.domain.models.ClientDomain;

public interface IAccountCreateRepository {
    String createNewClient(ClientAccountDomain newAccount, ClientDomain newClient);
}
