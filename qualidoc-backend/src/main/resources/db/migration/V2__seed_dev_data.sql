-- QualiDoc — Données de développement
-- Migration V2 : établissements et utilisateurs de test

-- Établissements de test
INSERT INTO establishment (id, name, code, active) VALUES
    ('11111111-0000-0000-0000-000000000001', 'CHU de Toulouse', 'CHU-TLS', true),
    ('11111111-0000-0000-0000-000000000002', 'Clinique Saint-Jean', 'CSJ-TLS', true),
    ('11111111-0000-0000-0000-000000000003', 'Hôpital de Rangueil', 'HOP-RAN', true),
    ('11111111-0000-0000-0000-000000000004', 'EHPAD Les Pins', 'EHP-PIN', true),
    ('11111111-0000-0000-0000-000000000005', 'Clinique Pasteur', 'CLI-PAS', true);

-- Utilisateurs de test (les UUIDs correspondent aux subs Keycloak à configurer)
INSERT INTO app_user (id, establishment_id, email, first_name, last_name, role) VALUES
    ('22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001',
     'editor@chu-toulouse.fr', 'Marie', 'Dupont', 'EDITOR'),
    ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001',
     'reader@chu-toulouse.fr', 'Jean', 'Martin', 'READER'),
    ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000002',
     'qualite@clinique-saintjean.fr', 'Sophie', 'Bernard', 'EDITOR'),
    ('22222222-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000003',
     'reader@rangueil.fr', 'Pierre', 'Leblanc', 'READER');
