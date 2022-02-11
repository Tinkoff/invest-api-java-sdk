[![Version](https://img.shields.io/maven-central/v/ru.tinkoff.piapi/java-sdk?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/ru.tinkoff.piapi/java-sdk)
[![Release](https://jitpack.io/v/Tinkoff/invest-api-java-sdk.svg?style=flat-square)](https://jitpack.io/#Tinkoff/invest-api-java-sdk)
[![License](https://img.shields.io/github/license/Tinkoff/invest-api-java-sdk?style=flat-square&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![GitHub Actions Status](<https://img.shields.io/github/workflow/status/Tinkoff/invest-api-java-sdk/Java CI with Maven?logo=GitHub&style=flat-square>)](https://github.com/Tinkoff/invest-api-java-sdk/actions?query=workflow%3A"Java+CI+with+Maven")

# Java SDK для Tinkoff Invest API

Данный проект представляет собой инструментарий на языке Java для работы с API Тинькофф Инвестиции, который можно
использовать для создания торговых роботов.

## Использование

Для начала работы подключите к вашему проекту core-модуль

```xml
<dependency>
  <groupId>ru.tinkoff.piapi</groupId>
  <artifactId>java-sdk-core</artifactId>
  <version>1.0-M1</version>
</dependency>
```

После чего можно пользоваться инструментарием

```java
import ru.tinkoff.piapi.core.InvestApi;

var token = "<secret-token>";
var api = InvestApi.create(token);

var order = api.getOrdersService().postOrderSync(...)
```

## Сборка

Для сборки библиотеки понадобится Apache Maven версии не ниже 3, а также JDK версии не ниже 11. Затем в терминале
перейдите в директорию проекта и выполните следующую команду

```bash
mvn clean package
```

## Предложения и пожелания к SDK

Смело выносите свои предложения в Issues, задавайте вопросы. Pull Request'ы также принимаются.

## У меня есть вопрос по работе API

Документация к API находится в [отдельном репозитории](https://github.com/Tinkoff/investAPI). Там вы можете задать
вопрос в Issues.
