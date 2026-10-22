package dk.digitalidentity.OS2ilm.service.cms;

import dk.digitalidentity.OS2ilm.model.entity.CmsMessage;
import dk.digitalidentity.OS2ilm.model.entity.enums.CmsProperty;
import dk.digitalidentity.OS2ilm.model.entity.enums.CmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CmsMessageBundle {
    private final CmsMessageService cmsMessageService;
    private final CmsMessageSource messageSource;

    public String getText(String key) {
        return getText(key, false);
    }

    public String getText(String key, boolean bypassCache) {
        String value = null;

        if (bypassCache) {
            CmsMessage cmsMessage = cmsMessageService.getByCmsKey(key);
            if (cmsMessage != null) {
                value = cmsMessage.getCmsValue();
            }
        }
        else {
            value = cmsMessageService.getCmsMap().get(key);
        }

        if (value == null) {
            value = messageSource.getMessage(key, null, "", null);
        }

        return value;
    }

	public boolean getActive(String key) {
		boolean result = true;
		CmsMessage cmsMessage = cmsMessageService.getByCmsKey(key);
		if (cmsMessage != null) {
			result = cmsMessage.isActive();
		}
		return result;
	}

    public record CmsMessageOutput(String key, String description, boolean active) { }
    
    public List<CmsMessageOutput> getAll() {
        List<CmsMessageOutput> all = new ArrayList<>();
        for (CmsProperty value : CmsProperty.values()) {
            all.add(new CmsMessageOutput(value.getKey(), value.getDescription(), getActive(value.getKey())));
        }

        return all;
    }

	public List<CmsMessageOutput> getByType(CmsType cmsType) {
		List<CmsMessageOutput> all = new ArrayList<>();
		for (CmsProperty value : CmsProperty.values()) {
			if (value.getCmsType().equals(cmsType)) {
				all.add(new CmsMessageOutput(value.getKey(), value.getDescription(), getActive(value.getKey())));
			}
		}
		return all;
	}

    public String getDescription(String key) {
		return Arrays.stream(CmsProperty.values())
                .filter(cmsProperty -> Objects.equals(cmsProperty.getKey(), key))
                .map(CmsProperty::getDescription)
                .findAny()
                .orElseThrow();
    }
}
