#!/usr/bin/env python3
import re
import subprocess
from pathlib import Path


SEMVER_RE = re.compile(r"^v(\d+)\.(\d+)\.(\d+)$")


def get_git_tags(root: Path) -> set[str]:
    return {
        tag.strip()
        for tag in subprocess.run(
            ["git", "tag", "--list", "--sort=-v:refname"],
            cwd=root,
            check=True,
            capture_output=True,
            text=True,
        ).stdout.splitlines()
        if tag.strip()
    }


def latest_semver_tag(root: Path) -> tuple[str, tuple[int, int, int]]:
    best = max(
        (
            (version, tag)
            for tag in get_git_tags(root)
            if (match := SEMVER_RE.fullmatch(tag))
            for version in [tuple(int(part) for part in match.groups())]
        ),
        default=None,
    )

    if best is None:
        raise SystemExit("No semantic version tag found. Expected tags like v1.8.0.")

    version, tag = best
    return tag, version


def android_code(version: tuple[int, int, int]) -> int:
    return int("".join(f"{part:03d}" for part in version))


def is_head_tag(tag: str, root: Path) -> bool:
    head_tags = subprocess.run(
        ["git", "tag", "--points-at", "HEAD", "--sort=-v:refname"],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    ).stdout.splitlines()
    return tag in {head_tag.strip() for head_tag in head_tags if SEMVER_RE.fullmatch(head_tag.strip())}


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    tag, version = latest_semver_tag(root)
    semver = ".".join(str(part) for part in version)
    code = android_code(version)
    fdroid_version_name = f"{semver}-fdroid"
    print(f"release_tag={tag}")
    print(f"semver={semver}")
    print(f"android_code={code}")
    print(f"is_head_tagged={str(is_head_tag(tag, root)).lower()}")
    print(f"fdroid_version_name={fdroid_version_name}")
    print(f"fdroid_tag=fdroid/v{fdroid_version_name}+{code}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
