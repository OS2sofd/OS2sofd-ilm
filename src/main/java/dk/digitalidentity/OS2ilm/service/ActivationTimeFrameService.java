package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.ActivationTimeFrameDao;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.dto.ActivationTimeFrameDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivationTimeFrameService {
	private final ActivationTimeFrameDao activationTimeFrameDao;

	public ActivationTimeFrame save(ActivationTimeFrame activationTimeFrame) {
		return activationTimeFrameDao.save(activationTimeFrame);
	}

	public List<ActivationTimeFrame> findAll() {
		return activationTimeFrameDao.findAll();
	}

	public void delete(ActivationTimeFrame activationTimeFrame) {
		activationTimeFrameDao.delete(activationTimeFrame);
	}

	@Transactional
	public ActivationTimeFrame findById(Long id) {
		return activationTimeFrameDao.findById(id).orElse(null);
	}

	public List<ActivationTimeFrameDTO> formatActivations(List<ActivationTimeFrame> activationTimeFrames) {
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

		List<ActivationTimeFrame> activations = activationTimeFrames;
		List<ActivationTimeFrameDTO> formattedActivations = new ArrayList<>();
		for (ActivationTimeFrame activation : activations) {
			String start = activation.getStartTime().format(timeFormatter);
			String stop = activation.getStopTime().format(timeFormatter);
			ActivationTimeFrameDTO activationTimeFrameDTO = new ActivationTimeFrameDTO();
			activationTimeFrameDTO.setId(activation.getId());
			activationTimeFrameDTO.setExcludeWeekends(activation.isExcludeWeekends());
			activationTimeFrameDTO.setFormattedStartTime(start);
			activationTimeFrameDTO.setFormattedStopTime(stop);
			formattedActivations.add(activationTimeFrameDTO);
		}

		return formattedActivations;
	}
}
