# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A multi-module Maven project demonstrating three Spring microservices communicating through a Spring Cloud Gateway, all persisting data to Apache Cassandra. Designed to run on Kubernetes (minikube) or locally via Docker Compose.

**Modules:**
- `microservice-spring-boot` — Products service (port 8083) using the Cassandra Java Driver directly
- `microservice-spring-data` — Orders service (port 8081) using Spring Data Cassandra
- `gateway-service` — Spring Cloud Gateway (port 8080) routing to both services

**Key versions:** Java 11, Spring Boot 2.3.0, Spring Cloud 2.2.3, Cassandra Driver 4.8.0

## Build Commands

```bash
# Build all modules from the root
mvn package

# Build a single module
cd microservice-spring-boot && mvn package
cd microservice-spring-data && mvn package
cd gateway-service && mvn package

# Skip tests (there are none, but speeds up the build)
mvn package -DskipTests

# Build Docker images via the Maven dockerfile plugin (alternative to direct docker build)
mvn package dockerfile:build -DskipTests
```

There are no unit tests in this repository — `src/test/` directories are empty.

## Local Development with Docker Compose

Start a 3-node Cassandra cluster (v3.11.6) locally. Node2 starts after 60s, node3 after 120s. Data is persisted to `~/docker-volumes/cassandra3-dc1-nodeN/`.

```bash
docker-compose up -d
```

Services connect on `localhost:9042` (default profile) or `host.docker.internal:9042` (docker profile). Keyspace: `betterbotz`, local DC: `dc1`. Default Cassandra credentials: `cassandra` / `cassandra`.

Run the Spring Boot and Spring Data services locally (after Cassandra is up):

```bash
# Products service
cd microservice-spring-boot && mvn spring-boot:run

# Orders service (separate terminal)
cd microservice-spring-data && mvn spring-boot:run
```

## Kubernetes Deployment (Minikube)

**Important:** Before applying manifests, update the image name in each `deploy/*/spring-*-deployment.yml` to use your Docker Hub username.

```bash
# Start minikube with RBAC
minikube start --driver=docker --extra-config=apiserver.authorization-mode=RBAC,Node
eval `minikube docker-env`

# Build Docker images against minikube's Docker daemon
docker build -t <user>/spring-boot-service:1.0.0-SNAPSHOT ./microservice-spring-boot
docker build -t <user>/spring-data-service:1.0.0-SNAPSHOT ./microservice-spring-data
docker build -t <user>/gateway-service:1.0.0-SNAPSHOT ./gateway-service

# Create namespaces
kubectl create ns cass-operator
kubectl create ns spring-boot-service
kubectl create ns spring-data-service
kubectl create ns gateway-service

# Create DB secrets in each service namespace
kubectl -n spring-boot-service create secret generic db-secret \
  --from-literal=username=$DB_USER --from-literal=password=$DB_PASSWORD
kubectl -n spring-data-service create secret generic db-secret \
  --from-literal=username=$DB_USER --from-literal=password=$DB_PASSWORD

# Deploy all services
kubectl -n spring-boot-service apply -f deploy/spring-boot
kubectl -n spring-data-service apply -f deploy/spring-data
kubectl -n gateway-service apply -f deploy/gateway

# Access the gateway locally
GATEWAY_POD=$(kubectl -n gateway-service get pods | tail -n 1 | cut -f 1 -d ' ')
kubectl -n gateway-service port-forward $GATEWAY_POD 8080:8080

# Optionally expose individual services for direct testing
BOOT_SERVICE_POD=$(kubectl -n spring-boot-service get pods | tail -n 1 | cut -f 1 -d ' ')
kubectl -n spring-boot-service port-forward $BOOT_SERVICE_POD 8083:8083

DATA_SERVICE_POD=$(kubectl -n spring-data-service get pods | tail -n 1 | cut -f 1 -d ' ')
kubectl -n spring-data-service port-forward $DATA_SERVICE_POD 8081:8081
```

Image pull policy is `Never` — images must be built against minikube's Docker daemon.

## Architecture

```
Client → Gateway (8080)
           ├── /api/products/** → Spring Boot Service (8083) → Cassandra
           └── /api/orders/**   → Spring Data Service (8081) → Cassandra
```

**Gateway** reads route config from a Kubernetes ConfigMap (`deploy/gateway/gateway-configmap.yml`). Routes use full K8s DNS: `http://spring-boot-service.spring-boot-service.svc.cluster.local:80`.

**Spring Boot Service** (Products) uses `CqlSession` from the Cassandra Java Driver directly. Configuration is in `SpringBootCassandraConfiguration`, which reads contact points from `application.yml` (or K8s ConfigMap). The `products` table schema (partition key: `name`, clustering key: `id`) is created programmatically by `ProductDao` on startup, not from a CQL file. All CQL statements are prepared at startup in `ProductDao`'s constructor. Entity: `Product` with fields: id (UUID), name, description, price, lastUpdated.

