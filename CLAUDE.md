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
                    - persistence/: Entidades JPA, mappers
                    - security/: EncryptedStringConverter (AES-256-GCM)
```

**Reglas Clave:**
- La capa de dominio debe tener cero dependencias de frameworks
- Todas las anotaciones Spring/JPA permanecen en la capa de infraestructura
- Los objetos de dominio usan factory methods: `Asset.create()`, `Asset.reconstitute()`
- Los value objects son inmutables y auto-validantes (lanzan excepción si input inválido)

## Perfiles

| Perfil | Base de Datos | SSH | Cuándo usar |
|--------|---------------|-----|-------------|
| `dev` | H2 en memoria | Mock | Iteración rápida, sin Docker |
| `demo` | PostgreSQL:5433 | Mock (realista) | Tests de integración, demos |
| `prod` | PostgreSQL (env vars) | Real | Despliegue en producción |

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

Usa JUnit 5 con Mockito. Los tests siguen el patrón:
- Clases de test anidadas con `@Nested` y `@DisplayName`
- Tests de servicio mockean la capa de repositorio
- Tests de dominio son unit tests puros (sin mocks)

## Variables de Entorno

Requeridas para demo/prod:
- `INFRATRACK_ENCRYPTION_KEY` - Clave AES de 32 bytes codificada en Base64

Solo para prod:
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`

## Decisiones de Diseño Importantes

- `JpaAssetRepository` NO tiene `@Repository` — se registra manualmente en `BeanConfiguration`
  para evitar bean duplicado. Spring lo instancia via `new JpaAssetRepository(springRepo)`.
- `EncryptedStringConverter` lee de `infratrack.encryption.key` (path en application.yml base).
  NO usar `infratrack.security.encryption.key` (path incorrecto en versiones anteriores).
- Sin DTOs por ahora — `AssetRestController` usa `@RequestParam`. Se refactorizará en Fase 3-4.
- `Credentials.toString()` omite el password deliberadamente (seguridad).
- Puerto PostgreSQL: 5433 (5432 ocupado por Docker Desktop interno).

## Problemas Conocidos Pendientes

- Serialización JSON: `id` e `ipAddress` se serializan como objetos `{value: ...}` en lugar
  de strings simples. Se resolverá con DTOs.
- `password` aparece en respuestas API. Se resolverá con DTOs de respuesta.

## Problema Activo — Sesión Actual

Hibernate no crea la tabla `assets` con `ddl-auto: update` en PostgreSQL vacío.
Con `ddl-auto: create` sí funciona. Con `update` los logs no muestran ningún DDL.
Conexión HikariCP funciona correctamente. Clave de cifrado correcta (32 bytes verificados).
Objetivo: conseguir que `ddl-auto: update` funcione, o implementar el schema SQL manualmente.
