from __future__ import annotations

import json
import logging
import tempfile
from pathlib import Path

from confluent_kafka import Consumer, KafkaException

from app.config import Settings
from app.db import Database
from app.detect import VideoDetector
from app.s3 import S3Client

log = logging.getLogger(__name__)


class VideoUploadedConsumer:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._db = Database(settings.database_url)
        self._s3 = S3Client(
            settings.aws_region,
            settings.s3_endpoint,
            settings.s3_access_key,
            settings.s3_secret_key,
        )
        self._detector = VideoDetector(settings.yolo_model)
        self._consumer = Consumer(self._kafka_config())

    def _kafka_config(self) -> dict:
        config = {
            "bootstrap.servers": self._settings.kafka_bootstrap_servers,
            "group.id": self._settings.kafka_group_id,
            "auto.offset.reset": "earliest",
            "enable.auto.commit": False,
        }
        if self._settings.kafka_security_protocol:
            config["security.protocol"] = self._settings.kafka_security_protocol
        if self._settings.kafka_sasl_mechanism:
            config["sasl.mechanism"] = self._settings.kafka_sasl_mechanism
        if self._settings.kafka_sasl_username:
            config["sasl.username"] = self._settings.kafka_sasl_username
        if self._settings.kafka_sasl_password:
            config["sasl.password"] = self._settings.kafka_sasl_password
        return config

    def run(self) -> None:
        self._consumer.subscribe([self._settings.kafka_topic])
        log.info("subscribed to %s", self._settings.kafka_topic)
        try:
            while True:
                message = self._consumer.poll(1.0)
                if message is None:
                    continue
                if message.error():
                    raise KafkaException(message.error())
                self._handle(message.value())
                self._consumer.commit(message=message, asynchronous=False)
        finally:
            self._consumer.close()

    def _handle(self, raw: bytes | None) -> None:
        if not raw:
            return
        event = json.loads(raw.decode("utf-8"))
        video_id = event["videoId"]
        bucket = event["bucket"]
        key = event["key"]
        log.info("processing video %s s3://%s/%s", video_id, bucket, key)

        if not self._db.video_exists(video_id):
            log.warning("video %s not found, skipping", video_id)
            return

        self._db.mark_processing(video_id)
        suffix = Path(key).suffix or ".mp4"
        try:
            with tempfile.NamedTemporaryFile(suffix=suffix, delete=True) as tmp:
                self._s3.download(bucket, key, Path(tmp.name))
                detections = self._detector.detect(tmp.name, self._settings.sample_fps)
                self._db.insert_detections(video_id, detections)
                self._db.append_custody(video_id, "DETECTED")
            self._db.mark_completed(video_id)
            log.info("completed video %s detections=%s", video_id, len(detections))
        except Exception:
            log.exception("failed video %s", video_id)
            self._db.mark_failed(video_id, "detection failed")
