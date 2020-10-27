package com.guylewin.grpc.errorhandling;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicReference;

public class GenericStreamObserver<T> implements StreamObserver<T> {

  public final Object onNextLock = new Object();
  public AtomicReference<Throwable> thrownErrorRef = new AtomicReference<>();
  public final Object onErrorLock = new Object();
  public final Object onCompletedLock = new Object();
  private final String name;

  public GenericStreamObserver(String name) {
    this.name = name;
  }

  private void printWithName(String message) {
    System.out.printf("%s: %s\n", name, message);
  }

  @Override
  public void onNext(T value) {
    printWithName("onNext");
    synchronized (onNextLock) {
      onNextLock.notifyAll();
    }
  }

  @Override
  public void onError(Throwable t) {
    printWithName("onError");
    thrownErrorRef.set(t);
    t.printStackTrace();
    synchronized (onErrorLock) {
      onErrorLock.notifyAll();
    }
  }

  @Override
  public void onCompleted() {
    printWithName("onCompleted");
    synchronized (onCompletedLock) {
      onCompletedLock.notifyAll();
    }
  }
}
