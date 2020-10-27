package com.guylewin.grpc.errorhandling.server;

import com.guylewin.grpc.errorhandling.WaitableThread;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.netty.util.NetUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class GrpcExampleServer extends WaitableThread {

    public static String HOST = NetUtil.LOCALHOST.getHostName();
    public int port = randomAvailableTcpPort();
    public ExampleServiceGrpcImpl service = new ExampleServiceGrpcImpl();

    private static int randomAvailableTcpPort() {
        ServerSocket s = null;
        try {
            // ServerSocket(0) results in availability of a free random port
            s = new ServerSocket(0);
            return s.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            assert s != null;
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void innerRun() {
        Server server = NettyServerBuilder
            .forAddress(new InetSocketAddress(HOST, port))
            .addService(service)
            .build();
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ready();
    }
}
