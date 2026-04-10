-- Migration V4 : séparation du type PROCEDURE en PROCEDURE et PROTOCOL

ALTER TABLE document DROP CONSTRAINT IF EXISTS document_type_check;

ALTER TABLE document
    ADD CONSTRAINT document_type_check
    CHECK (type IN ('PROCEDURE', 'PROTOCOL', 'FORM', 'AWARENESS_BOOKLET'));
