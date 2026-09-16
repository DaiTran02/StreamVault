# StreamVault

Register and log in through auth-service to get an OAuth2 access token. Upload a video with that token; ingestion stores the object in S3, binds it to the user, starts a SHA-256 chain of custody, publishes a Kafka event, then the detector records people and faces and extends the chain.

```
Client → Auth (OAuth2 JWT) → Ingestion (Spring WebFlux) → S3 + Postgres + Kafka
                                                      → Detector (Python) → detections + custody in Postgres
```

## AWS resources

Create these before running the services:

- **S3 bucket** for uploaded objects
- **RDS PostgreSQL** (database name e.g. `streamvault`)
- **Kafka** (Amazon MSK or any Kafka cluster) with topic `video.uploaded` created in advance if auto-create is disabled

IAM for the ingestion and detector runtimes:

- `s3:PutObject` (ingestion) and `s3:GetObject` (detector) on the bucket
- Network access to RDS and Kafka brokers
- Kafka produce (ingestion) and consume (detector) on `video.uploaded`

Credentials use the default AWS SDK chain (`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`, `~/.aws/credentials`, or an instance/task role).

## Environment

Shared:

| Variable | Purpose |
| --- | --- |
| `AWS_REGION` | AWS region (default `ap-southeast-1`) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap list |
| `KAFKA_TOPIC_VIDEO_UPLOADED` | Topic name (default `video.uploaded`) |
| `KAFKA_SECURITY_PROTOCOL` | Optional (`SASL_SSL`, `SSL`, …) |
| `KAFKA_SASL_MECHANISM` | Optional (`SCRAM-SHA-512`, `PLAIN`, …) |

Auth-service (Java, port 9002):

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC URL (same Postgres as ingestion) |
| `OAUTH2_ISSUER` | JWT issuer (default `http://localhost:9002`) |
| `OAUTH2_CLIENT_ID` / `OAUTH2_CLIENT_SECRET` | Confidential OAuth2 client |

Ingestion (Java):

| Variable | Purpose |
| --- | --- |
| `S3_BUCKET` | Destination bucket |
| `SPRING_R2DBC_URL` | e.g. `r2dbc:postgresql://host:5432/streamvault` |
| `SPRING_FLYWAY_URL` | e.g. `jdbc:postgresql://host:5432/streamvault` |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `KAFKA_SASL_JAAS_CONFIG` | Optional JAAS for the Java producer |
| `INGESTION_MAX_FILE_SIZE_BYTES` | Max upload size (default 5 GiB) |
| `OAUTH2_JWK_SET_URI` | Auth-service JWKS URL (default `http://localhost:9002/oauth2/jwks`) |

Detector (Python):

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | e.g. `postgresql://user:pass@host:5432/streamvault` |
| `KAFKA_GROUP_ID` | Consumer group (default `streamvault-detector`) |
| `KAFKA_SASL_USERNAME` / `KAFKA_SASL_PASSWORD` | Optional SASL |
| `DETECTOR_SAMPLE_FPS` | Frames sampled per second (default `1`) |
| `DETECTOR_YOLO_MODEL` | Ultralytics weights (default `yolov8n.pt`) |

Flyway on auth-service creates `users` and `refresh_tokens`. Ingestion Flyway creates `videos`, `detections`, and `custody_events`, and links videos to users.

Each video has `user_id` (owner) and `content_sha256` (SHA-256 of the file bytes). Detections belong to a video, so they inherit that user relationship. Every ingest, detection, and authenticated read appends a custody event whose `chain_hash` is:

`SHA-256(previous_chain_hash | videoId | userId | action | content_sha256 | epochMillis)`

Reads fail with 409 if the chain does not recompute.

## Local infrastructure

Kafka and MinIO (S3-compatible). Use your own local Postgres (`localhost:5432`).

```bash
docker compose up -d
```

| Service | Host port | Credentials |
| --- | --- | --- |
| Kafka | `localhost:9092` | none (plaintext), topic `video.uploaded` |
| MinIO S3 | `localhost:9000` | `streamvault` / `streamvault`, bucket `streamvault-videos` |
| MinIO console | http://localhost:9001 | same as MinIO |

Stop with `docker compose down`. Add `-v` to also delete data volumes.

## Run auth-service

Java 21 JDK (not a JRE) and Maven. Auth listens on port `9002` and issues JWTs signed with an in-memory RSA key (tokens are invalid after restart).

If `mvn` reports `release version 21 not supported`, it is running on a JRE (Ubuntu often defaults to a newer JRE). Point it at JDK 21:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

```bash
cd services/auth-service
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./mvnw spring-boot:run
```

```bash
curl -sS -X POST http://localhost:9002/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}'

# later
curl -sS -X POST http://localhost:9002/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}'
```

`/api/v1` responses use a shared envelope: `{ "status": 200, "service": "auth-service", "message": "success", "data": { ... } }`. Tokens are in `data` (`access_token`, `token_type`, `expires_in`, `refresh_token`, `scope`). JWKS is at `http://localhost:9002/oauth2/jwks`. Authorization-code and client-credentials grants stay on the standard `/oauth2/token` endpoint (`client_id` `streamvault-client`, secret `streamvault-secret`) and are not wrapped.

## Run ingestion

Java 21 and Maven wrapper. For local, put values in YAML instead of exporting env vars:

```bash
cd services/ingestion
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./mvnw spring-boot:run
```

`spring-boot:run` activates the `local` profile. `application-local.yml` is gitignored. In an IDE, set Active profiles to `local`.

Production/staging still use the env vars in the table above (or set `SPRING_PROFILES_ACTIVE` so `local` is not applied).

Upload and poll (replace `$TOKEN` with `access_token` from login):

```bash
curl -sS -X POST http://localhost:8080/api/v1/videos \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@sample.mp4;type=video/mp4"

# envelope data includes id, userId, contentSha256
curl -sS -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/videos/{id}
curl -sS -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/videos/{id}/detections
curl -sS -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/videos/{id}/custody
```

Allowed types: `video/mp4`, `video/quicktime`, `video/webm`.

## Run detector

Python 3.11+ recommended. The first run downloads `yolov8n.pt`.

```bash
cd services/detector
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export DATABASE_URL=postgresql://streamvault:streamvault@localhost:5432/streamvault
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export S3_ENDPOINT=http://localhost:9000
export S3_ACCESS_KEY=streamvault
export S3_SECRET_KEY=streamvault
export AWS_REGION=us-east-1
python -m app
```

Against AWS, point `DATABASE_URL`, `KAFKA_BOOTSTRAP_SERVERS`, and omit `S3_ENDPOINT` so the worker uses real S3.

The worker consumes `video.uploaded`, sets status `PROCESSING`, samples frames (~1 fps), runs YOLOv8n for `person` and OpenCV Haar for `face`, writes `detections`, then sets `COMPLETED` or `FAILED`.

## Status values

`STORED` → `PROCESSING` → `COMPLETED` (or `FAILED`).
