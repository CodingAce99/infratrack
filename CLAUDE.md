# CLAUDE.md

Este archivo proporciona orientación a Claude Code (claude.ai/code) para trabajar con el código de este repositorio.

## Comandos de Build y Ejecución

```bash
# Ejecutar con perfil dev (H2 en memoria, sin Docker)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ejecutar con perfil demo (PostgreSQL + mock SSH)
docker-compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# Construir JAR
./mvnw clean package

# Ejecutar todos los tests
./mvnw test

# Ejecutar clase de test específica
./mvnw test -Dtest=AssetServiceTest

# Saltar tests durante build
./mvnw clean package -DskipTests
```

## Arquitectura

**Arquitectura Hexagonal (Puertos y Adaptadores)** - El código impone límites estrictos entre capas:

```
domain/           → Java puro, sin frameworks. Contiene entidad Asset, value objects
                    (AssetId, IpAddress, Credentials) y lógica de dominio.

application/      → Casos de uso y puertos. Sin Spring. Contiene:
                    - port/input/: ManageAssetUseCase (lo que la app puede hacer)
                    - port/output/: AssetRepository (lo que la app necesita)
                    - service/: AssetService (implementación del caso de uso)

infrastructure/   → Spring Boot, JPA, REST. Contiene:
                    - adapter/input/: AssetRestController
                    - adapter/output/: JpaAssetRepository, InMemoryAssetRepository
                    - config/: BeanConfiguration (wiring de beans por perfil)
                    - persistence/: AssetJpaEntity, AssetMapper (Domain↔JPA), schema.sql
                    - security/: EncryptedStringConverter (AES-256-GCM)
```

**Reglas Clave:**
- La capa de dominio tiene cero dependencias de frameworks (bloqueado)
- Todas las anotaciones Spring/JPA permanecen en la capa de infraestructura
- Los objetos de dominio usan factory methods: `Asset.create()`, `Asset.reconstitute()`
- Los value objects son inmutables y auto-validantes (lanzan excepción si input inválido)
- `AssetService` y objetos de dominio se instancian via `BeanConfiguration` (no `@Service`)
- `JpaAssetRepository` tiene `@Repository` pero NO se registra con `@Bean` para evitar bean duplicado
- `InMemoryAssetRepository` para perfil `dev`; `JpaAssetRepository` para `demo`/`prod`

## Perfiles

| Perfil | Base de Datos | SSH | DDL | Cuándo usar |
|--------|---------------|-----|-----|-------------|
| `dev` | H2 en memoria | Mock | `create-drop` | Iteración rápida, sin Docker |
| `demo` | PostgreSQL:5433 | Mock (realista) | `validate` | Tests de integración, demos |
| `prod` | PostgreSQL (env vars) | Real | `validate` | Despliegue en producción |

El schema de la tabla `assets` se define en `src/main/resources/schema.sql` (gestionado manualmente).
`ddl-auto: validate` (base) + `ddl-auto: create-drop` (perfil dev).

## Endpoints de la API

Todos en `/api/v1/assets`:
- `GET /` - Listar todos los assets
- `GET /{id}` - Obtener por ID
- `POST /` - Crear asset
- `PUT /{id}/status` - Actualizar estado
- `PUT /{id}/credentials` - Actualizar credenciales SSH
- `PUT /{id}/ip` - Actualizar dirección IP
- `DELETE /{id}` - Eliminar asset

## Testing

51 tests pasando. Usa JUnit 5 con Mockito. Los tests siguen el patrón:
- Clases de test anidadas con `@Nested` y `@DisplayName`
- Tests de servicio mockean la capa de repositorio
- Tests de dominio son unit tests puros (sin mocks)

## Variables de Entorno

Requeridas para demo/prod:
- `INFRATRACK_ENCRYPTION_KEY` - Clave AES de 32 bytes codificada en Base64

Solo para prod:
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`

## Decisiones de Diseño Importantes

- `JpaAssetRepository` tiene `@Repository` para que Spring Boot autoconfigure el `SpringDataAssetRepository`
  subyacente, pero NO se registra con `@Bean` en `BeanConfiguration` — se instancia manualmente via
  `new JpaAssetRepository(springRepo)` para evitar bean duplicado.
- `EncryptedStringConverter` lee de `infratrack.encryption.key` (path en application.yml base).
  NO usar `infratrack.security.encryption.key` (path incorrecto en versiones anteriores).
- Sin DTOs todavía — `AssetRestController` usa `@RequestParam`. Se refactorizará en Sprint 3.1.
- `Credentials.toString()` omite el password deliberadamente (seguridad).
- Puerto PostgreSQL: 5433 (5432 ocupado por Docker Desktop interno).
- Schema SQL gestionado manualmente (`schema.sql`) en lugar de dejar que Hibernate lo genere;
  `ddl-auto: update` se descartó porque Hibernate no emitía DDL en PostgreSQL vacío.

## Estado del Proyecto

### Completado
- **Fase 2 completa**: CRUD de Asset end-to-end con perfil demo
- PostgreSQL 17 en Docker (puerto 5433)
- Cifrado AES-256-GCM via `EncryptedStringConverter` con schema.sql manual
- Dos mappers: `AssetMapper` (Domain↔JPA) en la capa de persistencia
- 51 tests pasando

### Problemas Conocidos (a resolver en Sprint 3.1)

- El controlador usa `@RequestParam` en lugar de `@RequestBody`
- El objeto de dominio `Asset` se devuelve directamente desde el controlador (sin capa DTO)
- `id` e `ipAddress` se serializan como objetos `{value: ...}` en lugar de strings simples
- `password` aparece en respuestas de la API (brecha de seguridad)

### Próximo: Sprint 3.1 — Implementación de DTOs

- 4 Request DTOs con Bean Validation: `CreateAssetRequest`, `UpdateStatusRequest`,
  `UpdateCredentialsRequest`, `UpdateIpAddressRequest`
- `AssetResponse` (sin campo password — seguridad por construcción)
- `AssetDtoMapper` en `infrastructure/adapter/input/dto/`
- Refactorizar `AssetRestController` para usar `@RequestBody` y los nuevos DTOs
- Tests que verifiquen que el password nunca aparece en ninguna respuesta
