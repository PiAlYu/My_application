# Store Checklist (Android)

Приложение чек-листов для магазина с двумя ролями:
- Администратор редактирует списки и запускает синхронизацию.
- Пользователь проходит список в режиме скрытия или маркера.

Теперь проект ориентирован не только на одну Wi-Fi сеть, а на публичный сервер, к которому можно подключаться из любой сети.

## Что изменилось

- Android-клиент по-прежнему хранит списки локально и работает офлайн.
- Серверный URL на устройстве можно задавать без пересборки APK.
- Для внешних адресов приложение требует `https://`.
- В приложении появились отдельные поля для `read token` и `write token`.
- Python-сервер умеет защищать `GET /api/checklists` и `PUT /api/checklists` через `Authorization: Bearer ...`.
- Сервер можно поднять локально, на VPS или в Docker-контейнере.

## Структура проекта

- `app/` - Android-приложение на Compose + Room + Retrofit + WorkManager.
- `local_server/` - простой HTTP API и CLI для работы с данными.
- `run_local_server.ps1` - быстрый локальный запуск сервера на Windows.
- `Dockerfile` - контейнеризация серверной части для деплоя на VPS/облако.

## Рекомендуемая схема для работы из разных сетей

1. Поднять сервер на VPS, облачной VM или другом компьютере с белым адресом.
2. Поставить перед ним HTTPS-прокси (`Caddy`, `Nginx`, `Traefik`).
3. Выдать приложению публичный URL вида `https://checklists.example.ru/api/`.
4. Если данные не должны быть публичными, включить `read token`.
5. Если только администратор должен переписывать серверную базу, включить отдельный `write token`.

Без HTTPS тоже можно запустить сервер, но приложение разрешит `http://` только для `localhost` и локальной сети.

## Сервер: локальный запуск

Требования:
- Python 3.10+.

Запуск без токенов:

```powershell
.\run_local_server.ps1
```

Запуск с токенами:

```powershell
.\run_local_server.ps1 -ReadToken "read-secret" -WriteToken "write-secret"
```

Альтернатива без скрипта:

```powershell
py -3 .\local_server\server.py --host 0.0.0.0 --port 8080 --read-token "read-secret" --write-token "write-secret"
```

Проверка:

```powershell
curl http://127.0.0.1:8080/health
curl -H "Authorization: Bearer read-secret" http://127.0.0.1:8080/api/checklists
```

Сервер читает данные из `local_server/data/checklists.json` по умолчанию.

## Сервер: деплой в Docker

Сборка образа:

```powershell
docker build -t store-checklist-server .
```

Запуск контейнера:

```powershell
docker run -d `
  --name store-checklist-server `
  -p 8080:8080 `
  -e CHECKLIST_SERVER_READ_TOKEN=read-secret `
  -e CHECKLIST_SERVER_WRITE_TOKEN=write-secret `
  -v store-checklist-data:/data `
  store-checklist-server
```

В контейнере используются переменные:
- `CHECKLIST_SERVER_HOST` - по умолчанию `0.0.0.0`
- `CHECKLIST_SERVER_PORT` - по умолчанию `8080`
- `CHECKLIST_SERVER_DATA_FILE` - по умолчанию `/data/checklists.json`
- `CHECKLIST_SERVER_READ_TOKEN` - токен для чтения
- `CHECKLIST_SERVER_WRITE_TOKEN` - токен для записи
- `CHECKLIST_SERVER_AUTH_TOKEN` - один общий токен для чтения и записи

Если разворачиваете сервер на платформе вроде Render/Fly.io/другой VM, достаточно пробросить `PORT` и запустить тот же `python local_server/server.py`.

## Управление данными на сервере

Показать все списки:

```powershell
py -3 .\local_server\manage.py list
```

Добавить новый список:

```powershell
py -3 .\local_server\manage.py add-list --title "Склад A" --item "Вода" --item "Сок" --item "Чай"
```

Переименовать список:

```powershell
py -3 .\local_server\manage.py rename-list --id <CHECKLIST_ID> --title "Склад A - утро"
```

Заменить товары списка:

```powershell
py -3 .\local_server\manage.py replace-items --id <CHECKLIST_ID> --item "Батон" --item "Молоко"
```

Удалить список:

```powershell
py -3 .\local_server\manage.py delete-list --id <CHECKLIST_ID>
```

## Настройка Android-приложения

В окне администратора теперь есть три параметра:
- `Server URL`
- `Read token`
- `Write token`

Типичные варианты:
- Только чтение с публичного сервера: укажите `https://.../api/` и при необходимости `read token`.
- Полная админская синхронизация: укажите `https://.../api/`, `read token` и отдельный `write token`.
- Локальная отладка в одной сети: можно использовать `http://192.168.x.x:8080/api/`.

Если `write token` пустой, приложение попробует использовать `read token` и сервер может вернуть `401 Unauthorized`, если запись защищена отдельно.

## Преднастройка URL и токенов при сборке APK

Если хотите, чтобы устройства сразу открывались с нужным сервером, добавьте в `~/.gradle/gradle.properties` или в локальный `gradle.properties`:

```properties
storeChecklistServerBaseUrl=https://checklists.example.ru/api/
storeChecklistServerReadToken=read-secret
storeChecklistServerWriteToken=write-secret
```

После этого эти значения попадут в `BuildConfig` и станут дефолтными для новых установок приложения.

## Сборка APK

Через Android Studio:

1. Откройте проект.
2. Дождитесь Gradle Sync.
3. Для тестовой сборки выберите `Build APK(s)`.
4. Для распространения выберите `Generate Signed Bundle / APK`.

Через Gradle в терминале:

```powershell
.\gradlew.bat assembleDebug
```

## Первичная проверка интернет-сценария

1. Разверните сервер на машине с внешним доступом.
2. Настройте HTTPS и домен, например `https://checklists.example.ru`.
3. Проверьте с любой внешней машины:
   - `curl https://checklists.example.ru/health`
   - `curl -H "Authorization: Bearer read-secret" https://checklists.example.ru/api/checklists`
4. Установите APK на телефон.
5. Введите `Server URL` вида `https://checklists.example.ru/api/`.
6. При необходимости заполните `read token` и `write token`.
7. Нажмите `Sync`.

## Важные замечания

- Если сервер доступен из интернета, лучше всегда использовать HTTPS.
- Открытый наружу `PUT /api/checklists` без токена небезопасен.
- Пользовательские устройства могут хранить локальные списки и работать офлайн, даже если сервер временно недоступен.
- `GET /health` остается без токена, чтобы было удобно проверять доступность сервера и мониторинг.
