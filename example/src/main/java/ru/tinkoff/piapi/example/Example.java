package ru.tinkoff.piapi.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;
import ru.tinkoff.piapi.core.models.FuturePosition;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.SecurityPosition;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

public class Example {
  static final Logger log = LoggerFactory.getLogger(Example.class);

  public static void main(String[] args) {
    //Figi инструмента, который будет использоваться в методах исполнения заявок и стоп-заявок
    //Для тестирования рекомендуется использовать дешевые бумаги
    var instrumentFigi = args[1];

    var token = args[0];
    var api = InvestApi.create(token);
    //Можно создать экземпляр sandbox - тогда все вызовы будут переадресованы в песочницу
    var sandboxApi = InvestApi.createSandbox(token);
    //Можно создать экземпляр readonly - тогда на уровне SDK будут заблокированы вызовы на выставление ордеров во избежании траты средств
    var readonlyToken = InvestApi.createReadonly(token);

    //Примеры unary запросов
    sandboxServiceExample(sandboxApi, instrumentFigi);
    instrumentsServiceExample(api);
    marketdataServiceExample(api);
    operationsServiceExample(api);
    usersServiceExample(api);
    ordersServiceExample(api, instrumentFigi);
    stopOrdersServiceExample(api, instrumentFigi);

    //Примеры подписок на стримы
    marketdataStreamExample(api);
    ordersStreamExample(api);
    portfolioStreamExample(api);
    positionsStreamExample(api);
  }

  private static void positionsStreamExample(InvestApi api) {
    //Server-side stream обновлений информации по изменению позиций портфеля
    StreamProcessor<PositionsStreamResponse> consumer = response -> {
      if (response.hasPing()) {
        log.info("пинг сообщение");
      } else if (response.hasPosition()) {
        log.info("Новые данные по позициям: {}", response);
      }
    };

    Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());
    var accountId1 = "my_account_id1";
    var accountId2 = "my_account_id2";
    //Подписка стрим позиций. Не блокирующий вызов
    //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
    api.getOperationsStreamService().subscribePositions(consumer, onErrorCallback, accountId1);

    //Если обработка ошибок не требуется, то можно использовать перегруженный метод
    api.getOperationsStreamService().subscribePositions(consumer, accountId2);

