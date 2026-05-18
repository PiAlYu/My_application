# Store Checklist iPhone PWA

PWA-клиент использует тот же API, что и Android:
- `GET /api/checklists`
- `PUT /api/checklists`

Рекомендуемый backend: Cloudflare Workers + D1.

## Быстрый запуск локально

```powershell
cd ios_pwa
.\serve_local.ps1
```

Откройте `http://127.0.0.1:8787`.

## Подключение к Cloudflare Worker

В настройках PWA задайте:
- `Server URL`: `https://store-checklist-api.<your-subdomain>.workers.dev/api/`
- `Read token`: `CHECKLIST_READ_TOKEN`
- `Write token`: `CHECKLIST_WRITE_TOKEN`

Важно:
- URL должен заканчиваться на `/api/`.
- Для внешнего адреса используйте `https://`.

## Установка на iPhone

1. Опубликуйте `ios_pwa/` на HTTPS-хостинге.
2. Откройте сайт в Safari на iPhone.
3. Нажмите `Поделиться` -> `Добавить на экран Домой`.
4. Запустите иконку и сохраните настройки подключения.

## Упаковка в zip

```powershell
cd ios_pwa
.\package_pwa.ps1
```

Скрипт создаст `store-checklist-ios-pwa.zip`.
