package com.guylewin.grpc.errorhandling;

public abstract class WaitableThread implements Runnable {

  private boolean shutdown = false;
  public final Object readinessLock = new Object();
  private final Object shutdownLock = new Object();

  public void shutdown() {
    shutdown = true;
    synchronized (shutdownLock) {
      shutdownLock.notify();
    }
  }

  public abstract void innerRun();

  protected void ready() {
    synchronized (readinessLock) {
      readinessLock.notifyAll();
    }
  }

  @Override
  public void run() {
    innerRun();
    synchronized (shutdownLock) {
      while (!shutdown) {
        try {
          shutdownLock.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    System.out.println("Shutting down");
  }
}
