package dk.digitalidentity.OS2ilm.service.cms;

import dk.digitalidentity.OS2ilm.model.dao.CmsMessageDao;
import dk.digitalidentity.OS2ilm.model.entity.CmsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EnableScheduling
@Service
@RequiredArgsConstructor
public class CmsMessageService {
    private final CmsMessageDao cmsMessageDao;
    private LocalDateTime lastCheckedForUpdates = LocalDateTime.now();

    private Map<String, String> cmsMap = new HashMap<>();

    public CmsMessage save(CmsMessage cmsMessage) {
        return cmsMessageDao.save(cmsMessage);
    }

    public CmsMessage getByCmsKey(String key) {
        return cmsMessageDao.findByCmsKey(key);
    }

    public void deleteById(long id) {
        cmsMessageDao.deleteById(id);
    }

    public Map<String, String> getCmsMap() {
        return cmsMap;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void updateCmsMap() {
        if (cmsMap == null || cmsMap.isEmpty()) {
            cmsMap = cmsMessageDao.findAll().stream()
                    .filter(cmsMessage -> cmsMessage.getCmsValue() != null)
                    .collect(Collectors.toMap(CmsMessage::getCmsKey, CmsMessage::getCmsValue));
        }
        else {
            List<CmsMessage> cmsMessages = cmsMessageDao.findAllByLastUpdatedAfter(lastCheckedForUpdates.minusMinutes(1L));
            lastCheckedForUpdates = LocalDateTime.now();

            // any changes?
            if (!cmsMessages.isEmpty()) {
                Map<String, String> newMap = new HashMap<>(cmsMap);

                for (CmsMessage cmsMessage : cmsMessages) {
                    if (cmsMessage.getCmsValue() != null) {
                        newMap.put(cmsMessage.getCmsKey(), cmsMessage.getCmsValue());
                    }
                    else {
                        newMap.remove(cmsMessage.getCmsKey());
                    }
                }

                cmsMap = newMap;
            }
        }
    }
}
