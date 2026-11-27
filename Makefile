SHELL := /bin/bash
.DEFAULT_GOAL := help

export DOCKER_COMPOSE ?= docker compose
export MVN ?= mvn
export APP_PORT ?= 8080
export KC_PORT ?= 8180
export BUILD_IMAGE ?= maven:3.9-eclipse-temurin-17-alpine
export RUNTIME_IMAGE ?= eclipse-temurin:17-jre-alpine
APP_URL ?= http://localhost:$(APP_PORT)

.PHONY: help build test up down token

help: ## Список команд
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

build: ## Сборка jar (без тестов)
	$(MVN) -B -DskipTests package

test: ## Прогон всех тестов, включая интеграционные (нужен Docker)
	$(MVN) -B verify

up: ## Запуск стека и ожидание readiness приложения (healthcheck)
	$(DOCKER_COMPOSE) up -d --build
	@echo "Ждём готовности приложения..."
	@ok=0; for i in $$(seq 1 30); do \
		if curl -sf $(APP_URL)/actuator/health > /dev/null; then ok=1; break; fi; \
		sleep 2; \
	done; \
	if [ "$$ok" != "1" ]; then \
		echo "Приложение не стало healthy за 60 секунд — проверьте: $(DOCKER_COMPOSE) logs notes-service"; \
		exit 1; \
	fi
	@echo "Готово: $(APP_URL)"

down: ## Полное удаление из системы: контейнеры, тома и локально собранный образ
	$(DOCKER_COMPOSE) down -v --rmi local

token: ## Access token демо-пользователя (demo/demo)
	curl -s http://localhost:$(KC_PORT)/realms/notes/protocol/openid-connect/token \
		-d grant_type=password -d client_id=notes-cli -d username=demo -d password=demo \
		| sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
