package io.dapr.it;

import org.junit.Assert;

import java.io.*;
import java.net.ServerSocket;
import java.util.UUID;
import java.util.concurrent.*;


public class DaprIntegrationTestingRunner {

    private DaprFreePorts daprFreePorts= new DaprFreePorts();
    private Runtime rt = Runtime.getRuntime();
    private Process proc;

    private String successMessage;
    private Class serviceClass;
    private Boolean useAppPort;
    private int sleepTime;
    private String appName;

    private DaprIntegrationTestingRunner(String successMessage, Class serviceClass, Boolean useAppPort, int sleepTime) {
        this.successMessage = successMessage;
        this.serviceClass = serviceClass;
        this.useAppPort = useAppPort;
        this.sleepTime = sleepTime;
        this.generateAppName();
    }

    public static DaprIntegrationTestingRunner createDaprIntegrationTestingRunner(String successMessage, Class serviceClass, Boolean useAppPort, int sleepTime) {
        return new DaprIntegrationTestingRunner(successMessage, serviceClass, useAppPort, sleepTime);
    }


    public DaprFreePorts initializeDapr() throws Exception {
        daprFreePorts.initPorts();

        String daprCommand=this.buildDaprCommand();
        System.out.println(daprCommand);
        proc= rt.exec(daprCommand);


        final Runnable stuffToDo = new Thread(() -> {
            try {
                try (InputStream stdin = proc.getInputStream()) {
                    try(InputStreamReader isr = new InputStreamReader(stdin)) {
                        try (BufferedReader br = new BufferedReader(isr)){
                            String line;
                            while ((line = br.readLine()) != null) {
                                System.out.println(line);
                                if (line.contains(successMessage)) {
                                    break;
                                }
                            }
                        }
                    }

                }
            } catch (IOException ex) {
                Assert.fail(ex.getMessage());
            }
        });

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future future = executor.submit(stuffToDo);
        executor.shutdown(); // This does not cancel the already-scheduled task.
        future.get(1, TimeUnit.MINUTES);
        Thread.sleep(sleepTime);
        return daprFreePorts;
    }

    private static final String DAPR_RUN = "dapr run --app-id %s ";
    private static final String DAPR_COMMAND = " -- mvn exec:java -D exec.mainClass=%s -D exec.classpathScope=\"test\" -Dexec.args=\"-p %d -grpcPort %d -httpPort %d\"";

    private String buildDaprCommand(){
        StringBuilder stringBuilder= new StringBuilder(String.format(DAPR_RUN, this.appName))
                .append(this.useAppPort ? "--app-port " + this.daprFreePorts.appPort : "")
                .append(" --grpc-port ")
                .append(this.daprFreePorts.grpcPort)
                .append(" --port ")
                .append(this.daprFreePorts.httpPort)
                .append(String.format(DAPR_COMMAND, this.serviceClass.getCanonicalName(),this.daprFreePorts.appPort, this.daprFreePorts.grpcPort, this.daprFreePorts.httpPort));
        return stringBuilder.toString();
    }

    private void generateAppName(){

        this.appName=UUID.randomUUID().toString();
    }

    public  void destroyDapr() throws IOException {
        rt.exec("dapr stop --app-id " + this.appName);
        proc.destroy();
    }

    private static Integer findRandomOpenPortOnAllLocalInterfaces() throws Exception {
        try (
                ServerSocket socket = new ServerSocket(0)
        ) {
            return socket.getLocalPort();

        }
    }

    public static class DaprFreePorts
    {
        public void initPorts() throws Exception {
            this.appPort= findRandomOpenPortOnAllLocalInterfaces();
            this.grpcPort= findRandomOpenPortOnAllLocalInterfaces();
            this.httpPort= findRandomOpenPortOnAllLocalInterfaces();
        }

        private int grpcPort;

        public int getGrpcPort() {
            return grpcPort;
        }



        public int getHttpPort() {
            return httpPort;
        }



        public int getAppPort() {
            return appPort;
        }


        private int httpPort;

        private int appPort;
    }
}
