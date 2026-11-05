.PHONY: help env infra-up infra-down up down build build-java build-python test test-java test-python lint logs ps clean contracts

SHELL := /bin/bash
COMPOSE := docker compose

help:
	@echo "Hospital Platform - common tasks"
	@echo ""
	@echo "  make env           Copy .env.example to .env if missing"
	@echo "  make infra-up      Start infrastructure only (Kafka, Postgres, Redis, Keycloak)"
	@echo "  make up            Start the full platform"
	@echo "  make down          Stop everything"
	@echo "  make build         Build all container images"
	@echo "  make test          Run Java and Python test suites"
	@echo "  make logs          Tail all logs"
	@echo "  make contracts     Regenerate Java/Python models from contracts"
	@echo "  make clean         Remove build artefacts"

env:
	@test -f .env || cp .env.example .env

infra-up: env
	$(COMPOSE) -f docker-compose.infra.yml up -d

infra-down:
	$(COMPOSE) -f docker-compose.infra.yml down

up: env
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

build:
	$(COMPOSE) build

build-java:
	mvn -f services-java/pom.xml -DskipTests package

build-python:
	@for svc in services-python/*/; do \
		if [ -f "$$svc/pyproject.toml" ]; then \
			echo "==> $$svc"; \
			python3 -m pip install -e "$$svc" || exit 1; \
		fi; \
	done

test: test-java test-python

test-java:
	mvn -f services-java/pom.xml test

test-python:
	@for svc in services-python/*/; do \
		if [ -d "$$svc/tests" ]; then \
			echo "==> $$svc"; \
			(cd "$$svc" && python3 -m pytest -q) || exit 1; \
		fi; \
	done

lint:
	mvn -f services-java/pom.xml -q -DskipTests verify
	@command -v ruff >/dev/null 2>&1 && ruff check services-python || true

logs:
	$(COMPOSE) logs -f --tail=200

ps:
	$(COMPOSE) ps

contracts:
	./contracts/codegen.sh

clean:
	mvn -f services-java/pom.xml clean || true
	find services-python -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	rm -rf frontend/*/node_modules frontend/*/dist
