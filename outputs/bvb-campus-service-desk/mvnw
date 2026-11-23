#!/bin/sh
set -eu
MVN_VERSION=3.9.9
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
WRAP_DIR=".mvn/wrapper/dists/apache-maven-$MVN_VERSION"
MVN="$WRAP_DIR/bin/mvn"
if [ ! -x "$MVN" ]; then
  mkdir -p "$WRAP_DIR"
  ARCHIVE="$WRAP_DIR/apache-maven-$MVN_VERSION-bin.tar.gz"
  curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MVN_VERSION/binaries/apache-maven-$MVN_VERSION-bin.tar.gz" -o "$ARCHIVE"
  tar -xzf "$ARCHIVE" -C "$WRAP_DIR" --strip-components=1
  rm -f "$ARCHIVE"
fi
exec "$MVN" "$@"
