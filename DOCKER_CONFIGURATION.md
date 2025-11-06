# Docker Configuration Guide

## Which Configuration File is Used?

When you run `docker compose up`, Spring Boot uses the following logic to determine which configuration file to load:

### Configuration Loading Order

1. **Base Configuration**: `application.yml` (always loaded)
2. **Profile-Specific**: `application-{profile}.yml` (loaded if profile is active)

### Current Setup

#### Scenario 1: `docker compose up` (Base docker-compose.yml)

```bash
docker compose up
```

**Result:**
- **No profile active** (default)
- Uses: **`application.yml`** only
- Database: File-based H2 (`jdbc:h2:file:/data/ordersdb`)
- Suitable for: Docker deployment without specific environment

**Configuration Files Loaded:**
- ✅ `application.yml` (base config with Docker database setup)

---

#### Scenario 2: `docker compose -f docker-compose.yml -f docker-compose.dev.yml up`

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

**Result:**
- **Profile active**: `dev`
- Uses: **`application.yml`** + **`application-dev.yml`**
- Database: In-memory H2 (`jdbc:h2:mem:ordersdb_dev`)
- Suitable for: Development environment

**Configuration Files Loaded:**
- ✅ `application.yml` (base config)
- ✅ `application-dev.yml` (overrides database to in-memory)

---

#### Scenario 3: `docker compose -f docker-compose.yml -f docker-compose.prod.yml up`

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up
```

**Result:**
- **Profile active**: `prod`
- Uses: **`application.yml`** + **`application-prod.yml`**
- Database: File-based H2 or PostgreSQL (configurable via env vars)
- Suitable for: Production environment

**Configuration Files Loaded:**
- ✅ `application.yml` (base config)
- ✅ `application-prod.yml` (production overrides)

---

## Configuration File Priority

When multiple configuration files are loaded, **profile-specific files override base configuration**:

```
application.yml (base)
    ↓
application-{profile}.yml (overrides)
    ↓
Environment Variables (highest priority)
```

### Example: Development Profile

1. `application.yml` sets:
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:file:/data/ordersdb
   ```

2. `application-dev.yml` overrides:
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:mem:ordersdb_dev  # This takes precedence
   ```

3. Environment variables (if set) override everything:
   ```bash
   DATABASE_URL=jdbc:postgresql://localhost/ordersdb
   ```

## Quick Reference

| Command | Profile | Files Used | Database |
|---------|---------|-----------|----------|
| `docker compose up` | None (default) | `application.yml` | File-based H2 |
| `docker compose -f docker-compose.yml -f docker-compose.dev.yml up` | `dev` | `application.yml` + `application-dev.yml` | In-memory H2 |
| `docker compose -f docker-compose.yml -f docker-compose.prod.yml up` | `prod` | `application.yml` + `application-prod.yml` | File-based H2 / PostgreSQL |

## Verifying Active Configuration

To verify which configuration is being used, check the application logs:

```bash
docker compose logs sales-order-api | grep "The following"
```

You should see:
- **No profile**: `No active profiles set.`
- **With profile**: `The following 1 profile is active: "dev"` (or "prod")

Also check the database URL in logs:
```bash
docker compose logs sales-order-api | grep "jdbc:h2"
```

## Recommended Usage

### For Development
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

### For Production
```bash
# Set environment variables first
export JWT_SECRET=your-secret-key
export DATABASE_URL=jdbc:postgresql://db:5432/ordersdb

docker compose -f docker-compose.yml -f docker-compose.prod.yml up
```

### For Simple Docker Testing
```bash
docker compose up
```
(Uses base `application.yml` with file-based H2)

## Summary

**Answer to your question:**

When you run `docker compose up` (without additional compose files):
- ✅ Uses **`application.yml`** (no profile active)
- ✅ Database: File-based H2 at `/data/ordersdb`
- ✅ Suitable for Docker deployment

If you want to use a specific profile, use the environment-specific compose files:
- Development: Add `-f docker-compose.dev.yml`
- Production: Add `-f docker-compose.prod.yml`

