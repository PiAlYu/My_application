# Store Checklist: Android + iPhone PWA

Проект состоит из:
- Android-приложения (`app/`)
- PWA для iPhone (`ios_pwa/`)
- backend на Cloudflare Workers + D1 (`cloudflare_worker/`)
- legacy Python backend для локальных сценариев (`local_server/`)

Основной серверный путь теперь: **Cloudflare Workers + D1**.

## Архитектура API

Оба клиента (Android и PWA) используют одинаковые endpoint'ы:
- `GET /health`
- `GET /api/checklists`
- `PUT /api/checklists`

Авторизация токенами:
- `Read token` для чтения (`GET /api/checklists`)
- `Write token` для записи (`PUT /api/checklists`)

Токены передаются в заголовке:

```http
Authorization: Bearer <token>
```

## Быстрый старт backend (Cloudflare)

Подробности: `cloudflare_worker/README.md`

Коротко:

1. Войти в Cloudflare:

```bash
npx wrangler@latest login
```

2. Создать D1 базу:

```bash
npx wrangler@latest d1 create store-checklist-db --location=enam
```

3. Вставить `database_id` в `cloudflare_worker/wrangler.toml`

4. Применить миграции:

```bash
npx wrangler@latest d1 migrations apply DB --remote
```

5. Задать секреты:

```bash
npx wrangler@latest secret put CHECKLIST_READ_TOKEN
npx wrangler@latest secret put CHECKLIST_WRITE_TOKEN
```

6. Задеплоить Worker:

```bash
npx wrangler@latest deploy
```

7. Взять итоговый URL и использовать в клиентах формат:

```text
https://store-checklist-api.<your-subdomain>.workers.dev/api/
```

## Настройка Android

В экране настроек приложения задайте:
- `Server URL`: `https://store-checklist-api.<your-subdomain>.workers.dev/api/`
- `Read token`: значение `CHECKLIST_READ_TOKEN`
- `Write token`: значение `CHECKLIST_WRITE_TOKEN`

Если `Write token` пустой, приложение использует `Read token` для записи.

### Преднастройка URL/токенов в сборку APK

Можно заранее передать значения через `gradle.properties`:

```properties
storeChecklistServerBaseUrl=https://store-checklist-api.<your-subdomain>.workers.dev/api/
storeChecklistServerReadToken=read-secret
storeChecklistServerWriteToken=write-secret
```

Сборка debug APK:

```powershell
.\gradlew.bat assembleDebug
```

## Настройка iPhone PWA

Откройте PWA, затем в настройках задайте:
- `Server URL`: `https://store-checklist-api.<your-subdomain>.workers.dev/api/`
- `Read token`
- `Write token`

### Локальный запуск PWA

```powershell
cd ios_pwa
.\serve_local.ps1
```

Открыть в браузере: `http://127.0.0.1:8787`

## Перенос данных со старого backend

Если у вас уже есть JSON-массив чек-листов, отправьте его в новый Worker:

```powershell
curl.exe -X PUT `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer <WRITE_TOKEN>" `
  --data-binary "@checklists.json" `
  "https://store-checklist-api.<your-subdomain>.workers.dev/api/checklists"
```

## Legacy Python backend (опционально)

Для локальной отладки или fallback остаётся `local_server/`.
Запуск:

```powershell
.\run_local_server.ps1
```

или с токенами:

```powershell
.\run_local_server.ps1 -ReadToken "read-secret" -WriteToken "write-secret"
```
