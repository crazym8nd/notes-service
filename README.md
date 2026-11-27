# Notes Service

[![CI](https://github.com/crazym8nd/notes-service/actions/workflows/ci.yml/badge.svg)](https://github.com/crazym8nd/notes-service/actions/workflows/ci.yml)

REST API сервиса «Управление заметками» на Spring Boot: CRUD заметок (title, content,
createdAt, tags), фильтрация по тегу, версионированные пути `/api/v1/notes`,
аутентификация через **Keycloak** (OAuth2 Resource Server), API-first по OpenAPI-контракту,
CI на GitHub Actions.
Хранилище — H2 (in-memory) с миграциями Flyway; observability — endpoints
метрик и трейсов, без локального мониторингового стека.

## Стек

| Область | Технологии |
|---|---|
| Ядро | Java 17, Spring Boot 3.5 (Web MVC, Validation, Security, Actuator), Maven, Lombok, MapStruct |
| Хранилище | H2 (in-memory), Spring Data JPA, миграции Flyway (`V1__init_notes.sql`) |
| Безопасность | Keycloak 26, Spring Security OAuth2 Resource Server (JWT, realm-роли) |
| Контракт | OpenAPI 3.0 (`openapi/notes-api.yaml`), openapi-generator (spring, interfaceOnly), springdoc Swagger UI |
| Observability | Micrometer: `/actuator/prometheus` (метрики), экспорт трейсов по OTLP (включается переменной окружения) |
| Тесты | JUnit 5, Mockito, AssertJ, MockMvc (web-срез), Testcontainers (Keycloak), JaCoCo |
| Инфраструктура | Docker (multi-stage Dockerfile), docker-compose, GitHub Actions |

## Архитектура

```
HTTP-запрос
   │  Bearer JWT
   ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│ SecurityFilterChain  │──▶│ realm_access.roles →         │
│ (OAuth2 RS, JWT)     │   │ authorities notes-reader/writer│
└──────────────────────┘   └──────────────────────────────┘
   ▼
NotesController (implements NotesApi — сгенерирован из OpenAPI)   ← транспорт, коды ответов
   │  NoteRequestMapper: request DTO → NoteDraft (@NotBlank — валидация на границе сервиса)
   ▼
NoteService (интерфейс, javadoc-контракт) ← NoteServiceImpl (@Validated, NoteNormalizer)
   │  NoteResponseMapper: Note → NoteResponseDto
   ▼
NoteRepository (интерфейс, DIP) ◀── JpaNoteRepository → NoteEntity (@CreatedDate) → H2 + Flyway
```

- **Контракт-first**: DTO (`NoteResponseDto`, `NoteCreateRequestDto`, `NoteUpdateRequestDto`, `ProblemDto`)
  и интерфейс `NotesApi` генерируются Maven-плагином из `openapi/notes-api.yaml`; контроллер
  *реализует* сгенерированный интерфейс — соответствие кода контракту проверяет компилятор.
- **Домен отделён от транспорта**: доменные records (`Note`, `NoteDraft`) не совпадают с DTO;
  преобразования — в MapStruct-мапперах (`dto/`: request → draft, response; `repository/jpa/`: entity ↔ domain).
- **Валидация декларативна**: `@NotBlank` на `NoteDraft` + `pattern` в контракте — пустой заголовок
  отклоняется на границе; сервис нормализует (trim, нижний регистр тегов) через `NoteNormalizer`.
- **Аудит через Spring Data**: `@CreatedDate` на entity — `createdAt` заполняется на insert и
  защищён от изменений (`updatable = false`).
- **Ошибки** — RFC 7807 Problem Details (`application/problem+json`) через единый `@RestControllerAdvice`.

### Диаграммы архитектуры (C4 + PlantUML)

Файлы в `docs/`:
- **Context** — [docs/context.puml](docs/context.puml)
- **Container** — [docs/container.puml](docs/container.puml)
- **Component** — [docs/component.puml](docs/component.puml)

Рендерятся любым PlantUML-совместимым плагином или CI; библиотека — C4-PlantUML (включение по URL).

## Быстрый старт

Требования: JDK 17+, Maven 3.9+, Docker (на Windows — Make в Git Bash/WSL либо команды `docker compose` напрямую).

Порты и образы переопределяются без правки файлов: `APP_PORT`, `KC_PORT`,
`BUILD_IMAGE`, `RUNTIME_IMAGE`, `APP_PLATFORM`.

```bash
make up        # Keycloak (:8180) + notes-service (:8080); команда дождётся readiness приложения
make token     # access token демо-пользователя demo/demo
```

Swagger UI: <http://localhost:8080/swagger-ui.html> · OpenAPI JSON: <http://localhost:8080/v3/api-docs>

### Команды

| Команда | Что делает |
|---|---|
| `make build` | Сборка jar (без тестов) |
| `make test` | Прогон всех тестов, включая интеграционные (нужен Docker) |
| `make up` | Запуск стека: Keycloak + notes-service, с ожиданием readiness |
| `make down` | Полное удаление: контейнеры, тома, локальный образ |
| `make token` | Access token демо-пользователя (demo/demo) |

## Keycloak

Realm **notes** импортируется из [infrastructure/keycloak/notes-realm.json](infrastructure/keycloak/notes-realm.json):

| Пользователь | Пароль | Роли |
|---|---|---|
| `demo` | `demo` | `notes-reader`, `notes-writer` |
| `readonly` | `readonly` | `notes-reader` |

Консоль администратора: <http://localhost:8180> (admin/admin).

Операции API, схемы и коды ошибок — в Swagger UI и в [openapi/notes-api.yaml](openapi/notes-api.yaml)
(дублировать их здесь не будем). Все запросы требуют заголовок `Authorization: Bearer <token>`:

```bash
TOKEN=$(make token)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/notes
```

Роли проверяются authorities: чтение доступно `notes-reader` и `notes-writer`,
мутации — только `notes-writer`
(см. [SecurityConfig](src/main/java/com/crazym8nd/notes/config/SecurityConfig.java)).
Токен пользователя `readonly` на мутации даст 403, запрос без токена — 401.

В compose Keycloak фиксирован на внутренний frontend-адрес
(`KC_HOSTNAME=http://keycloak:8080`): токены, полученные хоть с хоста, хоть из сети,
несут issuer внутреннего контура, и сервис валидирует их по своему issuer-uri.

## Тестирование

`make test` прогоняет все уровни: unit (Mockito) → web-срез (MockMvc + security-цепочка) →
интеграционные через HTTP с реальным Keycloak в Testcontainers — на каждый эндпоинт
контракта сценарий и ошибка. Тела запросов — JSON-фикстуры `fixtures/`, данные интеграционных — SQL-фикстура
`@Sql`. Покрытие — JaCoCo (line ≥ 60%, unit и интеграционные суммарно), отчёт — артефакт CI.

## CI/CD

**CI** ([.github/workflows/ci.yml](.github/workflows/ci.yml)): lint → тесты (JDK 17,
`mvn verify`, JaCoCo-отчёт в артефактах) → сборка docker-образа; на `main` образ уходит
в GHCR с тегами `sha-<commit>` и `latest`. Тесты выполняются один раз; образ собирается
без тестов и публикуется с неизменяемым `sha-`тегом — все последующие этапы используют
ровно этот артефакт. Action'ы запинены по SHA, устаревшие прогоны отменяются через
`concurrency`, permissions минимальные.

**CD** ([.github/workflows/cd.yml](.github/workflows/cd.yml)): после успешного CI на `main` —
скан образа и деплой в environment `development`; в `stage` — ручной запуск с выбором тега.
CD никогда не пересобирает
образ: деплоится то, что опубликовал CI.

## Структура репозитория

```
notes-service/
├─ openapi/notes-api.yaml            # контракт (источник генерации)
├─ src/main/java/com/crazym8nd/notes/
│  ├─ api/                           # сгенерировано из контракта (не коммитится, target/)
│  ├─ rest/                          # контроллер
│  ├─ service/                       # интерфейс, impl и нормализатор входных данных
│  ├─ repository/                    # интерфейс репозитория
│  │  └─ jpa/                        # JPA-реализация: entity, Spring Data
│  ├─ domain/                        # доменные модели (records: Note, NoteDraft)
│  ├─ dto/                           # request/response DTO (сгенерированы из контракта) + MapStruct-мапперы
│  ├─ config/                        # security, аудит JPA, конвертер ролей
│  └─ exception/                     # доменные исключения + RFC 7807 advice
├─ src/main/resources/db/migration/  # миграции Flyway
├─ src/test/java/...                 # unit + web-срез + IT; support/ — база IT и загрузчик фикстур
├─ docs/                             # C4-диаграммы архитектуры (PlantUML)
├─ infrastructure/keycloak/          # realm export
├─ .github/workflows/{ci,cd}.yml     # CI и CD пайплайны
├─ Dockerfile / docker-compose.yml / Makefile / .dockerignore
└─ pom.xml
```

## Как создавался проект

Полный цикл разработки (SDLC) выполнен в паре с ИИ-агентом в CLI (opencode) —
от OpenAPI-контракта до CI и ревью.
