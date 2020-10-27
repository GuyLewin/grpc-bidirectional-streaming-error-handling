package com.guylewin.grpc.errorhandling.client;

import com.guylewin.grpc.errorhandling.BiDirectionalExampleService;
import com.guylewin.grpc.errorhandling.ExampleServiceGrpc;
import com.guylewin.grpc.errorhandling.GenericStreamObserver;
import com.guylewin.grpc.errorhandling.WaitableThread;
import com.guylewin.grpc.errorhandling.server.GrpcExampleServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class GrpcExampleClient extends WaitableThread {

    public GenericStreamObserver<BiDirectionalExampleService.ResponseCall> clientResponseStreamObserver;
    public StreamObserver<BiDirectionalExampleService.RequestCall> clientRequestStreamObserver;
    private static final String STREAM_OBSERVER_NAME = "Client";

    private final int serverPort;

    public GrpcExampleClient(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void innerRun() {
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress(GrpcExampleServer.HOST, serverPort)
            .usePlaintext()
            .build();
        ExampleServiceGrpc.ExampleServiceStub service = ExampleServiceGrpc.newStub(channel);
        clientResponseStreamObserver = new GenericStreamObserver<>(STREAM_OBSERVER_NAME);
        clientRequestStreamObserver = service.connect(clientResponseStreamObserver);
        ready();
    }
}
