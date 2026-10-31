#!/usr/bin/env python3
"""Validate a Docker --env-file without sourcing it or disclosing values."""
import re
import stat
import sys
from pathlib import Path


def validate(path):
    path = Path(path)
    if stat.S_IMODE(path.stat().st_mode) & 0o077:
        raise ValueError("Environment file must not be accessible by group or others (use chmod 600)")
    values = {}
    for line in path.read_text().splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        key, sep, value = line.partition("=")
        if not sep or not re.fullmatch(r"[A-Z][A-Z0-9_]*", key) or key in values:
            raise ValueError("Environment file contains an invalid or duplicate key")
        if value.startswith(("'", '"')) or "${" in value:
            raise ValueError(f"{key}: use literal Docker env-file syntax, without quotes or interpolation")
        values[key] = value
    for key in ("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASS", "JWT_SECRET", "REDIS_HOST",
                "REDIS_PORT", "RESUME_STORAGE_S3_BUCKET", "RESUME_STORAGE_S3_REGION"):
        if not values.get(key, "").strip():
            raise ValueError(f"Required configuration is missing: {key}")
    for key in ("DB_HOST", "REDIS_HOST"):
        if values[key].lower() in ("localhost", "127.0.0.1", "::1"):
            raise ValueError(f"{key} must be reachable from the container network, not container loopback")
    for key in ("DB_PORT", "REDIS_PORT"):
        if not values[key].isdigit() or not 1 <= int(values[key]) <= 65535:
            raise ValueError(f"Invalid port configuration: {key}")
    if not re.fullmatch(r"[A-Za-z0-9_]+", values["DB_NAME"]):
        raise ValueError("Invalid DB_NAME")
    if values["DB_USER"].lower() == "root" or values["DB_PASS"] == "root":
        raise ValueError("A dedicated database account with a non-default password is required")
    if len(values["JWT_SECRET"]) < 64:
        raise ValueError("JWT_SECRET must contain at least 64 characters")
    expected = {"PORT": "8080", "SPRING_PROFILES_ACTIVE": "prod", "RATE_LIMIT_BACKEND": "redis",
                "RESUME_STORAGE_S3_ENABLED": "true"}
    for key, value in expected.items():
        if values.get(key, value) != value:
            raise ValueError(f"Unsupported production setting: {key}")
    if any(key.startswith("SPRING_DATASOURCE_") or key.startswith("SPRING_JPA_")
           or key in ("SPRING_APPLICATION_JSON", "SPRING_CONFIG_LOCATION", "SPRING_CONFIG_ADDITIONAL_LOCATION")
           for key in values):
        raise ValueError("Use the documented DB_* settings; overriding Spring database/config settings is unsupported")
    email, password = values.get("ADMIN_EMAIL", ""), values.get("ADMIN_PASSWORD", "")
    if bool(email) != bool(password):
        raise ValueError("Set both administrator bootstrap values or neither")
    if password in ("Admin@123", "CHANGE_ME_Strong!Pass123"):
        raise ValueError("Default administrator bootstrap credentials are forbidden")
    return values


if __name__ == "__main__":
    try:
        validate(sys.argv[1])
    except (OSError, ValueError, IndexError):
        # Never include exception text: it may contain file contents or secret values.
        print("Release environment validation failed; check permissions and the release runbook.", file=sys.stderr)
        sys.exit(1)
    print("Release environment validation passed (values withheld).")
