package dev.pugrilla.altmanager.network;

public interface HttpRequest<T> {
   T send() throws HttpRequestException;
}
