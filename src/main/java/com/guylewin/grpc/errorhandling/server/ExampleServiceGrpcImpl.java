package com.guylewin.grpc.errorhandling.server;

import com.guylewin.grpc.errorhandling.BiDirectionalExampleService;
import com.guylewin.grpc.errorhandling.ExampleServiceGrpc;
import com.guylewin.grpc.errorhandling.GenericStreamObserver;
import io.grpc.stub.StreamObserver;

public class ExampleServiceGrpcImpl extends ExampleServiceGrpc.ExampleServiceImplBase {

    private static final String STREAM_OBSERVER_NAME = "Server";
    public final Object incomingConnectionLock = new Object();

    public GenericStreamObserver<BiDirectionalExampleService.RequestCall> serverRequestStreamObserver = new GenericStreamObserver<>(STREAM_OBSERVER_NAME);
    public StreamObserver<BiDirectionalExampleService.ResponseCall> serverResponseStreamObserver;

    @Override
    public StreamObserver<BiDirectionalExampleService.RequestCall> connect(StreamObserver<BiDirectionalExampleService.ResponseCall> responseObserver) {
        // Subsequent connections will override one another
        serverResponseStreamObserver = responseObserver;
        synchronized (incomingConnectionLock) {
            incomingConnectionLock.notifyAll();
        }
        return serverRequestStreamObserver;
    }
}
