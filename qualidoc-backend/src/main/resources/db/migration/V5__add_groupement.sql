-- V5 : Ajout du concept de groupement
-- Un groupement regroupe plusieurs établissements qui partagent leurs documents.

ALTER TABLE establishment ADD COLUMN groupement_id UUID NOT NULL DEFAULT uuid_generate_v4();

-- Groupement 1 : CHU de Toulouse, Clinique Saint-Jean, Hôpital de Rangueil
UPDATE establishment
SET groupement_id = '99999999-0000-0000-0000-000000000001'
WHERE id IN (
    '11111111-0000-0000-0000-000000000001',
    '11111111-0000-0000-0000-000000000002',
    '11111111-0000-0000-0000-000000000003'
);

-- Groupement 2 : EHPAD Les Pins, Clinique Pasteur
UPDATE establishment
SET groupement_id = '99999999-0000-0000-0000-000000000002'
WHERE id IN (
    '11111111-0000-0000-0000-000000000004',
    '11111111-0000-0000-0000-000000000005'
);

CREATE INDEX idx_establishment_groupement ON establishment(groupement_id);
