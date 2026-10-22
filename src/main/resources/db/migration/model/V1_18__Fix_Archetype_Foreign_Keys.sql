ALTER TABLE archetype_ad_group_mapping
DROP FOREIGN KEY fk_archetype_ad_group;

ALTER TABLE archetype_ad_group_mapping
ADD CONSTRAINT fk_archetype_ad_group FOREIGN KEY (archetype_id) REFERENCES archetype(id) ON DELETE CASCADE;

ALTER TABLE archetype_ad_group_mapping
DROP FOREIGN KEY fk_ad_group_archetype;

ALTER TABLE archetype_ad_group_mapping
ADD CONSTRAINT fk_ad_group_archetype FOREIGN KEY (ad_group_id) REFERENCES ad_group(id) ON DELETE CASCADE;
