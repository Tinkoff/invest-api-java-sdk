package ru.tinkoff.piapi.core.stream;

import ru.tinkoff.piapi.contract.v1.MarketDataResponse;
import ru.tinkoff.piapi.contract.v1.MarketDataStreamServiceGrpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MarketDataStreamService {

  private final MarketDataStreamServiceGrpc.MarketDataStreamServiceStub stub;
  private final Map<String, MarketDataSubscriptionService> streamMap = new ConcurrentHashMap<>();

  public MarketDataStreamService(MarketDataStreamServiceGrpc.MarketDataStreamServiceStub stub) {
    this.stub = stub;
  }

  public int streamCount() {
    return streamMap.size();
  }

  public MarketDataSubscriptionService getStreamById(String id) {
    return streamMap.get(id);
  }

  public Map<String, MarketDataSubscriptionService> getAllStreams() {
    return streamMap;
  }

  public MarketDataSubscriptionService newStream(@Nonnull String id,
                                                 @Nonnull StreamProcessor<MarketDataResponse> streamProcessor,
                                                 @Nullable Consumer<Throwable> onErrorCallback) {
    if (streamMap.containsKey(id)) {
      var existSubscriptionService = streamMap.get(id);
      existSubscriptionService.cancel();
    }
    var subscriptionService = new MarketDataSubscriptionService(stub, streamProcessor, onErrorCallback);
    streamMap.put(id, subscriptionService);
    return subscriptionService;
  }
}
