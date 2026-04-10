# QualiDoc — Configuration Keycloak

## Prérequis

Keycloak doit être démarré et accessible sur `https://keycloak.qualidoc.fr`.
Se connecter avec les credentials admin définis dans `config/secrets.yaml` :
- `KC_BOOTSTRAP_ADMIN_USERNAME`
- `KC_BOOTSTRAP_ADMIN_PASSWORD`

---

## 1. Créer le Realm

1. Cliquer sur le menu déroulant en haut à gauche (affiche `master`)
2. **Create realm**
3. Realm name : `qualidoc`
4. **Create**

---

## 2. Créer le Client

1. **Clients** → **Create client**
2. Renseigner :

| Champ | Valeur |
|---|---|
| Client type | `OpenID Connect` |
| Client ID | `qualidoc-frontend` |

3. **Next** → activer **Standard flow**
4. **Next** → renseigner :

| Champ | Valeur |
|---|---|
| Valid redirect URIs | `https://qualidoc.fr/*` |
| Valid post logout redirect URIs | `https://qualidoc.fr/*` |
| Web origins | `https://qualidoc.fr` |

5. **Save**

---

## 3. Déclarer l'attribut establishment_id

Cet attribut lie chaque utilisateur Keycloak à un établissement en base de données.

1. **Realm settings** → onglet **User profile** → **Add attribute**

| Champ | Valeur |
|---|---|
| Name | `establishment_id` |
| Display name | `Establishment ID` |
| Enabled when | Always |

2. **Save**

---

## 4. Créer le Mapper JWT

Ce mapper injecte `establishment_id` dans le token JWT pour que le backend puisse l'utiliser.

1. **Clients** → `qualidoc-frontend` → onglet **Client scopes**
2. Cliquer sur le lien `qualidoc-frontend-dedicated`
3. **Add mapper** → **By configuration** → **User Attribute**

| Champ | Valeur |
|---|---|
| Name | `establishment_id` |
| User Attribute | `establishment_id` |
| Token Claim Name | `establishment_id` |
| Add to ID token | ON |
| Add to access token | ON |

4. **Save**

---

## 5. Créer les Rôles

1. **Realm roles** → **Create role**
2. Créer les deux rôles suivants :

| Role name | Usage |
|---|---|
| `EDITOR` | Upload et suppression de documents |
| `VIEWER` | Lecture seule |

---

## 6. Créer les Utilisateurs

Pour chaque utilisateur :

### 6.1 Récupérer l'UUID de l'établissement en base

```bash
sudo kubectl exec -it statefulset/postgres -n qualidoc -- \
  psql -U qualidoc -d qualidoc -c "SELECT id, name FROM establishment;"
```

### 6.2 Créer l'utilisateur

1. **Users** → **Add user**

| Champ | Valeur |
|---|---|
| Username | ex. `marie.dupont` |
| Email | ex. `marie.dupont@chu-toulouse.fr` |
| First name | ex. `Marie` |
| Last name | ex. `Dupont` |
| Email verified | ON |

2. **Create**

### 6.3 Définir le mot de passe

1. Onglet **Credentials** → **Set password**
2. Renseigner le mot de passe
3. Désactiver **Temporary** si l'utilisateur ne doit pas le changer à la première connexion
4. **Save**

### 6.4 Assigner un rôle

1. Onglet **Role mapping** → **Assign role**
2. Choisir `EDITOR` ou `VIEWER`
3. **Assign**

### 6.5 Associer l'établissement

1. Onglet **Attributes**
2. Ajouter :

| Key | Value |
|---|---|
| `establishment_id` | UUID de l'établissement (récupéré à l'étape 6.1) |

3. **Save**

---

## Récapitulatif des établissements (données initiales)

Insérés par la migration `V2__seed_dev_data.sql` :

| UUID | Nom | Code |
|---|---|---|
| `11111111-0000-0000-0000-000000000001` | CHU de Toulouse | CHU-TLS |
| `11111111-0000-0000-0000-000000000002` | Clinique Saint-Jean | CSJ-TLS |
| `11111111-0000-0000-0000-000000000003` | Hôpital de Rangueil | HOP-RAN |
| `11111111-0000-0000-0000-000000000004` | EHPAD Les Pins | EHP-PIN |
| `11111111-0000-0000-0000-000000000005` | Clinique Pasteur | CLI-PAS |

---

## Vérification

Une fois un utilisateur configuré, tester la connexion sur `https://qualidoc.fr`.

Le token JWT doit contenir :
- `realm_access.roles` → `["EDITOR"]` ou `["VIEWER"]`
- `establishment_id` → UUID de l'établissement
