#!/usr/bin/env bash
# Prints the latest figmacompare release tag as-is (e.g. "v1.2.0" - keeping the "v",
# unlike the old GitHub-Packages-based version of this script). JitPack needs the exact
# tag to resolve com.github.anandbagmar:figmacompare:<tag>.
#
# Usage:
#   ./gradlew build -PfigmacompareVersion=$(./scripts/latest-figmacompare-version.sh)
#
# anandbagmar/figmacompare is public, so this is a plain, unauthenticated request - no
# token, no gh CLI dependency.
set -euo pipefail

REPO="anandbagmar/figmacompare"

RESPONSE=$(curl -sf "https://api.github.com/repos/${REPO}/releases/latest") || {
    echo "Error: could not fetch the latest release for ${REPO} from the GitHub API." >&2
    exit 1
}

TAG=$(printf '%s' "$RESPONSE" | grep -o '"tag_name" *: *"[^"]*"' | head -1 | sed -E 's/.*"tag_name" *: *"([^"]*)".*/\1/')

if [[ -z "$TAG" ]]; then
    echo "Error: ${REPO} has no releases yet, or the API response was unexpected:" >&2
    echo "$RESPONSE" >&2
    exit 1
fi

echo "$TAG"