**Spring Data Service** (Orders) extends `AbstractCassandraConfiguration`. `OrderRepository` extends `CassandraRepository`, and Spring Data REST auto-generates CRUD endpoints under `/api`. `Order` has a composite primary key (`OrderPrimaryKey`: orderId + productId). Schema is loaded from `orders-schema.cql` at startup; `schema-action: create-if-not-exists` applies. Keyspace is also created programmatically in `getKeyspaceCreations()`. Additional endpoints beyond Spring Data REST are in `OrderController`.

## API Endpoints

### Products Service (port 8083)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/products/search/{name}` | Find products by name |
| GET | `/api/products/search/{name}/{id}` | Find product by name and UUID |
| POST | `/api/products/add` | Create new product |
| DELETE | `/api/products/delete/{name}` | Delete products by name |
| DELETE | `/api/products/delete/{name}/{id}` | Delete specific product |
| GET | `/swagger-ui.html` | Swagger UI (Springfox 2.9.2) |

### Orders Service (port 8081)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/orders/add` | Create new order |
| DELETE | `/api/orders/delete/order` | Delete entire order |
| DELETE | `/api/orders/delete/product-from-order` | Delete product from order |
| GET | `/api/orders/search/order-by-id` | Find orders by orderId |
| GET | `/api/orders/search/order-by-product-id` | Find by orderId + productId |
| GET | `/api/orders/search/name-and-price-only` | Projection: product name and price |
| GET | `/v3/api-docs` | OpenAPI spec (SpringDoc 1.3.9) |

Management/actuator runs on separate ports: `8084` (products), `8082` (orders), `8085` (gateway). Kubernetes liveness/readiness probes hit `/actuator/health` on these management ports with a 120s initial delay.

### Sample curl commands

```bash
# Add products
curl -X POST -H "Content-Type: application/json" \
  -d '{"name":"mobile","id":"123e4567-e89b-12d3-a456-556642440000","description":"iPhone","price":"500.00"}' \
  http://localhost:8083/api/products/add

# Search products
curl http://localhost:8083/api/products/search/mobile
curl http://localhost:8083/api/products/search/mobile/123e4567-e89b-12d3-a456-556642440000

# Add orders
curl -H "Content-Type: application/json" \
  -d '{"key":{"orderId":"123e4567-e89b-12d3-a456-556642440000","productId":"123e4567-e89b-12d3-a456-556642440000"},"productName":"iPhone","productPrice":"500.00","productQuantity":1,"addedToOrderTimestamp":"2020-04-12T11:21:59.001+0000"}' \
  http://localhost:8081/api/orders/add

# Search orders
curl "http://localhost:8081/api/orders/search/order-by-id?orderId=123e4567-e89b-12d3-a456-556642440000"
curl "http://localhost:8081/api/orders/search/name-and-price-only?orderId=123e4567-e89b-12d3-a456-556642440000"
```

## Spring Profiles

Each service has three profiles:
- `default` — connects to `localhost:9042`
- `docker` — connects to `host.docker.internal:9042`
- `kubernetes` — reads connection info from a K8s ConfigMap via Spring Cloud Kubernetes

The active profile is set via `SPRING_PROFILES_ACTIVE` env var in K8s deployments.

## Cassandra / Astra Dual Support

Both services check `astra.secure-connect-bundle` at startup. When set to anything other than `none`, the Astra code path is taken (skips contact-point config and keyspace creation). For K8s + Astra:

```bash
# Create the secure connect bundle secret (in addition to db-secret)
kubectl -n spring-boot-service create secret generic astracreds \
  --from-file=secure-connect-bundle=$SECURE_CONNECT_BUNDLE_PATH
kubectl -n spring-data-service create secret generic astracreds \
  --from-file=secure-connect-bundle=$SECURE_CONNECT_BUNDLE_PATH
```

Then update each service's ConfigMap to set `astra.secure-connect-bundle: /app/astra/creds` and uncomment the `astravol` volume/volumeMount sections in the deployment YAMLs.

## Key Kubernetes Files

| Path | Purpose |
|------|---------|
| `deploy/cassandra-4.0.0-1node.yml` | Single-node Cassandra via Cassandra Operator |
| `deploy/storage-class.yml` | StorageClass for Cassandra PVCs |
| `deploy/ingress.yml` | Optional ingress for the gateway |
| `deploy/*/spring-*-configmap.yml` | App config (contact points, keyspace) |
| `deploy/*/*-rbac.yml` | RBAC for Spring Cloud Kubernetes config access |
