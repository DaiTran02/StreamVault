from pathlib import Path

import boto3
from botocore.config import Config


class S3Client:
    def __init__(
            self,
            region: str,
            endpoint: str = "",
            access_key: str = "",
            secret_key: str = "",
    ) -> None:
        kwargs: dict = {"region_name": region}
        if endpoint:
            kwargs["endpoint_url"] = endpoint
            kwargs["config"] = Config(s3={"addressing_style": "path"})
        if access_key and secret_key:
            kwargs["aws_access_key_id"] = access_key
            kwargs["aws_secret_access_key"] = secret_key
        self._client = boto3.client("s3", **kwargs)

    def download(self, bucket: str, key: str, dest: Path) -> None:
        dest.parent.mkdir(parents=True, exist_ok=True)
        self._client.download_file(bucket, key, str(dest))
