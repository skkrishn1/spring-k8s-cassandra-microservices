---
name: cassandra-schema-design
description: '**CHECKLIST SKILL** — Design and validate Cassandra table schemas, keyspaces, partitioning strategies, and CQL queries. USE FOR: schema design reviews, performance validation, consistency checks. DO NOT USE FOR: data export/import or replication cluster management.'
---

# Cassandra Schema Design

## Overview
This skill provides guidance for designing, validating, and reviewing Cassandra schemas for optimal performance, consistency, and scalability. Focus is on partition/clustering key design, denormalization, and query patterns.

## Design Checklist

### Keyspace Configuration
- [ ] **Replication Factor**: Match number of nodes (RF=1 local, RF=3 production)
- [ ] **Class**: `SimpleStrategy` for single datacenter, `NetworkTopologyStrategy` for multi-DC
- [ ] **Consistency**: Appropriate read/write consistency levels (LOCAL_ONE vs QUORUM)
- [ ] **Durability**: Verify commitlog and hints are configured

### Partition Key Design
- [ ] **Cardinality**: High cardinality (many unique values) – avoid hot partitions
- [ ] **Even Distribution**: Data distributed evenly across nodes
- [ ] **Access Pattern**: Partition key matches primary query pattern
- [ ] **Immutability**: Partition key values don't change (or rare updates)

### Clustering Key Design
- [ ] **Sort Order**: Clustering keys sort data within partition (ascending/descending)
- [ ] **Range Queries**: Support efficient range queries (`>`, `<`, `BETWEEN`)
- [ ] **Composite Keys**: Multiple clustering columns for multi-level sort
- [ ] **Ordering**: Use ascending for time-series, descending for recent-first

### Table Structure
- [ ] **Denormalization**: Data is denormalized for query efficiency (joins are expensive)
- [ ] **Redundancy**: Acceptable redundancy for reads (Cassandra optimizes for reads)
- [ ] **Static Columns**: Use for non-clustering, non-changing data to reduce storage
- [ ] **Collections**: Use carefully (sets, lists, maps); large collections impact performance

### Query Patterns
- [ ] **Primary Queries**: Queries match partition key + optional clustering keys
- [ ] **Secondary Indexes**: Only on low-cardinality columns if used
- [ ] **Materialized Views**: For alternative query patterns (materialized views or denormalized tables)
- [ ] **No Full Table Scans**: Queries always include partition key

### Performance Optimization
- [ ] **TTL**: Use for temporary data (audit logs, sessions)
- [ ] **Compaction**: Set appropriate compaction strategy (SizeTieredCompactionStrategy, LeveledCompactionStrategy)
- [ ] **Bloom Filters**: Enabled for partition lookups (default)
- [ ] **Read/Write Latency**: Understand trade-offs between consistency and speed

### Consistency & Reliability
- [ ] **Consistency Levels**: Production uses at least LOCAL_QUORUM for critical data
- [ ] **Batch Operations**: Use carefully; avoid large batches
- [ ] **Lightweight Transactions**: Use for distributed transactions (performance trade-off)
- [ ] **Timeouts**: Appropriate timeout settings in driver config

## Schema Design Examples

### Example 1: Products Table

**Use Case**: Find products by name, then by name + id

```sql
CREATE TABLE betterbotz.products (
    name TEXT,
    id UUID,
    description TEXT,
    price DECIMAL,
    last_updated TIMESTAMP,
    PRIMARY KEY (name, id)
);
```

**Analysis**:
- Partition Key (name): Prevents hot partition if product names distributed evenly
- Clustering Key (id): Allows range queries within product name
- Query: `SELECT * FROM products WHERE name = 'iPhone'` (efficient)
- Query: `SELECT * FROM products WHERE name = 'iPhone' AND id = '123e...'` (very efficient)

### Example 2: Orders Table

**Use Case**: Find orders by orderId, then by orderId + productId

```sql
CREATE TABLE betterbotz.orders (
    order_id UUID,
    product_id UUID,
    product_name TEXT,
    product_price DECIMAL,
    product_quantity INT,
    added_to_order_timestamp TIMESTAMP,
    PRIMARY KEY (order_id, product_id)
);
```

**Analysis**:
- Partition Key (order_id): Groups products for a single order
- Clustering Key (product_id): Multiple products per order, ordered by productId
- Query: `SELECT * FROM orders WHERE order_id = '456e...'` (gets all products for order)
- Query: `SELECT product_name, product_price FROM orders WHERE order_id = '456e...'` (projection)

### Example 3: Time-Series Events

**Use Case**: Find recent events for a user

```sql
CREATE TABLE events (
    user_id UUID,
    event_time TIMESTAMP,
    event_type TEXT,
    event_data TEXT,
    PRIMARY KEY (user_id, event_time)
) WITH CLUSTERING ORDER BY (event_time DESC);
```

**Analysis**:
- Partition Key (user_id): Events grouped by user
- Clustering Key (event_time DESC): Recent events first within partition
- Query: `SELECT * FROM events WHERE user_id = '789e...' LIMIT 10` (gets 10 most recent)

## Validation Checklist

- [ ] **No Full Table Scans**: All queries include partition key
- [ ] **Partition Size**: Each partition <100MB (rule of thumb)
- [ ] **Write Throughput**: Partition key distributes writes evenly
- [ ] **Read Efficiency**: Queries avoid secondary indexes where possible
- [ ] **Schema Alignment**: Schema matches accessed query patterns
- [ ] **Consistency**: Read/write consistency levels appropriate for use case
- [ ] **Replication**: RF and consistency levels provide desired fault tolerance

## CQL Best Practices

```cql
-- Good: Uses partition key
SELECT * FROM products WHERE name = 'iPhone';

-- Good: Partition + clustering key
SELECT * FROM products WHERE name = 'iPhone' AND id = ?;

-- BAD: No partition key (full table scan)
SELECT * FROM products WHERE price > 50;

-- Good: With secondary index on price (use sparingly)
CREATE INDEX price_idx ON products(price);
SELECT * FROM products WHERE price > 50;

-- Good: Use LIMIT to prevent large result sets
SELECT * FROM orders WHERE order_id = ? LIMIT 100;

-- BAD: Unbounded query without LIMIT
SELECT * FROM orders;

-- Good: Prepare statements for performance
PREPARE SELECT * FROM products WHERE name = ? AND id = ?;
```

## Related Skills
- Use `spring-boot-unit-tests` to test schema access patterns
- Use `kubernetes-deployment` for verifying schema in deployed clusters
- Consult CLAUDE.md for project-specific Cassandra configuration