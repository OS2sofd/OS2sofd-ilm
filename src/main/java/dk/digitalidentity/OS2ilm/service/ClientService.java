package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.ClientDao;
import dk.digitalidentity.OS2ilm.model.entity.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientDao clientDao;

	public List<Client> findAll() {
		return clientDao.findAll();
	}

	// note - never reloaded, as the ApiKey is not intended to change
	@Cacheable(value = "clientCache")
	public Client findByApiKey(String apiKey) {
		return clientDao.findByApiKey(apiKey);
	}

	public void save(Client client) {
		clientDao.save(client);
	}
}
