-- QualiDoc — Schéma initial
-- Migration V1 : tables de base

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Établissements
CREATE TABLE establishment (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Utilisateurs
CREATE TABLE app_user (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    establishment_id   UUID         NOT NULL REFERENCES establishment(id),
    email              VARCHAR(255) NOT NULL UNIQUE,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    role               VARCHAR(20)  NOT NULL CHECK (role IN ('READER', 'EDITOR')),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Documents
CREATE TABLE document (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title               VARCHAR(500) NOT NULL,
    type                VARCHAR(50)  NOT NULL CHECK (type IN ('PROCEDURE', 'FORM', 'AWARENESS_BOOKLET')),
    uploader_id         UUID         NOT NULL REFERENCES app_user(id),
    establishment_id    UUID         NOT NULL REFERENCES establishment(id),
    storage_key         VARCHAR(500) NOT NULL UNIQUE,
    original_filename   VARCHAR(500) NOT NULL,
    mime_type           VARCHAR(100) NOT NULL,
    size_bytes          BIGINT       NOT NULL,
    version             INT          NOT NULL DEFAULT 1,
    published           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Partages inter-établissements
CREATE TABLE document_share (
    id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id              UUID        NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    source_establishment_id  UUID        NOT NULL REFERENCES establishment(id),
    target_establishment_id  UUID        NOT NULL REFERENCES establishment(id),
    shared_by_user_id        UUID        NOT NULL REFERENCES app_user(id),
    shared_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, target_establishment_id)
);

-- Journal d'audit (RGPD / HDS)
CREATE TABLE audit_log (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID        NOT NULL REFERENCES app_user(id),
    document_id  UUID        REFERENCES document(id) ON DELETE SET NULL,
    action       VARCHAR(50) NOT NULL,
    details      TEXT,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Notifications
CREATE TABLE notification (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id      UUID        NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    recipient_email  VARCHAR(255) NOT NULL,
    subject          VARCHAR(500) NOT NULL,
    body             TEXT         NOT NULL,
    sent             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Index pour les performances
CREATE INDEX idx_document_establishment ON document(establishment_id);
CREATE INDEX idx_document_created_at    ON document(created_at DESC);
CREATE INDEX idx_share_target           ON document_share(target_establishment_id);
CREATE INDEX idx_audit_document         ON audit_log(document_id);
CREATE INDEX idx_audit_user             ON audit_log(user_id);
CREATE INDEX idx_notification_sent      ON notification(sent) WHERE sent = FALSE;
