package ru.tinkoff.piapi.core.stream;

public interface StreamProcessor<T> {

  void process(T response);
}
