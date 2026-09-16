from __future__ import annotations

import hashlib
import time
import uuid
from typing import Iterable

import psycopg
from psycopg.rows import dict_row

from app.detect import Detection


class Database:
    def __init__(self, url: str) -> None:
        self._url = url

    def mark_processing(self, video_id: str) -> None:
        with psycopg.connect(self._url) as conn:
            conn.execute(
                """
                UPDATE videos
                SET status = 'PROCESSING', error_message = NULL, updated_at = NOW()
                WHERE id = %s
                """,
                (video_id,),
            )
            conn.commit()

    def mark_completed(self, video_id: str) -> None:
        with psycopg.connect(self._url) as conn:
            conn.execute(
                """
                UPDATE videos
                SET status = 'COMPLETED', error_message = NULL, updated_at = NOW()
                WHERE id = %s
                """,
                (video_id,),
            )
            conn.commit()

    def mark_failed(self, video_id: str, message: str) -> None:
        with psycopg.connect(self._url) as conn:
            conn.execute(
                """
                UPDATE videos
                SET status = 'FAILED', error_message = %s, updated_at = NOW()
                WHERE id = %s
                """,
                (message[:2000], video_id),
            )
            conn.commit()

    def insert_detections(self, video_id: str, detections: Iterable[Detection]) -> None:
        rows = [
            (
                str(uuid.uuid4()),
                video_id,
                item.label,
                item.confidence,
                item.bbox_x,
                item.bbox_y,
                item.bbox_w,
                item.bbox_h,
                item.frame_index,
                item.ts_ms,
            )
            for item in detections
        ]
        if not rows:
            return
        with psycopg.connect(self._url) as conn:
            conn.executemany(
                """
                INSERT INTO detections (
                    id, video_id, label, confidence,
                    bbox_x, bbox_y, bbox_w, bbox_h,
                    frame_index, ts_ms
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                rows,
            )
            conn.commit()

    def video_exists(self, video_id: str) -> bool:
        with psycopg.connect(self._url, row_factory=dict_row) as conn:
            row = conn.execute("SELECT 1 FROM videos WHERE id = %s", (video_id,)).fetchone()
            return row is not None

    def append_custody(self, video_id: str, action: str) -> None:
        with psycopg.connect(self._url, row_factory=dict_row) as conn:
            video = conn.execute(
                "SELECT user_id, content_sha256 FROM videos WHERE id = %s",
                (video_id,),
            ).fetchone()
            if video is None or not video["content_sha256"]:
                return
            previous = conn.execute(
                """
                SELECT chain_hash
                FROM custody_events
                WHERE video_id = %s
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """,
                (video_id,),
            ).fetchone()
            previous_hash = previous["chain_hash"] if previous else ("0" * 64)
            user_id = video["user_id"]
            content_sha256 = video["content_sha256"]
            millis = time.time_ns() // 1_000_000
            user = "" if user_id is None else str(user_id)
            canonical = f"{previous_hash}|{video_id}|{user}|{action}|{content_sha256}|{millis}"
            chain_hash = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
            conn.execute(
                """
                INSERT INTO custody_events (
                    id, video_id, user_id, action, content_sha256,
                    previous_chain_hash, chain_hash, created_at
                ) VALUES (
                    %s, %s, %s, %s, %s, %s, %s,
                    TIMESTAMPTZ 'epoch' + (%s) * INTERVAL '1 millisecond'
                )
                """,
                (
                    str(uuid.uuid4()),
                    video_id,
                    str(user_id) if user_id is not None else None,
                    action,
                    content_sha256,
                    previous_hash,
                    chain_hash,
                    millis,
                ),
            )
            conn.commit()
