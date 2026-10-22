package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.LeaderDao;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.Leader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderService {
	private final LeaderDao leaderDao;
	private final BusinessService businessService;

	public Leader findByMitIdUuid(String mitIdUuid) {
		return leaderDao.findByMitIdUuid(mitIdUuid);
	}

	public Leader findByMitIdUuidWithBusinesses(String mitIdUuid) {
		return leaderDao.findByMitIdUuidWithBusinesses(mitIdUuid);
	}

	public Leader findByCpr(String cpr) {
		return leaderDao.findByCpr(cpr);
	}

	public Leader findByCprWithBusinesses(String cpr) {
		return leaderDao.findByCprWithBusinesses(cpr);
	}

	public Leader findByInternalUuid(String internalUuid) {
		return leaderDao.findByInternalUuid(internalUuid);
	}

	public Leader findByInternalUuidWithBusinesses(String internalUuid) {
		return leaderDao.findByInternalUuidWithBusinesses(internalUuid);
	}

	public Leader findByAnyIdentifier(String mitIdUuid, String cpr, String internalUuid) {
		if (mitIdUuid != null) {
			Leader leader = findByMitIdUuid(mitIdUuid);
			if (leader != null) {
				return leader;
			}
		}
		if (cpr != null) {
			Leader leader = findByCpr(cpr);
			if (leader != null) {
				return leader;
			}
		}
		if (internalUuid != null) {
			return findByInternalUuid(internalUuid);
		}
		return null;
	}

	public Optional<Leader> findByIdWithBusinesses(long id) {
		return leaderDao.findByIdWithBusinesses(id);
	}

	public List<String> getLeaderCvrs(Leader leader) {
		return leader.getBusinesses().stream()
				.map(Business::getCvr)
				.toList();
	}

	public void syncBusinessesFromCvrs(Leader leader, List<String> cvrs) {
		List<Business> businesses = businessService.findByCvrIn(cvrs);

		List<String> matchedCvrs = businesses.stream().map(Business::getCvr).toList();
		List<String> unmatchedCvrs = cvrs.stream().filter(cvr -> !matchedCvrs.contains(cvr)).toList();
		if (!unmatchedCvrs.isEmpty()) {
			log.warn("Leader {} - CVRs from token did not match any known business: {}", leader.getIdentifier(), unmatchedCvrs);
		}

		leader.setBusinesses(new HashSet<>(businesses));
	}

	public Leader save(Leader leader) {
		return leaderDao.save(leader);
	}

	public List<Leader> findAll() {
		return leaderDao.findAll();
	}

	public List<Leader> findAllWithBusinesses() {
		return leaderDao.findAllWithBusinesses();
	}

	public Optional<Leader> findById(long id) {
		return leaderDao.findById(id);
	}

	public void delete(Leader leader) {
		leaderDao.delete(leader);
	}

	public List<Leader> findByBusinessId(long businessId) {
		return leaderDao.findByBusinessId(businessId);
	}
}
