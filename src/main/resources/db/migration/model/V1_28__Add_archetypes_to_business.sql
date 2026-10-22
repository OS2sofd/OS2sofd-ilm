CREATE TABLE business_archetypes (
    business_id             BIGINT NOT NULL,
    archetype_id            BIGINT NOT NULL,

    PRIMARY KEY (business_id, archetype_id),
    CONSTRAINT fk_business_archetype FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE,
    CONSTRAINT fk_archetype_business FOREIGN KEY (archetype_id) REFERENCES archetype(id) ON DELETE CASCADE
);
