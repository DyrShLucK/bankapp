# Микросервисное приложение "Банк"


## Основные функции
- Регистрация и аутентификация пользователей
- Управление счетами в различных валютах (RUB, USD, EUR)
- Пополнение и снятие виртуальных денег
- Переводы между своими счетами и счетами других пользователей
- Конвертация валют по актуальным курсам
- Уведомления о финансовых операциях
- Блокировка подозрительных операций и блокировка с вероятностью 33%

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
соберите все вместе

d
- затем используйте докер
- или запустите все микросервисы из корневой папки
#### желательно сначала заупустить keycloak и config-server затем все остальные

### Запуск через Docker
```bash

docker-compose up --build
```
Запуск в Kubernetes (Minikube)
Предварительные требования
Установленный Minikube
Установленный kubectl
Установленный Helm
Достаточно ресурсов на хосте (рекомендуется минимум 4 CPU и 8GB RAM)
1. ## Запуск Minikube
```bash
minikube start --driver=docker 
```
2. ## Сборка и загрузка Docker-образов
### Сборка образов
```bash
./gradlew build
```
#### Загрузка образов в registry
```bash
docker-compose push
```
3. ## Установка приложения через Helm
```bash
helm install bankapp-dev ./charts/bankapp-umbrella \
  -f ./charts/bankapp-umbrella/values-dev.yaml \
  --namespace bankapp-dev \
  --create-namespace \
  --atomic \
  --timeout 2m0s \
  --debug
```
4.## Проверка статуса
```bash
kubectl get pods -n bankapp-dev
kubectl get ingress -n bankapp-dev
minikube service list
```
5. ## Доступ к приложению
### Получить URL для доступа к приложению
```bash
minikube service bankapp-dev-front-service -n bankapp-dev --url
```

# Настройка Jenkins
# Jenkins разворачивается через Docker Compose в папке jenkins/:

```bash
cd jenkins
docker-compose up -d
```
# Для доступа к Jenkins UI используйте: http://localhost:8080

## Основные команды для управления
# Очистка и перезапуск
```bash
kubectl delete configmap nginx-ingress-controller -n ingress-nginx --ignore-not-found
kubectl delete namespace bankapp-dev --ignore-not-found
kubectl delete all --all -n bankapp-dev --ignore-not-found
helm uninstall bankapp-dev --namespace default --ignore-not-found
```
# Перезапуск отдельных сервисов
```bash
kubectl rollout restart deployment/bankapp-dev-account-service -n bankapp-dev
kubectl rollout restart deployment/bankapp-dev-blocker-service -n bankapp-dev
kubectl rollout restart deployment/bankapp-dev-cash-service -n bankapp-dev
kubectl rollout restart deployment/bankapp-dev-exchange-service -n bankapp-dev
kubectl rollout restart deployment/bankapp-dev-exchangegenerator-service -n bankapp-dev
kubectl rollout restart deployment/bankapp-dev-front-service -n bankapp-dev
kubectl rollout restart deployment/bankapp-dev-notification-service -n bankapp-dev
kubectl rollout restart deployment/bankapp-dev-transfer-service -n bankapp-dev
```

# Перезапуск Keycloak
```bash
kubectl rollout restart deployment/keycloak -n bankapp-dev
```
# Редактирование секретов
```bash
# Редактирование секретов
kubectl edit secret bankapp-secret -n bankapp-dev
```

# Редактирование конфигмапов
```bash
kubectl edit configmap bankapp-config -n bankapp-dev
```
# Просмотр логов в реальном времени
```bash
kubectl logs -l app=keycloak -n bankapp-dev -f
```

# Проброс порта для локального доступа
```bash
kubectl port-forward -n bankapp-dev svc/keycloak 8081:8080
```
# после запуска надо настроить keycloak и вписать в secret  после перезапустить сервисы

## Схема CI/CD пайплайна
- Сборка и тестирование - Gradle собирает проект и выполняет тесты
- Сборка Docker-образов - Сборка образов для всех микросервисов
- Загрузка в registry - Загрузка образов в GitHub Container Registry
- Развертывание в DEV - Установка в Minikube через Helm
- Тестирование - Запуск тестов развернутого приложения
- Ручное подтверждение - Ручное подтверждение для продакшена
- Развертывание в PROD - Установка в продакшен-окружение
