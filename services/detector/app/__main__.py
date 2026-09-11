import logging

from app.config import Settings
from app.consumer import VideoUploadedConsumer


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    consumer = VideoUploadedConsumer(Settings.from_env())
    consumer.run()


if __name__ == "__main__":
    main()
