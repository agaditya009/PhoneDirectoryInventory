# Phone Inventory Service — Spring JDBC (no Hibernate)

**Java 11**, **Spring Boot 2.7.18**, **H2 in-memory**, **JdbcTemplate**, **Elasticsearch**.

## Elasticsearch

An Elasticsearch node must be available before starting the service. It defaults to
`http://localhost:9200`; to use a different instance, configure `spring.elasticsearch.uris`
in your environment or `application.properties`.

The application keeps the `telephone_numbers` index in sync with database changes. State
transitions and batch uploads automatically update Elasticsearch.

## Run
```bash
mvn spring-boot:run
```
H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:phones`)

## Endpoints
- `POST /api/numbers/upload` — multipart `file` (CSV with header: `number,countryCode,areaCode,prefix`)
- `GET /api/numbers/search?countryCode=&areaCode=&prefix=&contains=&status=&page=&size=`
- `GET /api/phones/elastic?countryCode=&areaCode=&prefix=&contains=&status=` — optional search via Elasticsearch (database search above remains unchanged)
- `POST /api/numbers/{id}/reserve?userId=U123&minutes=15`
- `POST /api/numbers/{id}/allocate?userId=U123`
- `POST /api/numbers/{id}/activate?userId=U123`
- `POST /api/numbers/{id}/deactivate?userId=U123`

## Design (Best Practices, No Hibernate)
- **Schema-first**: `schema.sql` defines DDL; `data.sql` seeds demo data.
- **DAO layer**: `JdbcTemplate` with `RowMapper` (no ORM).
- **Optimistic locking**: `version` column in `UPDATE ... WHERE version=?`.
- **Idempotent load**: unique constraint on `number`; duplicate inserts ignored.
- **Audit trail**: each state transition recorded.
- **Pagination**: `LIMIT/OFFSET` with count.

