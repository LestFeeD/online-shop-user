# online-shop-user

> **Описание:** REST API на Spring Boot с базой данных PostgreSQL.  
> Поддерживает документацию API через Swagger, сборку и запуск в Docker.

Database layout in pgAdmin 4
![Структура БД](postgFile.pgerd.png)
_________
## Swagger REST API docs:
http://localhost:8090/swagger-ui/index.html#/
_________

## Models

+ city
+ address
+ role_user
+ web_user
+ address_warehouse
+ warehouse
+ ProjectParticipants
+ inventory
+ product
+ cart
+ product_characteristics
+ order_product
+ delivery
+ product_category
+ supply_goods
+ payment_status
+ order_user
+ characteristic
+ mail_supplier
+ supplier
+ payment_type
+ order_status
+ payment_status

_________

## 🐳 Запуск через Docker Compose

В проекте уже настроен `docker-compose.yml`, который поднимает:

- **PostgreSQL** — основная база данных `rinma_shop` (порт `5433` на хосте)
- **user-app** — сервис пользователей (порт `8090`)
- **product-app** — сервис товаров (порт `8091`)
- **Redis** — кэш (порт `6379`)
- **Kafka** — брокер сообщений (порт `9092` внутри контейнера, доступ с хоста через `29092`)

### 1. Сборка и запуск

```bash
docker compose up --build -d

