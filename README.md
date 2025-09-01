# Phone Inventory Service — Spring JDBC (no Hibernate)

**Java 11**, **Spring Boot 2.7.18**, **H2 (default) / MySQL**, **JdbcTemplate**, **Elasticsearch**.

## Elasticsearch

An Elasticsearch node must be available before starting the service. It defaults to
`http://localhost:9200`; to use a different instance, configure `spring.elasticsearch.uris`
in your environment or `application.properties`.

The application keeps the `telephone_numbers` index in sync with database changes. State
transitions and batch uploads automatically update Elasticsearch.

## Run
Start the Elasticsearch container (required). The service defaults to an in-memory H2 database. If you prefer a persistent MySQL database, start MySQL as well and run the app with the `mysql` profile.
```bash
# Elasticsearch only
docker-compose up -d elasticsearch

# Optional: add MySQL
docker-compose up -d mysql
```
Or run them manually:
```bash
# MySQL
docker run --name phones-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=phones \
  -e MYSQL_USER=app \
  -e MYSQL_PASSWORD=app \
  -p 3306:3306 -d mysql:8

# Elasticsearch
docker run --name phones-es \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  -e ES_JAVA_OPTS="-Xms512m -Xmx512m" \
  -p 9200:9200 -d docker.elastic.co/elasticsearch/elasticsearch:8.11.1
```

Run the service:
```bash
# H2 (default)
mvn spring-boot:run

# MySQL
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

## Endpoints
- `POST /api/phones/upload` — multipart `file` (CSV with header: `number,countryCode,areaCode`)
 - `GET /api/phones?countryCode=&areaCode=&contains=&status=&page=&size=`
 - `GET /api/phones/elastic?countryCode=&areaCode=&contains=&status=` — optional search via Elasticsearch (database search above remains unchanged)
- `POST /api/numbers/{id}/reserve?userId=U123&minutes=15`
- `POST /api/numbers/{id}/allocate?userId=U123`
- `POST /api/numbers/{id}/activate?userId=U123`
- `POST /api/numbers/{id}/deactivate?userId=U123`

## Batch Import
- CSV numbers are normalized to digits-only `numberDigits` for consistent lookup
- Uses vendor-specific upsert (`MERGE` for H2, `INSERT ... ON DUPLICATE KEY UPDATE` for MySQL) with conditional assignments to skip unchanged rows
- Step chunk size is configurable via `batch.chunk.size` (default `1000`)

## Design (Best Practices, No Hibernate)
- **Schema-first**: `schema.sql` defines DDL; `data.sql` seeds demo data.
- **DAO layer**: `JdbcTemplate` with `RowMapper` (no ORM).
- **Optimistic locking**: `version` column in `UPDATE ... WHERE version=?`.
- **Idempotent load**: unique constraint on `number`; duplicate inserts ignored.
- **Audit trail**: each state transition recorded.
- **Pagination**: `LIMIT/OFFSET` with count.

