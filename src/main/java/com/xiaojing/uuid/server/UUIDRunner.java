package com.xiaojing.uuid.server;

import com.xiaojing.uuid.bean.UUID;
import com.xiaojing.uuid.common.ConfigUtil;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiaojing on 17/4/26.
 */
public class UUIDRunner {

  public static void main(String[] args) throws Exception{

    Configurator.initialize("log4j2", ConfigUtil.getURLByName("log4j2.xml").getPath());

    UUID.Iface uuidImpl = ContextConfig.getInstance().getInjector().getInstance(UUIDImpl.class);
    TProcessor processor = new UUID.Processor<>(uuidImpl);

    ServerSocket serverSocket = new ServerSocket(9999);
    TServerSocket serverTransport = new TServerSocket(serverSocket);

    TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport)
        .processor(processor)
        .protocolFactory(new TBinaryProtocol.Factory())
        .minWorkerThreads(1024)
        .maxWorkerThreads(1024)
        .requestTimeout(1000).requestTimeoutUnit(TimeUnit.MILLISECONDS);
    TServer server = new TThreadPoolServer(serverArgs);
    server.serve();
  }
}
