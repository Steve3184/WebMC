package com.lmax.disruptor;

public interface EventTranslatorTwoArg<T, A, B> {
    void translateTo(T event, long sequence, A arg0, B arg1);
}
