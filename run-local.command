#!/bin/zsh
set -e
cd "$(dirname "$0")"
if command -v brew >/dev/null 2>&1 && brew --prefix openjdk@21 >/dev/null 2>&1; then
  export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if [[ -n "${DB_URL:-}" ]]; then
  ./mvnw spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=$DB_URL,--spring.datasource.username=${DB_USER:-bvb_app},--spring.datasource.password=${DB_PASSWORD:-bvb_local_password}"
else
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
fi
