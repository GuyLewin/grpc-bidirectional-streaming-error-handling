package com.guylewin.grpc.errorhandling;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guylewin.grpc.errorhandling.client.GrpcExampleClient;
import com.guylewin.grpc.errorhandling.server.GrpcExampleServer;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrpcErrorsTest {

  GrpcExampleServer grpcExampleServer;
  Thread serverThread;
  GrpcExampleClient grpcExampleClient;
  Thread clientThread;

  // Every wait is limited to 10 seconds, in case the test is waiting on the wrong lock
  int WAIT_MILLISECONDS = 10 * 1000;

  @BeforeEach
  void beforeEach() throws InterruptedException {
    grpcExampleServer = new GrpcExampleServer();
    serverThread = new Thread(grpcExampleServer);
    serverThread.start();
    synchronized (grpcExampleServer.readinessLock) {
      grpcExampleServer.readinessLock.wait(WAIT_MILLISECONDS);
    }

    grpcExampleClient = new GrpcExampleClient(grpcExampleServer.port);
    clientThread = new Thread(grpcExampleClient);
    clientThread.start();
    synchronized (grpcExampleClient.readinessLock) {
      grpcExampleClient.readinessLock.wait(WAIT_MILLISECONDS);
    }

    // Wait for incoming connection from client
    synchronized (grpcExampleServer.service.incomingConnectionLock) {
      grpcExampleServer.service.incomingConnectionLock.wait(WAIT_MILLISECONDS);
    }
  }

  @AfterEach
  void afterEach() {
    grpcExampleClient.shutdown();
    grpcExampleServer.shutdown();
  }

  @Test
  void errorFromClient() throws InterruptedException {
    // Send custom exception from client request stream
    grpcExampleClient.clientRequestStreamObserver.onError(new TestExceptionClass());

    // Expect onError to be called on the server request stream
    synchronized (grpcExampleServer.service.serverRequestStreamObserver.onErrorLock) {
      grpcExampleServer.service.serverRequestStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }
    Throwable serverError = grpcExampleServer.service.serverRequestStreamObserver.thrownErrorRef.get();
    assertSame(StatusRuntimeException.class, serverError.getClass());
    StatusRuntimeException serverStatusRuntimeException = (StatusRuntimeException)serverError;
    // Exception on server isn't aware why the connection is terminated
    assertNull(serverStatusRuntimeException.getCause());
    assertEquals("CANCELLED: client cancelled", serverStatusRuntimeException.getMessage());

    // Expect onError to be called on the client response stream
    Throwable clientError = grpcExampleClient.clientResponseStreamObserver.thrownErrorRef.get();
    assertSame(StatusRuntimeException.class, clientError.getClass());
    StatusRuntimeException clientStatusRuntimeException = (StatusRuntimeException)clientError;
    // Exception on client is aware of originating Exception instance
    assertSame(TestExceptionClass.class, clientStatusRuntimeException.getCause().getClass());
    assertEquals("CANCELLED: Cancelled by client with StreamObserver.onError()", clientStatusRuntimeException.getMessage());
  }

  @Test
  void errorFromServer() throws InterruptedException {
    // Send custom exception from server response stream
    grpcExampleServer.service.serverResponseStreamObserver.onError(new TestExceptionClass());

    // Expect onError to be called on the client response stream
    synchronized (grpcExampleClient.clientResponseStreamObserver.onErrorLock) {
      grpcExampleClient.clientResponseStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }
    Throwable clientError = grpcExampleClient.clientResponseStreamObserver.thrownErrorRef.get();
    assertSame(StatusRuntimeException.class, clientError.getClass());
    StatusRuntimeException clientStatusRuntimeException = (StatusRuntimeException)clientError;
    // Exception on client is unaware why the connection is terminated
    assertNull(clientStatusRuntimeException.getCause());
    assertEquals("UNKNOWN", clientStatusRuntimeException.getMessage());

    // Expect onError to NOT be called on server request stream
    assertNull(grpcExampleServer.service.serverRequestStreamObserver.thrownErrorRef.get());
  }

  @Test
  void clientOnNextAfterErrorFromClient() throws InterruptedException {
    // Send custom exception from client request stream
    grpcExampleClient.clientRequestStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the server request stream
    synchronized (grpcExampleServer.service.serverRequestStreamObserver.onErrorLock) {
      grpcExampleServer.service.serverRequestStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // IllegalStateException is thrown from client after error in client
    assertThrows(IllegalStateException.class, () -> grpcExampleClient.clientRequestStreamObserver.onNext(
        BiDirectionalExampleService.RequestCall.getDefaultInstance()
    ));
  }

  @Test
  void clientOnCompletedAfterErrorFromClient() throws InterruptedException {
    // Send custom exception from client request stream
    grpcExampleClient.clientRequestStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the server request stream
    synchronized (grpcExampleServer.service.serverRequestStreamObserver.onErrorLock) {
      grpcExampleServer.service.serverRequestStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // IllegalStateException is thrown from client after error in client
    assertThrows(IllegalStateException.class, () -> grpcExampleClient.clientRequestStreamObserver.onCompleted());
  }

  @Test
  void serverOnNextAfterErrorFromClient() throws InterruptedException {
    // Send custom exception from client request stream
    grpcExampleClient.clientRequestStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the server request stream
    synchronized (grpcExampleServer.service.serverRequestStreamObserver.onErrorLock) {
      grpcExampleServer.service.serverRequestStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // StatusRuntimeException is thrown from server after error in client (only on onNext)
    assertThrows(StatusRuntimeException.class, () -> grpcExampleServer.service.serverResponseStreamObserver.onNext(
        BiDirectionalExampleService.ResponseCall.getDefaultInstance()
    ));
  }

  @Test
  void serverOnCompletedAfterErrorFromClient() throws InterruptedException {
    // Send custom exception from client request stream
    grpcExampleClient.clientRequestStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the server request stream
    synchronized (grpcExampleServer.service.serverRequestStreamObserver.onErrorLock) {
      grpcExampleServer.service.serverRequestStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // No exception is thrown from server after error in client (only on onCompleted)
    assertDoesNotThrow(() -> grpcExampleServer.service.serverResponseStreamObserver.onCompleted());
  }

  @Test
  void clientOnNextAfterErrorFromServer() throws InterruptedException {
    // Send custom exception from server response stream
    grpcExampleServer.service.serverResponseStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the client response stream
    synchronized (grpcExampleClient.clientResponseStreamObserver.onErrorLock) {
      grpcExampleClient.clientResponseStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // No exception is thrown from client after error in server
    assertDoesNotThrow(() -> grpcExampleClient.clientRequestStreamObserver.onNext(
        BiDirectionalExampleService.RequestCall.getDefaultInstance()
    ));
  }

  @Test
  void clientOnCompletedAfterErrorFromServer() throws InterruptedException {
    // Send custom exception from server response stream
    grpcExampleServer.service.serverResponseStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the client response stream
    synchronized (grpcExampleClient.clientResponseStreamObserver.onErrorLock) {
      grpcExampleClient.clientResponseStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // No exception is thrown from client after error in server
    assertDoesNotThrow(() -> grpcExampleClient.clientRequestStreamObserver.onCompleted());
  }

  @Test
  void serverOnNextAfterErrorFromServer() throws InterruptedException {
    // Send custom exception from server response stream
    grpcExampleServer.service.serverResponseStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the client response stream
    synchronized (grpcExampleClient.clientResponseStreamObserver.onErrorLock) {
      grpcExampleClient.clientResponseStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // IllegalStateException is thrown from server after error in server
    assertThrows(IllegalStateException.class, () -> grpcExampleServer.service.serverResponseStreamObserver.onNext(
        BiDirectionalExampleService.ResponseCall.getDefaultInstance()
    ));
  }

  @Test
  void serverOnCompletedAfterErrorFromServer() throws InterruptedException {
    // Send custom exception from server response stream
    grpcExampleServer.service.serverResponseStreamObserver.onError(new TestExceptionClass());

    // Wait for onError to be called on the client response stream
    synchronized (grpcExampleClient.clientResponseStreamObserver.onErrorLock) {
      grpcExampleClient.clientResponseStreamObserver.onErrorLock.wait(WAIT_MILLISECONDS);
    }

    // IllegalStateException is thrown from server after error in server
    assertThrows(IllegalStateException.class, () -> grpcExampleServer.service.serverResponseStreamObserver.onCompleted());
  }

  static class TestExceptionClass extends Exception {}
}
