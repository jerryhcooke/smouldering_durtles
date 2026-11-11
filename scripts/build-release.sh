#!/usr/bin/env bash
# Builds the signed release APK for a given semantic version.
# Called by semantic-release during the prepare step, but can also be run manually:
#   ./scripts/build-release.sh 1.3.0
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "usage: $0 <version>" >&2
  exit 1
fi

VERSION="$1"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "unsupported version format (expected X.Y.Z): $VERSION" >&2
  exit 1
fi

# Locate a JDK for Gradle: JAVA_HOME if set, otherwise java on PATH,
# otherwise a well-known JDK location.
if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_BIN="$(command -v java || true)"
  if [ -z "$JAVA_BIN" ] && [ -d /usr/lib/jvm ]; then
    for jvm in /usr/lib/jvm/*; do
      if [ -x "$jvm/bin/java" ]; then
        JAVA_BIN="$jvm/bin/java"
        break
      fi
    done
  fi
  if [ -z "$JAVA_BIN" ]; then
    echo "error: no Java runtime found; set JAVA_HOME or install a JDK" >&2
    exit 1
  fi
  JAVA_HOME="$(cd "$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")" && pwd)"
  export JAVA_HOME
fi

# semver -> versionCode: major*10000 + minor*100 + patch
MAJOR=${VERSION%%.*}
REST=${VERSION#*.}
MINOR=${REST%%.*}
PATCH=${REST#*.}
VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))

export VERSION_NAME="$VERSION"
export VERSION_CODE="$VERSION_CODE"
echo "Building Lazy Durtles v$VERSION (versionCode $VERSION_CODE)"

./gradlew --no-daemon clean :app:assembleRelease

mkdir -p dist
rm -f dist/*.apk
cp app/build/outputs/apk/release/app-release.apk "dist/LazyDurtles-v${VERSION}.apk"
echo "dist/LazyDurtles-v${VERSION}.apk ready"