    //Если требуется подписаться на обновление сразу по нескольким accountId - можно передать список
    api.getOperationsStreamService().subscribePositions(consumer, List.of(accountId1, accountId2));
  }

  private static void portfolioStreamExample(InvestApi api) {
    //Server-side stream обновлений портфеля
    StreamProcessor<PortfolioStreamResponse> consumer = response -> {
      if (response.hasPing()) {
        log.info("пинг сообщение");
      } else if (response.hasPortfolio()) {
        log.info("Новые данные по портфолио: {}", response);
      }
    };

    Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());
    var accountId1 = "my_account_id1";
    var accountId2 = "my_account_id2";
    //Подписка стрим портфолио. Не блокирующий вызов
    //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
    api.getOperationsStreamService().subscribePortfolio(consumer, onErrorCallback, accountId1);

    //Если обработка ошибок не требуется, то можно использовать перегруженный метод
    api.getOperationsStreamService().subscribePortfolio(consumer, accountId2);

    //Если требуется подписаться на обновление сразу по нескольким accountId - можно передать список
    api.getOperationsStreamService().subscribePortfolio(consumer, List.of(accountId1, accountId2));
  }

  private static void ordersStreamExample(InvestApi api) {
    StreamProcessor<TradesStreamResponse> consumer = response -> {
      if (response.hasPing()) {
        log.info("пинг сообщение");
      } else if (response.hasOrderTrades()) {
        log.info("Новые данные по сделкам: {}", response);
      }
    };

    Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

    //Подписка стрим сделок. Не блокирующий вызов
    //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
    api.getOrdersStreamService().subscribeTrades(consumer, onErrorCallback);

    //Если обработка ошибок не требуется, то можно использовать перегруженный метод
    api.getOrdersStreamService().subscribeTrades(consumer);
  }

  private static List<String> randomFigi(InvestApi api, int count) {
    return api.getInstrumentsService().getTradableSharesSync()
      .stream()
      .filter(el -> Boolean.TRUE.equals(el.getApiTradeAvailableFlag()))
      .map(Share::getFigi)
      .limit(count)
      .collect(Collectors.toList());
  }

  private static void marketdataStreamExample(InvestApi api) {
    var randomFigi = randomFigi(api, 5);

    //Описываем, что делать с приходящими в стриме данными
    StreamProcessor<MarketDataResponse> processor = response -> {
      if (response.hasTradingStatus()) {
        log.info("Новые данные по статусам: {}", response);
      } else if (response.hasPing()) {
        log.info("пинг сообщение");
      } else if (response.hasCandle()) {
        log.info("Новые данные по свечам: {}", response);
      } else if (response.hasOrderbook()) {
        log.info("Новые данные по стакану: {}", response);
      } else if (response.hasTrade()) {
        log.info("Новые данные по сделкам: {}", response);
      } else if (response.hasSubscribeCandlesResponse()) {
        var successCount = response.getSubscribeCandlesResponse().getCandlesSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        log.info("удачных подписок на свечи: {}", successCount);
        log.info("неудачных подписок на свечи: {}", errorCount);
      } else if (response.hasSubscribeInfoResponse()) {
        var successCount = response.getSubscribeInfoResponse().getInfoSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        log.info("удачных подписок на статусы: {}", successCount);
        log.info("неудачных подписок на статусы: {}", errorCount);
      } else if (response.hasSubscribeOrderBookResponse()) {
        var successCount = response.getSubscribeOrderBookResponse().getOrderBookSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        log.info("удачных подписок на стакан: {}", successCount);
        log.info("неудачных подписок на стакан: {}", errorCount);
      } else if (response.hasSubscribeTradesResponse()) {
        var successCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        log.info("удачных подписок на сделки: {}", successCount);
        log.info("неудачных подписок на сделки: {}", errorCount);
      } else if (response.hasSubscribeLastPriceResponse()) {
        var successCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        var errorCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
        log.info("удачных подписок на последние цены: {}", successCount);
        log.info("неудачных подписок на последние цены: {}", errorCount);
      }
    };
    Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

    //Подписка на список инструментов. Не блокирующий вызов
    //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
    api.getMarketDataStreamService().newStream("trades_stream", processor, onErrorCallback).subscribeTrades(randomFigi);
    api.getMarketDataStreamService().newStream("candles_stream", processor, onErrorCallback).subscribeCandles(randomFigi);
    api.getMarketDataStreamService().newStream("info_stream", processor, onErrorCallback).subscribeInfo(randomFigi);
    api.getMarketDataStreamService().newStream("orderbook_stream", processor, onErrorCallback).subscribeOrderbook(randomFigi);
    api.getMarketDataStreamService().newStream("last_prices_stream", processor, onErrorCallback).subscribeLastPrices(randomFigi);


    //Для стримов стаканов и свечей есть перегруженные методы с дефолтными значениями
    //глубина стакана = 10, интервал свечи = 1 минута
    api.getMarketDataStreamService().getStreamById("trades_stream").subscribeOrderbook(randomFigi);
    api.getMarketDataStreamService().getStreamById("candles_stream").subscribeCandles(randomFigi);

    //Отписка на список инструментов. Не блокирующий вызов
    api.getMarketDataStreamService().getStreamById("trades_stream").unsubscribeTrades(randomFigi);
    api.getMarketDataStreamService().getStreamById("candles_stream").unsubscribeCandles(randomFigi);
    api.getMarketDataStreamService().getStreamById("info_stream").unsubscribeInfo(randomFigi);
    api.getMarketDataStreamService().getStreamById("orderbook_stream").subscribeOrderbook(randomFigi);
    api.getMarketDataStreamService().getStreamById("last_prices_stream").unsubscribeLastPrices(randomFigi);

    //Каждый marketdata стрим может отдавать информацию максимум по 300 инструментам
    //Если нужно подписаться на большее количество, есть 2 варианта:
    // - открыть новый стрим
    api.getMarketDataStreamService().newStream("new_stream", processor, onErrorCallback).subscribeCandles(randomFigi);
    // - отписаться от инструментов в существующем стриме, освободив место под новые
    api.getMarketDataStreamService().getStreamById("new_stream").unsubscribeCandles(randomFigi);
  }

  private static void sandboxServiceExample(InvestApi sandboxApi, String figi) {
    //Открываем новый счет в песочнице
    var accountId = sandboxApi.getSandboxService().openAccountSync();
    log.info("открыт новый аккаунт в песочнице {}", accountId);

    //В sandbox режиме можно делать запросы в те же методы, что и в обычном API
    //Поэтому не придется писать отдельный код для песочницы, чтоб проверить свою стратегию
    var accounts = sandboxApi.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0);
    for (Account account : accounts) {
      log.info("sandbox account id: {}, access level: {}", account.getId(), account.getAccessLevel().name());
    }

    //Убеждаемся, что мы в режиме песочницы
    log.info("тариф должен быть sandbox. фактический тариф: {}", sandboxApi.getUserService().getInfoSync().getTariff());

    //пополняем счет песочницы на 10_000 рублей и 10_000 долларов
    sandboxApi.getSandboxService().payIn(mainAccount.getId(), MoneyValue.newBuilder().setUnits(10000).setCurrency("RUB").build());
    sandboxApi.getSandboxService().payIn(mainAccount.getId(), MoneyValue.newBuilder().setUnits(10000).setCurrency("USD").build());

    //В режиме песочницы недоступны следующие методы:
    //UsersService.GetMarginAttributes
    //Все методы сервиса StopOrdersService.*
    //OperationsService.GetBrokerReport
    //OperationsService.GetDividendsForeignIssuer
    //OperationsService.GetWithdrawLimits
    //OperationsService.GetOperationsByCursor
    //OperationsStreamService.PortfolioStream
    //OrdersStreamService.TradesStream

    //Остальные методы - доступны. При вызове из sandbox режима запрос идет в песочницу, например, получим портфолио
    getPortfolioExample(sandboxApi);
    //Или список позиций
    getPositionsExample(sandboxApi);
    //Выставляем ордер
    ordersServiceExample(sandboxApi, figi);
  }
  private static void usersServiceExample(InvestApi api) {
    //Получаем список аккаунтов и распечатываем их с указанием привилегий токена
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0);
    for (Account account : accounts) {
      log.info("account id: {}, access level: {}", account.getId(), account.getAccessLevel().name());
    }

    //Получаем и печатаем информацию о текущих лимитах пользователя
    var tariff = api.getUserService().getUserTariffSync();
    log.info("stream type: marketdata, stream limit: {}", tariff.getStreamLimitsList().get(0).getLimit());
    log.info("stream type: orders, stream limit: {}", tariff.getStreamLimitsList().get(1).getLimit());
    log.info("current unary limit per minute: {}", tariff.getUnaryLimitsList().get(0).getLimitPerMinute());

    //Получаем и печатаем информацию об обеспеченности портфеля
    var marginAttributes = api.getUserService().getMarginAttributesSync(mainAccount.getId());
    log.info("Ликвидная стоимость портфеля: {}", moneyValueToBigDecimal(marginAttributes.getLiquidPortfolio()));
    log.info("Начальная маржа — начальное обеспечение для совершения новой сделки: {}",
      moneyValueToBigDecimal(marginAttributes.getStartingMargin()));
    log.info("Минимальная маржа — это минимальное обеспечение для поддержания позиции, которую вы уже открыли: {}",
      moneyValueToBigDecimal(marginAttributes.getMinimalMargin()));
    log.info("Уровень достаточности средств. Соотношение стоимости ликвидного портфеля к начальной марже: {}",
      quotationToBigDecimal(marginAttributes.getFundsSufficiencyLevel()));
    log.info("Объем недостающих средств. Разница между стартовой маржой и ликвидной стоимости портфеля: {}",
      moneyValueToBigDecimal(marginAttributes.getAmountOfMissingFunds()));
  }

  private static void stopOrdersServiceExample(InvestApi api, String figi) {

    //Выставляем стоп-заявку
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    var lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
    var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
    var stopPrice = Quotation.newBuilder().setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
      .setNano(lastPrice.getNano() - minPriceIncrement.getNano() * 100).build();
    var stopOrderId = api.getStopOrdersService()
      .postStopOrderGoodTillDateSync(figi, 1, stopPrice, stopPrice, StopOrderDirection.STOP_ORDER_DIRECTION_BUY,
        mainAccount, StopOrderType.STOP_ORDER_TYPE_STOP_LOSS, Instant.now().plus(1, ChronoUnit.DAYS));
    log.info("выставлена стоп-заявка. id: {}", stopOrderId);

    //Получаем список стоп-заявок и смотрим, что наша заявка в ней есть
    var stopOrders = api.getStopOrdersService().getStopOrdersSync(mainAccount);
    stopOrders.stream().filter(el -> el.getStopOrderId().equals(stopOrderId)).findAny().orElseThrow();

    //Отменяем созданную стоп-заявку
    api.getStopOrdersService().cancelStopOrder(mainAccount, stopOrderId);
    log.info("стоп заявка с id {} отменена", stopOrderId);
  }

  private static void ordersServiceExample(InvestApi api, String figi) {
    //Выставляем заявку
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    var lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
    var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
    var price = Quotation.newBuilder().setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
      .setNano(lastPrice.getNano() - minPriceIncrement.getNano() * 100).build();

    //Выставляем заявку на покупку по лимитной цене
    var orderId = api.getOrdersService()
      .postOrderSync(figi, 1, price, OrderDirection.ORDER_DIRECTION_BUY, mainAccount, OrderType.ORDER_TYPE_LIMIT,
        UUID.randomUUID().toString()).getOrderId();

    //Получаем список активных заявок, проверяем наличие нашей заявки в списке
    var orders = api.getOrdersService().getOrdersSync(mainAccount);
    if (orders.stream().anyMatch(el -> orderId.equals(el.getOrderId()))) {
      log.info("заявка с id {} есть в списке активных заявок", orderId);
    }

    //Отменяем заявку
    api.getOrdersService().cancelOrder(mainAccount, orderId);
  }

  private static void operationsServiceExample(InvestApi api) {
    getOperationsExample(api);
    getOperationsByCursorExample(api);
    getPositionsExample(api);
    getPortfolioExample(api);
    getWithdrawLimitsExample(api);
    getBrokerReportExample(api);
    getDividendsForeignIssuer(api);
  }

  private static void marketdataServiceExample(InvestApi api) {
    getCandlesExample(api);
    getOrderbookExample(api);
    getLastPricesExample(api);
    getTradingStatusExample(api);
    getLastTradesExample(api);
    getClosePricesExample(api);
  }

  private static void getBrokerReportExample(InvestApi api) {
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    //Запрашиваем отчет
    var response = api.getOperationsService().getBrokerReportSync(mainAccount, Instant.now().minus(30, ChronoUnit.DAYS), Instant.now());
    if (response.hasGenerateBrokerReportResponse()) {
      //Если отчет не готов - вернется task_id
      log.info("task_id: {}", response.getGenerateBrokerReportResponse().getTaskId());
    } else if (response.hasGetBrokerReportResponse()) {
      //Если отчет уже готов - вернется отчет
      var report = response.getGetBrokerReportResponse();
      log.info("отчет содержит в себе {} позиций", report.getItemsCount());
    }


    //Можно запрашивать готовый отчет по task_id
    var taskId = "feb07b1f-300a-438c-ad01-4798c74e915e";
    //Асинхронно
    var report = api.getOperationsService().getBrokerReport(taskId, 0).join();
    log.info("отчет содержит в себе {} позиций", report.getItemsCount());

    //Синхронно
    var reportSync = api.getOperationsService().getBrokerReportSync(taskId, 0);
    log.info("отчет содержит в себе {} позиций", reportSync.getItemsCount());
  }

  private static void getDividendsForeignIssuer(InvestApi api) {
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    //Запрашиваем справку о доходах за пределами РФ
    var response = api.getOperationsService().getDividendsForeignIssuerSync(mainAccount, Instant.now().minus(30, ChronoUnit.DAYS), Instant.now());
    var taskId = "feb07b1f-300a-438c-ad01-4798c74e915e";

    if (response.hasGenerateDivForeignIssuerReportResponse()) {
      //Если отчет не готов - вернется task_id
      taskId = response.getGenerateDivForeignIssuerReportResponse().getTaskId();
      log.info("task_id: {}", taskId);
    } else if (response.hasDivForeignIssuerReport()) {
      //Если отчет уже готов - вернется отчет
      var report = response.getDivForeignIssuerReport();
      log.info("отчет содержит в себе {} позиций", report.getItemsCount());
    }

    //Можно запрашивать готовый отчет по task_id
    //Асинхронно
    var report = api.getOperationsService().getDividendsForeignIssuer(taskId, 0).join();
    log.info("отчет содержит в себе {} позиций", report.getItemsCount());

    //Синхронно
    var reportSync = api.getOperationsService().getDividendsForeignIssuerSync(taskId, 0);
    log.info("отчет содержит в себе {} позиций", reportSync.getItemsCount());
  }


  private static void getWithdrawLimitsExample(InvestApi api) {
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    var withdrawLimits = api.getOperationsService().getWithdrawLimitsSync(mainAccount);
    var money = withdrawLimits.getMoney();
    var blocked = withdrawLimits.getBlocked();
    var blockedGuarantee = withdrawLimits.getBlockedGuarantee();

    log.info("доступный для вывода остаток для счета {}", mainAccount);
    log.info("массив валютных позиций");
    for (Money moneyValue : money) {
      log.info("валюта: {}, количество: {}", moneyValue.getCurrency(), moneyValue);
    }

    log.info("массив заблокированных валютных позиций портфеля");
    for (Money moneyValue : blocked) {
      log.info("валюта: {}, количество: {}", moneyValue.getCurrency(), moneyValue);
    }

    log.info("заблокировано под гарантийное обеспечение фьючерсов");
    for (Money moneyValue : blockedGuarantee) {
      log.info("валюта: {}, количество: {}", moneyValue.getCurrency(), moneyValue);
    }
  }

  private static void getPortfolioExample(InvestApi api) {
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    //Получаем и печатаем портфолио
    var portfolio = api.getOperationsService().getPortfolioSync(mainAccount);
    var totalAmountBonds = portfolio.getTotalAmountBonds();
    log.info("общая стоимость облигаций в портфеле {}", totalAmountBonds);

    var totalAmountEtf = portfolio.getTotalAmountEtfs();
    log.info("общая стоимость фондов в портфеле {}", totalAmountEtf);

    var totalAmountCurrencies = portfolio.getTotalAmountCurrencies();
    log.info("общая стоимость валют в портфеле {}", totalAmountCurrencies);

    var totalAmountFutures = portfolio.getTotalAmountFutures();
    log.info("общая стоимость фьючерсов в портфеле {}", totalAmountFutures);

    var totalAmountShares = portfolio.getTotalAmountShares();
    log.info("общая стоимость акций в портфеле {}", totalAmountShares);

    log.info("текущая доходность портфеля {}", portfolio.getExpectedYield());

    var positions = portfolio.getPositions();
    log.info("в портфолио {} позиций", positions.size());
    for (int i = 0; i < Math.min(positions.size(), 5); i++) {
      var position = positions.get(i);
      var figi = position.getFigi();
      var quantity = position.getQuantity();
      var currentPrice = position.getCurrentPrice();
      var expectedYield = position.getExpectedYield();
      log.info(
        "позиция с figi: {}, количество инструмента: {}, текущая цена инструмента: {}, текущая расчитанная " +
          "доходность: {}",
        figi, quantity, currentPrice, expectedYield);
    }

  }

  private static void getPositionsExample(InvestApi api) {
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();
    //Получаем и печатаем список позиций
    var positions = api.getOperationsService().getPositionsSync(mainAccount);

    log.info("список валютных позиций портфеля");
    var moneyList = positions.getMoney();
    for (Money moneyValue : moneyList) {
      log.info("валюта: {}, количество: {}", moneyValue.getCurrency(), moneyValue);
    }

    log.info("список заблокированных валютных позиций портфеля");
    var blockedList = positions.getBlocked();
    for (Money moneyValue : blockedList) {
      log.info("валюта: {}, количество: {}", moneyValue.getCurrency(), moneyValue);
    }

    log.info("список ценно-бумажных позиций портфеля");
    var securities = positions.getSecurities();
    for (SecurityPosition security : securities) {
      var figi = security.getFigi();
      var balance = security.getBalance();
      var blocked = security.getBlocked();
      log.info("figi: {}, текущий баланс: {}, заблокировано: {}", figi, balance, blocked);
    }

    log.info("список фьючерсов портфеля");
    var futuresList = positions.getFutures();
    for (FuturePosition security : futuresList) {
      var figi = security.getFigi();
      var balance = security.getBalance();
      var blocked = security.getBlocked();
      log.info("figi: {}, текущий баланс: {}, заблокировано: {}", figi, balance, blocked);
    }
  }

  private static void getOperationsByCursorExample(InvestApi api) {
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    //Получаем и печатаем список операций клиента
    var operations = api.getOperationsService()
      .getOperationByCursorSync(mainAccount, Instant.now().minus(30, ChronoUnit.DAYS), Instant.now())
      .getItemsList();
    for (int i = 0; i < Math.min(operations.size(), 5); i++) {
      var operation = operations.get(i);
      var date = timestampToString(operation.getDate());
      var state = operation.getState().name();
      var id = operation.getId();
      var payment = moneyValueToBigDecimal(operation.getPayment());
      var figi = operation.getFigi();
      log.info("операция с id: {}, дата: {}, статус: {}, платеж: {}, figi: {}", id, date, state, payment, figi);
    }

    //Метод так же позволяет отфильтровать операции по многим параметрам
    operations = api.getOperationsService()
      .getOperationByCursorSync(
        mainAccount,
        Instant.now().minus(30, ChronoUnit.DAYS),
        Instant.now(),
        null,
        10,
        OperationState.OPERATION_STATE_EXECUTED,
        "BBG00RPRPX12",
        true,
        true,
        true,
        List.of(OperationType.OPERATION_TYPE_BUY, OperationType.OPERATION_TYPE_SELL))
      .getItemsList();

    for (int i = 0; i < Math.min(operations.size(), 5); i++) {
      var operation = operations.get(i);
      var date = timestampToString(operation.getDate());
      var state = operation.getState().name();
      var id = operation.getId();
      var payment = moneyValueToBigDecimal(operation.getPayment());
      var figi = operation.getFigi();
      log.info("операция с id: {}, дата: {}, статус: {}, платеж: {}, figi: {}", id, date, state, payment, figi);
    }
  }

  private static void getOperationsExample(InvestApi api) {
    var accounts = api.getUserService().getAccountsSync();
    var mainAccount = accounts.get(0).getId();

    //Получаем и печатаем список операций клиента
    var operations = api.getOperationsService()
      .getAllOperationsSync(mainAccount, Instant.now().minus(30, ChronoUnit.DAYS), Instant.now());
    for (int i = 0; i < Math.min(operations.size(), 5); i++) {
      var operation = operations.get(i);
      var date = timestampToString(operation.getDate());
      var state = operation.getState().name();
      var id = operation.getId();
      var payment = moneyValueToBigDecimal(operation.getPayment());
      var figi = operation.getFigi();
      log.info("операция с id: {}, дата: {}, статус: {}, платеж: {}, figi: {}", id, date, state, payment, figi);
    }
  }

  private static void getInstrumentByExample(InvestApi api) {
    //Поиск инструмента по ticker/classcode/uid/name (в результатах поиска нет опционов)
    var sberName = api.getInstrumentsService().findInstrumentSync("Сбербанк");
    var sberFigi = api.getInstrumentsService().findInstrumentSync("BBG004730N88");
    var sberUid = api.getInstrumentsService().findInstrumentSync("e6123145-9665-43e0-8413-cd61b8aa9b13");
    var sberTicker = api.getInstrumentsService().findInstrumentSync("SBER");


    //Синхронный поиск по figi (недоступно для опционов)
    var sharesSyncByFigi = api.getInstrumentsService().getShareByFigiSync("BBG004730N88"); //Сбербанк
    var currenciesSyncByFigi = api.getInstrumentsService().getCurrencyByFigiSync("BBG0013HGFT4"); //usd
    var etfsSyncByFigi = api.getInstrumentsService().getEtfByFigiSync(("BBG000000001")); //Тинькофф Вечный портфель RUB
    var bondsSyncByFigi = api.getInstrumentsService().getBondByFigiSync(("BBG00NHJGKN2")); //Европлан БО 05
    var futuresSyncByFigi = api.getInstrumentsService().getFutureByFigiSync(("FUTBR0323000")); //BR-3.23 Нефть Brent

    //Асинхронный поиск по figi (недоступно для опционов)
    var sharesAsync = api.getInstrumentsService().getShareByFigi("BBG004730N88"); //Сбербанк
    var currenciesAsync = api.getInstrumentsService().getCurrencyByFigi("BBG0013HGFT4"); //usd
    var etfsAsync = api.getInstrumentsService().getEtfByFigi(("BBG000000001")); //Тинькофф Вечный портфель RUB
    var bondsAsync = api.getInstrumentsService().getBondByFigi(("BBG00NHJGKN2")); //Европлан БО 05
    var futuresAsync = api.getInstrumentsService().getFutureByFigi(("FUTBR0323000")); //BR-3.23 Нефть Brent

    //Синхронный поиск по uid
    var sharesSyncByUid = api.getInstrumentsService().getShareByUidSync("e6123145-9665-43e0-8413-cd61b8aa9b13"); //Сбербанк
    var currenciesSyncByUid = api.getInstrumentsService().getCurrencyByUidSync("a22a1263-8e1b-4546-a1aa-416463f104d3"); //usd
    var etfsSyncByUid = api.getInstrumentsService().getEtfByUidSync(("e2d0dbac-d354-4c36-a5ed-e5aae42ffc76")); //Тинькофф Вечный портфель RUB
    var bondsSyncByUid = api.getInstrumentsService().getBondByUidSync(("992f7309-0921-48b0-9791-190c9725f498")); //Европлан БО 05
    var futuresSyncByUid = api.getInstrumentsService().getFutureByUidSync(("5905f7a5-196f-4cde-9060-e80a2a425aa2")); //BR-3.23 Нефть Brent
    var optionsSyncByUid = api.getInstrumentsService().getOptionByUidSync(("bfe09100-01c8-4b70-9ed8-61f1f5aafb4e")); //Газпром 24.08

    //Асинхронный поиск по uid
    var sharesAsyncByUid = api.getInstrumentsService().getShareByUid("e6123145-9665-43e0-8413-cd61b8aa9b13"); //Сбербанк
    var currenciesAsyncByUid = api.getInstrumentsService().getCurrencyByUid("a22a1263-8e1b-4546-a1aa-416463f104d3"); //usd
    var etfsAsyncByUid = api.getInstrumentsService().getEtfByUid(("e2d0dbac-d354-4c36-a5ed-e5aae42ffc76")); //Тинькофф Вечный портфель RUB
    var bondsAsyncByUid = api.getInstrumentsService().getBondByUid(("992f7309-0921-48b0-9791-190c9725f498")); //Европлан БО 05
    var futuresAsyncByUid = api.getInstrumentsService().getFutureByUid(("5905f7a5-196f-4cde-9060-e80a2a425aa2")); //BR-3.23 Нефть Brent
    var optionsAsyncByUid = api.getInstrumentsService().getOptionByUid(("bfe09100-01c8-4b70-9ed8-61f1f5aafb4e")); //Газпром 24.08

    //Синхронный поиск по position_uid
    var sharesSyncByPositionUid = api.getInstrumentsService().getShareByPositionUidSync("41eb2102-5333-4713-bf15-72b204c4bf7b"); //Сбербанк
    var currenciesSyncByPositionUid = api.getInstrumentsService().getCurrencyByPositionUidSync("6e97aa9b-50b6-4738-bce7-17313f2b2cc2"); //usd
    var etfsSyncByPositionUid = api.getInstrumentsService().getEtfByPositionUidSync(("8005e2ec-66b3-49ae-9711-a424d9c9b61b")); //Тинькофф Вечный портфель RUB
    var bondsSyncByPositionUid = api.getInstrumentsService().getBondByPositionUidSync(("b6aee87b-066e-4ea6-91de-cc2b94709c8f")); //Европлан БО 05
    var futuresSyncByPositionUid = api.getInstrumentsService().getFutureByPositionUidSync(("1f9cc3c4-0958-45ec-9aa6-c9a577f2b2cc")); //BR-3.23 Нефть Brent
    var optionsSyncByPositionUid = api.getInstrumentsService().getOptionByPositionUidSync(("1f9a310b-ddad-42e2-8b68-50937deac71f")); //Газпром 24.08

    //Асинхронный поиск по position_uid
    var sharesAsyncByPositionUid = api.getInstrumentsService().getShareByPositionUid("41eb2102-5333-4713-bf15-72b204c4bf7b"); //Сбербанк
    var currenciesAsyncByPositionUid = api.getInstrumentsService().getCurrencyByPositionUid("6e97aa9b-50b6-4738-bce7-17313f2b2cc2"); //usd
    var etfsAsyncByPositionUid = api.getInstrumentsService().getEtfByPositionUid(("8005e2ec-66b3-49ae-9711-a424d9c9b61b")); //Тинькофф Вечный портфель RUB
    var bondsAsyncByPositionUid = api.getInstrumentsService().getBondByPositionUid(("b6aee87b-066e-4ea6-91de-cc2b94709c8f")); //Европлан БО 05
    var futuresAsyncByPositionUid = api.getInstrumentsService().getFutureByPositionUid(("1f9cc3c4-0958-45ec-9aa6-c9a577f2b2cc")); //BR-3.23 Нефть Brent
    var optionsAsyncByPositionUid = api.getInstrumentsService().getOptionByPositionUid(("1f9a310b-ddad-42e2-8b68-50937deac71f")); //Газпром 24.08

  }

  private static void instrumentsServiceExample(InvestApi api) {
    //Поиск инструментов по параметрам
    getInstrumentByExample(api);

    //Получаем базовые списки инструментов и печатаем их
    var shares = api.getInstrumentsService().getTradableSharesSync();
    var etfs = api.getInstrumentsService().getTradableEtfsSync();
    var bonds = api.getInstrumentsService().getTradableBondsSync();
    var futures = api.getInstrumentsService().getTradableFuturesSync();
    var currencies = api.getInstrumentsService().getTradableCurrenciesSync();

    //Для 3 акций выводим список событий по выплате дивидендов
    for (int i = 0; i < Math.min(shares.size(), 3); i++) {
      var share = shares.get(i);
      var figi = share.getFigi();
      var dividends =
        api.getInstrumentsService().getDividendsSync(figi, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS));
      for (Dividend dividend : dividends) {
        log.info("дивиденд для figi {}: {}", figi, dividend);
      }
    }

    //Для 3 облигаций выводим список НКД
    for (int i = 0; i < Math.min(bonds.size(), 3); i++) {
      var bond = bonds.get(i);
      var figi = bond.getFigi();
      var accruedInterests = api.getInstrumentsService()
        .getAccruedInterestsSync(figi, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS));
      for (AccruedInterest accruedInterest : accruedInterests) {
        log.info("НКД для figi {}: {}", figi, accruedInterest);
      }
    }

    //Для 3 фьючерсов выводим размер обеспечения
    for (int i = 0; i < Math.min(futures.size(), 3); i++) {
      var future = futures.get(i);
      var figi = future.getFigi();
      var futuresMargin = api.getInstrumentsService().getFuturesMarginSync(figi);
      log.info("гарантийное обеспечение при покупке для figi {}: {}", figi,
        moneyValueToBigDecimal(futuresMargin.getInitialMarginOnBuy()));
      log.info("гарантийное обеспечение при продаже для figi {}: {}", figi,
        moneyValueToBigDecimal(futuresMargin.getInitialMarginOnSell()));
      log.info("шаг цены figi для {}: {}", figi, quotationToBigDecimal(futuresMargin.getMinPriceIncrement()));
      log.info("стоимость шага цены для figi {}: {}", figi,
        quotationToBigDecimal(futuresMargin.getMinPriceIncrementAmount()));
    }

    //Получаем время работы биржи
    var tradingSchedules =
      api.getInstrumentsService().getTradingScheduleSync("spb", Instant.now(), Instant.now().plus(5, ChronoUnit.DAYS));
    for (TradingDay tradingDay : tradingSchedules.getDaysList()) {
      var date = timestampToString(tradingDay.getDate());
      var startDate = timestampToString(tradingDay.getStartTime());
      var endDate = timestampToString(tradingDay.getEndTime());
      if (tradingDay.getIsTradingDay()) {
        log.info("расписание торгов для площадки SPB. Дата: {},  открытие: {}, закрытие: {}", date, startDate, endDate);
      } else {
        log.info("расписание торгов для площадки SPB. Дата: {}. Выходной день", date);
      }
    }

    //Получаем инструмент по его figi
    var instrument = api.getInstrumentsService().getInstrumentByFigiSync("BBG000B9XRY4");
    log.info(
      "инструмент figi: {}, лотность: {}, текущий режим торгов: {}, признак внебиржи: {}, признак доступности торгов " +
        "через api : {}",
      instrument.getFigi(),
      instrument.getLot(),
      instrument.getTradingStatus().name(),
      instrument.getOtcFlag(),
      instrument.getApiTradeAvailableFlag());


    //Проверяем вывод ошибки в лог
    //Проверяем, что будет ошибка 50002. Об ошибках и причинах их возникновения - https://tinkoff.github.io/investAPI/errors/
    var bondFigi = bonds.get(0).getFigi(); //инструмент с типом bond
    try {
      api.getInstrumentsService().getCurrencyByFigiSync(bondFigi);
    } catch (ApiRuntimeException e) {
      log.info(e.toString());
    }

    //Получаем информацию о купонах облигации
    var bondCoupons = api.getInstrumentsService().getBondCouponsSync(bondFigi, Instant.now().minus(30, ChronoUnit.DAYS), Instant.now());
    for (Coupon bondCoupon : bondCoupons) {
      var couponDate = bondCoupon.getCouponDate();
      var couponType = bondCoupon.getCouponType().getDescriptorForType();
      var payment = moneyValueToBigDecimal(bondCoupon.getPayOneBond());
      log.info("выплаты по купонам. дата: {}, тип: {}, выплата на 1 облигацию: {}", couponDate, couponType, payment);
    }

    //Получаем список активов
    var assets = api.getInstrumentsService().getAssetsSync().stream().limit(5).collect(Collectors.toList());
    for (Asset asset : assets) {
      log.info("актив. uid : {}, имя: {}, тип: {}", asset.getUid(), asset.getName(), asset.getType());
    }

    //Получаем подробную информацию о активе
    var uid = assets.get(0).getUid();
    var assetBy = api.getInstrumentsService().getAssetBySync(uid);
    log.info("подробная информация об активе. описание: {}, статус: {}, бренд: {}", assetBy.getDescription(), assetBy.getStatus(), assetBy.getBrand().getInfo());

    //Добавление избранных инструментов
    var instruments = currencies.stream().map(Currency::getFigi).collect(Collectors.toList());
    var favoriteInstruments = api.getInstrumentsService().addFavoritesSync(instruments);

    //Удаление из списка избранных инструментов
    favoriteInstruments = api.getInstrumentsService().deleteFavoritesSync(List.of(currencies.get(0).getFigi()));
  }

  private static void getLastTradesExample(InvestApi api) {

    //Получаем и печатаем последние трейды
    var figi = randomFigi(api, 1).get(0);
    var lastTrades = api.getMarketDataService().getLastTradesSync(figi);
    log.info("последние трейды. количество: {}", lastTrades.size());
  }


  private static void getTradingStatusExample(InvestApi api) {

    //Получаем и печатаем торговый статус инструмента
    var figi = randomFigi(api, 1).get(0);
    var tradingStatus = api.getMarketDataService().getTradingStatusSync(figi);
    log.info("торговый статус для инструмента {} - {}", figi, tradingStatus.getTradingStatus().name());
  }

  private static void getClosePricesExample(InvestApi api) {

    //Получаем и печатаем цены закрытия торговой сессии по инструментам
    var randomFigi = randomFigi(api, 5);
    var closePrices = api.getMarketDataService().getClosePricesSync(randomFigi);
    for (var closePrice : closePrices) {
      var figi = closePrice.getFigi();
      var price = quotationToBigDecimal(closePrice.getPrice());
      var time = timestampToString(closePrice.getTime());
      log.info("цены закрытия торговой сессии по инструменту {}, цена: {}, дата совершения торгов: {}", figi, price, time);
    }
  }

  private static void getLastPricesExample(InvestApi api) {

    //Получаем и печатаем последнюю цену по инструменту
    var randomFigi = randomFigi(api, 5);
    var lastPrices = api.getMarketDataService().getLastPricesSync(randomFigi);
    for (LastPrice lastPrice : lastPrices) {
      var figi = lastPrice.getFigi();
      var price = quotationToBigDecimal(lastPrice.getPrice());
      var time = timestampToString(lastPrice.getTime());
      log.info("последняя цена по инструменту {}, цена: {}, время обновления цены: {}", figi, price, time);
    }

  }

  private static void getOrderbookExample(InvestApi api) {

    //Получаем и печатаем стакан для инструмента
    var figi = randomFigi(api, 1).get(0);
    var depth = 10;
    var orderBook = api.getMarketDataService().getOrderBookSync(figi, depth);
    var asks = orderBook.getAsksList();
    var bids = orderBook.getBidsList();
    var lastPrice = quotationToBigDecimal(orderBook.getLastPrice());
    var closePrice = quotationToBigDecimal(orderBook.getClosePrice());
    log.info(
      "получен стакан по инструменту {}, глубина стакана: {}, количество предложений на покупку: {}, количество " +
        "предложений на продажу: {}, цена последней сделки: {}, цена закрытия: {}",
      figi, depth, bids.size(), asks.size(), lastPrice, closePrice);

    log.info("предложения на покупку");
    for (Order bid : bids) {
      var price = quotationToBigDecimal(bid.getPrice());
      var quantity = bid.getQuantity();
      log.info("количество в лотах: {}, цена: {}", quantity, price);
    }

    log.info("предложения на продажу");
    for (Order ask : asks) {
      var price = quotationToBigDecimal(ask.getPrice());
      var quantity = ask.getQuantity();
      log.info("количество в лотах: {}, цена: {}", quantity, price);
    }
  }

  private static void getCandlesExample(InvestApi api) {

    //Получаем и печатаем список свечей для инструмента
    var figi = randomFigi(api, 1).get(0);
    var candles1min = api.getMarketDataService()
      .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
        CandleInterval.CANDLE_INTERVAL_1_MIN);
    var candles5min = api.getMarketDataService()
      .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
        CandleInterval.CANDLE_INTERVAL_5_MIN);
    var candles15min = api.getMarketDataService()
      .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
        CandleInterval.CANDLE_INTERVAL_15_MIN);
    var candlesHour = api.getMarketDataService()
      .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
        CandleInterval.CANDLE_INTERVAL_HOUR);
    var candlesDay = api.getMarketDataService()
      .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(), CandleInterval.CANDLE_INTERVAL_DAY);

    log.info("получено {} 1-минутных свечей для инструмента с figi {}", candles1min.size(), figi);
    for (HistoricCandle candle : candles1min) {
      printCandle(candle);
    }

    log.info("получено {} 5-минутных свечей для инструмента с figi {}", candles5min.size(), figi);
    for (HistoricCandle candle : candles5min) {
      printCandle(candle);
    }

    log.info("получено {} 15-минутных свечей для инструмента с figi {}", candles15min.size(), figi);
    for (HistoricCandle candle : candles15min) {
      printCandle(candle);
    }

    log.info("получено {} 1-часовых свечей для инструмента с figi {}", candlesHour.size(), figi);
    for (HistoricCandle candle : candlesHour) {
      printCandle(candle);
    }

    log.info("получено {} 1-дневных свечей для инструмента с figi {}", candlesDay.size(), figi);
    for (HistoricCandle candle : candlesDay) {
      printCandle(candle);
    }
  }

  private static void printCandle(HistoricCandle candle) {
    var open = quotationToBigDecimal(candle.getOpen());
    var close = quotationToBigDecimal(candle.getClose());
    var high = quotationToBigDecimal(candle.getHigh());
    var low = quotationToBigDecimal(candle.getLow());
    var volume = candle.getVolume();
    var time = timestampToString(candle.getTime());
    log.info(
      "цена открытия: {}, цена закрытия: {}, минимальная цена за 1 лот: {}, максимальная цена за 1 лот: {}, объем " +
        "торгов в лотах: {}, время свечи: {}",
      open, close, low, high, volume, time);
  }
}
