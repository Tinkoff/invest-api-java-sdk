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

    //Примеры unary запросов
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
  }

  private static void portfolioStreamExample(InvestApi api) {
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

  private static void instrumentsServiceExample(InvestApi api) {
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
