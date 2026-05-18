# Cloudflare Workers + D1 backend

Этот backend полностью совместим с текущими клиентами Android и iOS PWA:
- `GET /health`
- `GET /api/checklists`
- `PUT /api/checklists`

Токены передаются через `Authorization: Bearer <token>`.

## 1) Что внутри папки

- `src/index.ts` — Worker API.
- `migrations/0001_init.sql` — схема D1.
- `wrangler.toml` — конфигурация Worker + привязка D1.
- `.dev.vars.example` — пример локальных секретов.

## 2) Подготовка

Требования:
- Cloudflare account.
- Node.js 20+.

Авторизация в Cloudflare:

```bash
npx wrangler@latest login
```

## 3) Создать D1 базу

```bash
npx wrangler@latest d1 create store-checklist-db --location=enam
```

После выполнения команда покажет `database_id`.
Скопируйте его в `wrangler.toml`:

```toml
[[d1_databases]]
binding = "DB"
database_name = "store-checklist-db"
database_id = "REPLACE_WITH_D1_DATABASE_ID" # <-- заменить
```

Примечание:
- `--location=enam` подходит для Северной Америки.
- Для Европы можно использовать `--location=weur`.

## 4) Применить миграцию в D1

```bash
npx wrangler@latest d1 migrations apply DB --remote
```

Проверка структуры таблиц:

```bash
npx wrangler@latest d1 execute DB --remote --command="SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
```

## 5) Настроить секреты токенов

Секреты задаются отдельно для deployed Worker:

```bash
npx wrangler@latest secret put CHECKLIST_READ_TOKEN
npx wrangler@latest secret put CHECKLIST_WRITE_TOKEN
```

Если хотите одинаковый токен для чтения и записи, введите одно и то же значение для обоих секретов.

## 6) Локальный запуск

1. Скопируйте пример:

```powershell
Copy-Item .dev.vars.example .dev.vars
```

2. Поднимите локальный dev-сервер:

```bash
npx wrangler@latest dev
```

3. Примените миграции в локальную базу:

```bash
npx wrangler@latest d1 migrations apply DB --local
```

4. Проверка:

```powershell
curl.exe -i http://127.0.0.1:8787/health
curl.exe -i -H "Authorization: Bearer read-secret-for-local" http://127.0.0.1:8787/api/checklists
```

## 7) Деплой

```bash
npx wrangler@latest deploy
```

После деплоя получите URL вида:

```text
https://store-checklist-api.<your-subdomain>.workers.dev
```

Для приложений используйте:

```text
https://store-checklist-api.<your-subdomain>.workers.dev/api/
```

## 8) Перенос данных из старого backend

Если у вас есть JSON-массив чек-листов старого API, отправьте его одним `PUT`:

```powershell
curl.exe -X PUT `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer <WRITE_TOKEN>" `
  --data-binary "@checklists.json" `
  "https://store-checklist-api.<your-subdomain>.workers.dev/api/checklists"
```

`checklists.json` должен быть массивом объектов формата:

```json
[
  {
    "id": "uuid-or-string",
    "title": "Склад A",
    "updatedAt": 1740000000000,
    "items": [{ "name": "Вода" }, { "name": "Сок" }]
  }
]
```

## 9) Важные замечания

- `PUT /api/checklists` полностью заменяет состояние БД (нужно для режима super user).
- В продакшене используйте только HTTPS URL.
- Для прода предпочтительнее отдельный custom domain, а не только `workers.dev`.
