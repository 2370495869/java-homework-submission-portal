#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
./mvnw -B -ntp -DskipTests package
exec java -jar "target/homework-submission-portal.jar" "$@"
