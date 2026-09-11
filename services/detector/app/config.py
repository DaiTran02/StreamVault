import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    kafka_bootstrap_servers: str
    kafka_topic: str
    kafka_group_id: str
    kafka_security_protocol: str
    kafka_sasl_mechanism: str
    kafka_sasl_username: str
    kafka_sasl_password: str
    database_url: str
    aws_region: str
    s3_endpoint: str
    s3_access_key: str
    s3_secret_key: str
    sample_fps: float
    yolo_model: str

    @staticmethod
    def from_env() -> "Settings":
        return Settings(
            kafka_bootstrap_servers=os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
            kafka_topic=os.environ.get("KAFKA_TOPIC_VIDEO_UPLOADED", "video.uploaded"),
            kafka_group_id=os.environ.get("KAFKA_GROUP_ID", "streamvault-detector"),
            kafka_security_protocol=os.environ.get("KAFKA_SECURITY_PROTOCOL", ""),
            kafka_sasl_mechanism=os.environ.get("KAFKA_SASL_MECHANISM", ""),
            kafka_sasl_username=os.environ.get("KAFKA_SASL_USERNAME", ""),
            kafka_sasl_password=os.environ.get("KAFKA_SASL_PASSWORD", ""),
            database_url=os.environ.get(
                "DATABASE_URL",
                "postgresql://streamvault:streamvault@localhost:5432/streamvault",
            ),
            aws_region=os.environ.get("AWS_REGION", "us-east-1"),
            s3_endpoint=os.environ.get("S3_ENDPOINT", ""),
            s3_access_key=os.environ.get("S3_ACCESS_KEY", ""),
            s3_secret_key=os.environ.get("S3_SECRET_KEY", ""),
            sample_fps=float(os.environ.get("DETECTOR_SAMPLE_FPS", "1")),
            yolo_model=os.environ.get("DETECTOR_YOLO_MODEL", "yolov8n.pt"),
        )
