#!/usr/bin/env bash
# Prints the latest published figmacompare release version (no 'v' prefix), e.g. 0.1.0.
#
# Usage:
#   ./gradlew build -PfigmacompareVersion=$(./scripts/latest-figmacompare-version.sh)
#
# anandbagmar/figmacompare is private, so this needs an authenticated gh CLI (or
# GH_TOKEN set) with at least read access to that repo - the same FIGMACOMPARE_PAT
# already used elsewhere in this repo works.
set -euo pipefail

REPO="anandbagmar/figmacompare"

if ! command -v gh &> /dev/null; then
    echo "Error: gh CLI is not installed. See https://cli.github.com/" >&2
    exit 1
fi

TAG=$(gh release view --repo "$REPO" --json tagName -q .tagName 2>&1) || {
    echo "Error: could not fetch the latest release for $REPO:" >&2
    echo "$TAG" >&2
    exit 1
}

if [[ -z "$TAG" ]]; then
    echo "Error: $REPO has no releases yet." >&2
    exit 1
fi

echo "${TAG#v}"
