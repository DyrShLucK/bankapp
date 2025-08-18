# Микросервисное приложение "Банк"


## Основные функции
- Регистрация и аутентификация пользователей
- Управление счетами в различных валютах (RUB, USD, EUR)
- Пополнение и снятие виртуальных денег
- Переводы между своими счетами и счетами других пользователей
- Конвертация валют по актуальным курсам
- Уведомления о финансовых операциях
- Блокировка каждого 3 запроса

## Архитектура микросервисов
- **Front Service** - веб-интерфейс приложения
- **Account Service** - управление аккаунтами и счетами
- **Cash Service** - операции с наличными
- **Transfer Service** - переводы между счетами
- **Exchange Service** - конвертация валют
- **Exchange Generator Service** - генерация курсов валют
- **Blocker Service** - блокировка подозрительных операций
- **Notifications Service** - система уведомлений
- **Config Server** - внешняя конфигурация
- **Service Discovery** - регистрация сервисов (Eureka/Consul)
- **API Gateway** - шлюз для межсервисного взаимодействия

## Технологии
- Java 21
- Spring Boot
- Spring Cloud (Gateway, Eureka, Config)
- Spring Security OAuth2
- Spring WebFlux
- PostgreSQL
- Docker
- Docker Compose

## Требования к системе
- Java 21
- Docker и Docker Compose
- PostgreSQL (через Docker)
- Доступ к интернету для загрузки зависимостей

## Запуск приложения
запустите по отдельносьти bootrun или соберите все вместе
./gradlew build
или используйте docker
желательно сначала заупустить keycloak и config-server затем все остальные

### Запуск через Docker
```bash

docker-compose up --build
