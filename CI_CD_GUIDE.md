# CI/CD Guide

## Overview

This guide explains how to set up Continuous Integration and Continuous Deployment (CI/CD) for the Sales Order Management API.

## CI/CD Pipeline Stages

### 1. Continuous Integration (CI)

#### Test Stage
- Runs all unit and integration tests
- Generates code coverage report
- Fails build if tests fail

#### Build Stage
- Compiles the application
- Packages as JAR file
- Builds Docker image
- Creates artifacts

#### Security Scan Stage
- Scans dependencies for vulnerabilities
- Checks for security issues
- Fails if critical vulnerabilities found

### 2. Continuous Deployment (CD)

#### Development Deployment
- Automatic deployment to dev environment
- Triggered on push to `develop` branch
- Includes smoke tests

#### Production Deployment
- Manual approval required
- Triggered on push to `main` branch
- Includes database migrations
- Blue-green deployment strategy

## GitHub Actions Setup

### Prerequisites

1. **Secrets Configuration**
   - Go to Repository → Settings → Secrets and variables → Actions
   - Add the following secrets:
     - `REGISTRY_URL`: Container registry URL
     - `REGISTRY_USERNAME`: Registry username
     - `REGISTRY_PASSWORD`: Registry password
     - `DEPLOYMENT_KEY`: SSH key for deployment (if needed)

### Workflow Files

The CI/CD pipeline is defined in `.github/workflows/ci-cd.yml`

### Manual Triggers

You can also trigger workflows manually:
1. Go to Actions tab
2. Select "CI/CD Pipeline"
3. Click "Run workflow"

## Local CI/CD Testing

### Run Tests Locally

```bash
# Run all tests
./mvnw clean test

# Run with coverage
./mvnw clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Build Docker Image Locally

```bash
# Build image
docker build -t sales-order-api:local .

# Test image
docker run -p 8080:8080 sales-order-api:local
```

### Security Scanning

```bash
# Install OWASP Dependency Check
# https://owasp.org/www-project-dependency-check/

# Run scan
dependency-check.sh --project sales-order-api --scan .
```

## Deployment Strategies

### 1. Docker Compose Deployment

**Development:**
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

**Production:**
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 2. Kubernetes Deployment

**Deployment YAML:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sales-order-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sales-order-api
  template:
    metadata:
      labels:
        app: sales-order-api
    spec:
      containers:
      - name: sales-order-api
        image: registry.example.com/sales-order-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: sales-order-secrets
              key: jwt-secret
```

### 3. Blue-Green Deployment

1. Deploy new version to "green" environment
2. Run smoke tests
3. Switch traffic from "blue" to "green"
4. Monitor for issues
5. Keep "blue" as backup

## Database Migrations in CI/CD

### Pre-Deployment Migrations

```bash
# Run migrations before deployment
liquibase --changeLogFile=db/changelog/db.changelog-master.xml \
          --url=$DATABASE_URL \
          --username=$DB_USER \
          --password=$DB_PASS \
          update
```

### Rollback Strategy

```bash
# Rollback if deployment fails
liquibase --changeLogFile=db/changelog/db.changelog-master.xml \
          --url=$DATABASE_URL \
          --username=$DB_USER \
          --password=$DB_PASS \
          rollback <previous-tag>
```

## Environment Configuration

### Development
- Profile: `dev`
- Auto-deploy on push to `develop`
- Less strict security checks

### Staging
- Profile: `test`
- Manual trigger or on release
- Full test suite
- Production-like configuration

### Production
- Profile: `prod`
- Manual approval required
- Full security scan
- Database backup before migration

## Monitoring Deployment

### Health Checks

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check database connectivity
curl http://localhost:8080/actuator/health/db
```

### Logs

```bash
# View application logs
docker compose logs -f sales-order-api

# View in Kubernetes
kubectl logs -f deployment/sales-order-api
```

### Metrics

- Application startup time
- Request latency
- Error rates
- Database connection pool status

## Rollback Procedures

### Quick Rollback

1. **Docker Compose:**
   ```bash
   docker compose down
   docker compose up -d --scale sales-order-api=0
   # Deploy previous version
   ```

2. **Kubernetes:**
   ```bash
   kubectl rollout undo deployment/sales-order-api
   ```

3. **Database:**
   ```bash
   liquibase rollback <previous-tag>
   ```

## Best Practices

1. **Always Test Locally First**
   - Run tests before pushing
   - Build Docker image locally
   - Test in local environment

2. **Small, Frequent Deployments**
   - Deploy small changes frequently
   - Easier to rollback
   - Faster feedback

3. **Database Migrations**
   - Always backup before migration
   - Test migrations in staging
   - Have rollback plan ready

4. **Monitoring**
   - Monitor after deployment
   - Set up alerts
   - Track key metrics

5. **Documentation**
   - Document deployment process
   - Keep runbooks updated
   - Document rollback procedures

## Troubleshooting

### Build Fails

1. Check test failures
2. Review compilation errors
3. Check dependency issues
4. Review GitHub Actions logs

### Deployment Fails

1. Check application logs
2. Verify environment variables
3. Check database connectivity
4. Review health endpoints

### Migration Fails

1. Check Liquibase logs
2. Verify database connection
3. Review changelog syntax
4. Check for conflicts

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Kubernetes Deployment Guide](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)

