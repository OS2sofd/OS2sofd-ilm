package dk.digitalidentity.OS2ilm.model.entity;

import dk.digitalidentity.OS2ilm.model.entity.enums.CmsStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "cms_message")
public class CmsMessage {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String cmsKey;

    @Column
    private String cmsValue;

	@Column
	private boolean active = true;

	@Column
	@Enumerated(EnumType.STRING)
	private CmsStatus cmsStatus = CmsStatus.ACTIVE;

    @Column
    private LocalDateTime lastUpdated;
}
