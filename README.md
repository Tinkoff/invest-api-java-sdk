[![Version](https://img.shields.io/maven-central/v/ru.tinkoff.piapi/java-sdk?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/ru.tinkoff.piapi/java-sdk)
[![Release](https://jitpack.io/v/Tinkoff/invest-api-java-sdk.svg?style=flat-square)](https://jitpack.io/#Tinkoff/invest-api-java-sdk)
[![License](https://img.shields.io/github/license/Tinkoff/invest-api-java-sdk?style=flat-square&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![GitHub Actions Status](<https://img.shields.io/github/workflow/status/Tinkoff/invest-api-java-sdk/Java CI with Maven?logo=GitHub&style=flat-square>)](https://github.com/Tinkoff/invest-api-java-sdk/actions?query=workflow%3A"Java+CI+with+Maven")

# Java SDK для Tinkoff Invest API

Данный проект представляет собой инструментарий на языке Java для работы с API Тинькофф Инвестиции, который можно
использовать для создания торговых роботов.

## Пререквизиты
- Java версии не ниже 11
- Maven версии не ниже 3, либо Gradle версии не ниже 5.0


## Использование

Для начала работы подключите к вашему проекту core-модуль

|     Система сборки     | Код                                                                                                                                                                                                                                                                                                                     |
|:----------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|         Maven          | <b>\<dependency></b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>\<groupId></b>ru.tinkoff.piapi<b>\</groupId></b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>\<artifactId></b>java-sdk-core<b>\</artifactId></b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>\<version></b>1.0-M5<b>\</version></b><br><b>\</dependency></b> |
 | Gradle with Groovy DSL | <b>implementation</b> 'ru.tinkoff.piapi:java-sdk-core:1.0-M5'                                                                                                                                                                                                                                                           |
 | Gradle with Kotlin DSL | <b>implementation</b>("ru.tinkoff.piapi:java-sdk-core:1.0-M5")                                                                                                                                                                                                                                                          |



После этого можно пользоваться инструментарием

```java
import ru.tinkoff.piapi.core.InvestApi;

var token = "<secret-token>";
var api = InvestApi.create(token);

var order = api.getOrdersService().postOrderSync(...)
```

## Сборка

Для сборки перейдите в директорию проекта и выполните одну из следующих команд

|     Система сборки     | Код                |
|:----------------------:|--------------------|
|         Maven          | mvn clean package  |
| Gradle with Groovy DSL | gradle clean build |
| Gradle with Kotlin DSL | gradle clean build |


## Предложения и пожелания к SDK

Смело выносите свои предложения в Issues, задавайте вопросы. Pull Request'ы также принимаются.

## У меня есть вопрос по работе API

Документация к API находится в [отдельном репозитории](https://github.com/Tinkoff/investAPI). Там вы можете задать
вопрос в Issues.
