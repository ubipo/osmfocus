#!/usr/bin/env python3

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


VERSION_TAG_RE = re.compile(r"^v?(\d+)\.(\d+)\.(\d+)$")
VERSION_NAME_RE = re.compile(r'^\s*versionName\s*=\s*(".*?")', re.MULTILINE)
VERSION_CODE_RE = re.compile(r'^\s*versionCode\s*=\s*(\d+)', re.MULTILINE)

BUILD_FILE = Path(__file__).resolve().parent.parent / "app" / "build.gradle.kts"


@dataclass(frozen=True)
class Version:
    major: int
    minor: int
    patch: int

    @classmethod
    def parse(cls, version_expr: str) -> "Version":
        without_prefix = version_expr.rsplit("/", 1)[-1]
        match = VERSION_TAG_RE.fullmatch(without_prefix)
        if match:
            major, minor, patch = (int(part) for part in match.groups())
            return cls(
                major=major,
                minor=minor,
                patch=patch,
            )

        raise ValueError(
            "Expected release version expression like main/v1.8.0, v1.8.0, 1.8.0, or any prefixed "
            "variant ending in one of those forms, "
            f"got: {version_expr}"
        )

    @property
    def android_code(self) -> int:
        return self.major * 1_000_000 + self.minor * 1_000 + self.patch

    def __str__(self) -> str:
        return f"{self.major}.{self.minor}.{self.patch}"


def _replace_group_once(pattern: re.Pattern, group: int, replacement: str, text: str) -> str:
    """Replace captured group of exactly one match of *pattern* in *text*."""
    matches = list(pattern.finditer(text))
    if len(matches) != 1:
        raise RuntimeError(
            f"Expected exactly one match for {pattern.pattern!r}, got {len(matches)}"
        )
    m = matches[0]
    return text[: m.start(group)] + replacement + text[m.end(group) :]


def get_latest_git_version() -> Version:
    output = subprocess.check_output(
        ["git", "tag", "--sort=-version:refname"],
        text=True,
    )
    for tag in (line.strip() for line in output.splitlines() if line.strip()):
        try:
            return Version.parse(tag)
        except ValueError:
            continue
    raise ValueError("No release tags found in git tag list")


def stamp_version(version: Version, version_code: int) -> None:
    if str(version) in {"0.0.0", "0.0.1"}:
        raise ValueError(f"{version} is reserved")
    if version_code <= 1:
        raise ValueError(f"{version_code} must be positive and not 0 or 1 (reserved)")

    content = BUILD_FILE.read_text(encoding="utf-8")
    content = _replace_group_once(VERSION_NAME_RE, 1, f'"{version}"', content)
    content = _replace_group_once(VERSION_CODE_RE, 1, str(version_code), content)
    BUILD_FILE.write_text(content, encoding="utf-8")

    print(f"stamped_version_name={version}")
    print(f"stamped_version_code={version_code}")


def handle_version_get(args: argparse.Namespace) -> None:
    version = Version.parse(args.version_expr) if args.version_expr else get_latest_git_version()
    print(f"semver={version}")
    print(f"android_code={version.android_code}")


def handle_stamp(args: argparse.Namespace) -> None:
    stamp_version(Version.parse(args.version_expr), args.version_code)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    get_parser = subparsers.add_parser("get")
    get_parser.add_argument("version_expr", nargs="?")
    get_parser.set_defaults(handler=handle_version_get)

    stamp_parser = subparsers.add_parser("stamp")
    stamp_parser.add_argument("version_expr")
    stamp_parser.add_argument("version_code", type=int)
    stamp_parser.set_defaults(handler=handle_stamp)

    return parser


def main() -> None:
    parser = build_parser()
    raw_args = sys.argv[1:]
    if not raw_args or raw_args[0] not in {"get", "stamp"}:
        raw_args = ["get", *raw_args]
    args = parser.parse_args(raw_args)
    args.handler(args)


if __name__ == "__main__":
    main()
