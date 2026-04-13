-- QualiDoc — Mots de passe de développement (BCrypt strength 12)
-- Mot de passe commun : qualidoc-dev-2026
-- NE PAS exécuter en production

UPDATE app_user SET password_hash = '$2a$12$J5WhrTGl0rMmN/tmYO7bmOuS4axPYOYo4UeyyXFA0s7pU1iHPEnZq'
WHERE id = '22222222-0000-0000-0000-000000000001';

UPDATE app_user SET password_hash = '$2a$12$30GLTufXDKHYu.h2HdsFUeANutDVYcrUtJGFMHVwTLuJh.fvRGIFK'
WHERE id = '22222222-0000-0000-0000-000000000002';

UPDATE app_user SET password_hash = '$2a$12$kXvLixOAgP0ubuSZezHS0Ogd8n1Xx.A8630IQkPEYEHZ1wOeYRlle'
WHERE id = '22222222-0000-0000-0000-000000000003';

UPDATE app_user SET password_hash = '$2a$12$liqjUSVmLk6AtqG1UDeBhuxQ9k.4rbv/CPC3bDj2pkx00Fv36fisG'
WHERE id = '22222222-0000-0000-0000-000000000004';
