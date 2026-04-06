-- V6 : Arborescence de dossiers par groupement

CREATE TABLE folder (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name          VARCHAR(255) NOT NULL,
    parent_id     UUID REFERENCES folder(id) ON DELETE CASCADE,
    groupement_id UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE document ADD COLUMN folder_id UUID REFERENCES folder(id) ON DELETE SET NULL;

CREATE INDEX idx_folder_groupement ON folder(groupement_id);
CREATE INDEX idx_folder_parent     ON folder(parent_id);
CREATE INDEX idx_document_folder   ON document(folder_id);
