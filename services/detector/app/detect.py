from __future__ import annotations

from dataclasses import dataclass

import cv2
from ultralytics import YOLO


@dataclass(frozen=True)
class Detection:
    label: str
    confidence: float
    bbox_x: float
    bbox_y: float
    bbox_w: float
    bbox_h: float
    frame_index: int
    ts_ms: int


class VideoDetector:
    def __init__(self, yolo_model: str) -> None:
        self._yolo = YOLO(yolo_model)
        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        self._faces = cv2.CascadeClassifier(cascade_path)

    def detect(self, path: str, sample_fps: float) -> list[Detection]:
        capture = cv2.VideoCapture(path)
        if not capture.isOpened():
            raise RuntimeError(f"unable to open video: {path}")

        native_fps = capture.get(cv2.CAP_PROP_FPS) or 25.0
        frame_interval = max(int(round(native_fps / max(sample_fps, 0.1))), 1)
        detections: list[Detection] = []
        index = 0

        try:
            while True:
                ok, frame = capture.read()
                if not ok:
                    break
                if index % frame_interval != 0:
                    index += 1
                    continue

                height, width = frame.shape[:2]
                ts_ms = int(capture.get(cv2.CAP_PROP_POS_MSEC))
                detections.extend(self._persons(frame, width, height, index, ts_ms))
                detections.extend(self._faces_in(frame, width, height, index, ts_ms))
                index += 1
        finally:
            capture.release()

        return detections

    def _persons(self, frame, width: int, height: int, index: int, ts_ms: int) -> list[Detection]:
        results = self._yolo.predict(frame, classes=[0], verbose=False)
        found: list[Detection] = []
        for result in results:
            if result.boxes is None:
                continue
            for box in result.boxes:
                xyxy = box.xyxy[0].tolist()
                conf = float(box.conf[0]) if box.conf is not None else 0.0
                found.append(_normalized("person", conf, xyxy, width, height, index, ts_ms))
        return found

    def _faces_in(self, frame, width: int, height: int, index: int, ts_ms: int) -> list[Detection]:
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = self._faces.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(32, 32))
        found: list[Detection] = []
        for x, y, w, h in faces:
            found.append(
                Detection(
                    label="face",
                    confidence=1.0,
                    bbox_x=x / width,
                    bbox_y=y / height,
                    bbox_w=w / width,
                    bbox_h=h / height,
                    frame_index=index,
                    ts_ms=ts_ms,
                )
            )
        return found


def _normalized(
    label: str,
    confidence: float,
    xyxy: list[float],
    width: int,
    height: int,
    index: int,
    ts_ms: int,
) -> Detection:
    x1, y1, x2, y2 = xyxy
    return Detection(
        label=label,
        confidence=confidence,
        bbox_x=max(x1 / width, 0.0),
        bbox_y=max(y1 / height, 0.0),
        bbox_w=max((x2 - x1) / width, 0.0),
        bbox_h=max((y2 - y1) / height, 0.0),
        frame_index=index,
        ts_ms=ts_ms,
    )
