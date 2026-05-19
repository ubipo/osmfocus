#!/usr/bin/env bash
set -euo pipefail

SEMVER_RE='^v([0-9]+)\.([0-9]+)\.([0-9]+)$'

latest_semver_tag() {
  local tag
  while IFS= read -r tag; do
    [[ -n "$tag" ]] || continue
    [[ "$tag" =~ $SEMVER_RE ]] || continue
    printf '%s\n' "$tag"
    return 0
  done < <(git tag --list --sort=-v:refname)

  echo "No semantic version tag found. Expected tags like v1.8.0." >&2
  return 1
}

get_android_code() {
  local padded
  padded="$(printf '%03d%03d%03d' "$1" "$2" "$3")"
  printf '%s\n' "$((10#$padded))"
}

is_head_tagged() {
  local wanted_tag="$1"
  local head_commit tag_commit
  head_commit="$(git rev-parse HEAD)"
  tag_commit="$(git rev-parse "${wanted_tag}^{commit}")"
  [[ "$head_commit" == "$tag_commit" ]]
}

main() {
  local tag major minor patch semver android_code fdroid_version_name
  cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

  tag="$(latest_semver_tag)"
  IFS=. read -r major minor patch <<< "${tag#v}"
  semver="${major}.${minor}.${patch}"
  android_code="$(get_android_code "$major" "$minor" "$patch")"
  fdroid_version_name="${semver}-fdroid"

  printf 'release_tag=%s\n' "$tag"
  printf 'semver=%s\n' "$semver"
  printf 'android_code=%s\n' "$android_code"
  printf 'is_head_tagged=%s\n' "$(is_head_tagged "$tag" && printf true || printf false)"
  printf 'fdroid_version_name=%s\n' "$fdroid_version_name"
  printf 'fdroid_tag=fdroid/v%s+%s\n' "$fdroid_version_name" "$android_code"
}

main "$@"
