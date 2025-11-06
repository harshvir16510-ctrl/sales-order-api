# Database Migration Guide

## Overview

This project uses **Liquibase** for database schema version control and migrations. Liquibase ensures consistent database schemas across all environments (development, testing, production).

## Migration Files Location

```
src/main/resources/db/changelog/
├── db.changelog-master.xml          # Master changelog (includes all changes)
└── changes/
    └── 001-create-tables.xml        # Initial schema creation
```

## How Liquibase Works

1. **Changelog Files**: XML files describing database changes
2. **ChangeSets**: Individual changes (create table, add column, etc.)
3. **Database Tracking**: Liquibase tracks applied changes in `DATABASECHANGELOG` table
4. **Automatic Execution**: Runs on application startup

## Current Schema

### Tables Created

1. **customer**
   - `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
   - `name` (VARCHAR(255), NOT NULL)

2. **catalog_item**
   - `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
   - `sku` (VARCHAR(100), NOT NULL, UNIQUE)
   - `name` (VARCHAR(255), NOT NULL)
   - `price` (DECIMAL(19,2), NOT NULL)
   - `updated_at` (TIMESTAMP, NOT NULL)

3. **sales_order**
   - `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
   - `order_reference` (VARCHAR(36), NOT NULL, UNIQUE)
   - `customer_id` (BIGINT, NOT NULL, FK to customer.id)
   - `subtotal` (DECIMAL(19,2), NOT NULL)
   - `vat` (DECIMAL(19,2), NOT NULL)
   - `total` (DECIMAL(19,2), NOT NULL)
   - `created_at` (TIMESTAMP, NOT NULL)
   - `cancelled_at` (TIMESTAMP, NULL)
   - `status` (VARCHAR(50), NOT NULL)
   - `version` (BIGINT, NOT NULL) - For optimistic locking

4. **order_item**
   - `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
   - `order_id` (BIGINT, NOT NULL, FK to sales_order.id)
   - `catalog_item_id` (BIGINT, NOT NULL)
   - `item_name` (VARCHAR(255), NOT NULL)
   - `item_price` (DECIMAL(19,2), NOT NULL)
   - `quantity` (INTEGER, NOT NULL)
   - `total_price` (DECIMAL(19,2), NOT NULL)

## Creating New Migrations

### Step 1: Create New Changelog File

Create a new file in `src/main/resources/db/changelog/changes/`:

```xml
<!-- 002-add-indexes.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="002-add-indexes" author="developer">
        <createIndex indexName="idx_sales_order_created_at"
                     tableName="sales_order">
            <column name="created_at"/>
        </createIndex>
        
        <createIndex indexName="idx_sales_order_customer_id"
                     tableName="sales_order">
            <column name="customer_id"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

### Step 2: Include in Master Changelog

Update `db.changelog-master.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/changes/001-create-tables.xml"/>
    <include file="db/changelog/changes/002-add-indexes.xml"/>

</databaseChangeLog>
```

## Common Migration Operations

### Add a Column

```xml
<changeSet id="003-add-column" author="developer">
    <addColumn tableName="sales_order">
        <column name="notes" type="VARCHAR(500)"/>
    </addColumn>
</changeSet>
```

### Modify Column

```xml
<changeSet id="004-modify-column" author="developer">
    <modifyDataType tableName="customer" 
                    columnName="name" 
                    newDataType="VARCHAR(500)"/>
</changeSet>
```

### Add Foreign Key

```xml
<changeSet id="005-add-fk" author="developer">
    <addForeignKeyConstraint
            baseTableName="order_item"
            baseColumnNames="catalog_item_id"
            constraintName="fk_order_item_catalog"
            referencedTableName="catalog_item"
            referencedColumnNames="id"/>
</changeSet>
```

### Create Index

```xml
<changeSet id="006-create-index" author="developer">
    <createIndex indexName="idx_order_item_order_id"
                 tableName="order_item">
        <column name="order_id"/>
    </createIndex>
</changeSet>
```

### Drop Table

```xml
<changeSet id="007-drop-table" author="developer">
    <dropTable tableName="old_table_name"/>
</changeSet>
```

## Running Migrations

### Automatic (Default)

Migrations run automatically when the application starts:

```bash
./mvnw spring-boot:run
```

Liquibase checks for new changesets and applies them.

### Manual (Using Liquibase CLI)

If you have Liquibase installed:

```bash
liquibase --changeLogFile=src/main/resources/db/changelog/db.changelog-master.xml \
          --url=jdbc:h2:mem:ordersdb \
          --username=sa \
          --password= \
          update
```

### Check Migration Status

```bash
liquibase --changeLogFile=src/main/resources/db/changelog/db.changelog-master.xml \
          --url=jdbc:h2:mem:ordersdb \
          --username=sa \
          --password= \
          status
```

## Rollback Migrations

### Rollback Last ChangeSet

```xml
<rollback>
    <dropTable tableName="new_table"/>
</rollback>
```

### Rollback to Tag

```bash
liquibase rollback <tag_name>
```

## Best Practices

1. **One ChangeSet Per Logical Change**: Don't mix unrelated changes
2. **Idempotent Changes**: Changes should be safe to run multiple times
3. **Meaningful IDs**: Use descriptive changeSet IDs
4. **Test Locally First**: Always test migrations in development
5. **Backup Production**: Always backup before production migrations
6. **Version Control**: Keep all changelog files in Git

## Migration Naming Convention

- Format: `XXX-description.xml`
- Examples:
  - `001-create-tables.xml`
  - `002-add-indexes.xml`
  - `003-add-notes-column.xml`
  - `004-update-constraints.xml`

## Troubleshooting

### Migration Fails on Startup

1. Check Liquibase logs in application output
2. Verify changelog XML syntax
3. Ensure database connection is working
4. Check for conflicting changesets

### Changes Not Applied

1. Verify changeSet ID is unique
2. Check `DATABASECHANGELOG` table for applied changes
3. Ensure changelog is included in master file

### Rollback Issues

1. Always include `<rollback>` in changeSets
2. Test rollback in development first
3. Keep database backups

## Environment-Specific Migrations

### Development
- Migrations run automatically
- Can use `ddl-auto: update` for rapid prototyping (not recommended)

### Testing
- Migrations run before tests
- Clean database for each test run

### Production
- Migrations run on deployment
- **Always backup first**
- Consider running migrations separately before app deployment

## Database Schema Validation

Liquibase validates schema on startup:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Validates schema matches entities
```

This ensures:
- Entity annotations match database schema
- No schema drift
- Early detection of mismatches

## Migration Checklist

Before deploying a new migration:

- [ ] Migration file created with unique ID
- [ ] Included in master changelog
- [ ] Tested locally
- [ ] Tested in staging environment
- [ ] Rollback script tested
- [ ] Database backup created (production)
- [ ] Migration documented
- [ ] Team notified

## Additional Resources

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Liquibase Change Types](https://docs.liquibase.com/change-types/home.html)
- [Best Practices](https://www.liquibase.org/get-started/best-practices)

