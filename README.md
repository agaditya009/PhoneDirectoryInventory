# Phone Inventory Service

**Java 11**, **Spring Boot 2.7.18**, **MySQL**, **Elasticsearch**, **JdbcTemplate**.

## Run
The service requires MySQL and Elasticsearch. Start both services and then run the application.

```bash
docker-compose up -d mysql elasticsearch
```

Or run the services manually:

```bash
docker run --name phones-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=phones \
  -e MYSQL_USER=app \
  -e MYSQL_PASSWORD=app \
  -p 3306:3306 -d mysql:8

docker run --name phones-es \
  -e discovery.type=single-node \
  -p 9200:9200 -d docker.elastic.co/elasticsearch/elasticsearch:7.17.0
```

Run the service:
```bash
mvn spring-boot:run
```

### Elasticsearch Mapping Update

If upgrading from an earlier version, drop and recreate the `phones` index or reindex existing documents so the `reservedUntil` field is stored as an `epoch_millis` date.

## Endpoints
- `POST /api/phones/upload` — multipart `file` (CSV with header: `number,countryCode,areaCode`)
 - `GET /api/phones?countryCode=&areaCode=&contains=&status=&page=&size=`
- `POST /api/numbers/{id}/reserve?userId=U123&minutes=15`
- `POST /api/numbers/{id}/allocate?userId=U123`
- `POST /api/numbers/{id}/activate?userId=U123`
- `POST /api/numbers/{id}/deactivate?userId=U123`

