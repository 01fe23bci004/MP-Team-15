#!/bin/zsh
set -e
cd "$(dirname "$0")"
if command -v brew >/dev/null 2>&1 && brew --prefix openjdk@21 >/dev/null 2>&1; then
  export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
APP_DB_URL="${DB_URL:-jdbc:mysql://localhost:3306/bvb_campus?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata}"
APP_DB_USER="${DB_USER:-bvb_app}"
APP_DB_PASSWORD="${DB_PASSWORD:-bvb_local_password}"
./mvnw spring-boot:run -- \
  --spring.datasource.url="$APP_DB_URL" \
  --spring.datasource.username="$APP_DB_USER" \
  --spring.datasource.password="$APP_DB_PASSWORD" \
  --spring.jpa.hibernate.ddl-auto=update \
  --spring.jpa.open-in-view=false
