package com.splunk.logging;

public interface IHttpEventCollectorMiddleware<T extends IHttpSenderMiddleware> {
    void add(T middleware);
}