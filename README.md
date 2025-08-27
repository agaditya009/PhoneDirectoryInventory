# Phone Inventory Service — Spring JDBC (no Hibernate)

**Java 11**, **Spring Boot 2.7.18**, **Elasticsearch-backed search**, **H2 in-memory**, **JdbcTemplate**.

## Run
```bash
mvn spring-boot:run
```
H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:phones`)

## Endpoints
- `POST /api/numbers/upload` — multipart `file` (CSV with header: `number,countryCode,areaCode,prefix`)
- `GET /api/numbers/search?countryCode=&areaCode=&prefix=&contains=&status=&page=&size=` — powered by Elasticsearch
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

