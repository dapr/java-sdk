package io.dapr.it;

import org.junit.Assert;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.*;


public class DaprIntegrationTestingRunner {

    private Runtime rt = Runtime.getRuntime();
    private Process proc;

    private String successMessage;
    private Class serviceClass;
    private Boolean isGrpc;
    private int port;
    private int sleepTime;
    private String appName;

    private DaprIntegrationTestingRunner(String successMessage, Class serviceClass, Boolean isGrpc, int port, int sleepTime) {
        this.successMessage = successMessage;
        this.serviceClass = serviceClass;
        this.isGrpc = isGrpc;
        this.port = port;
        this.sleepTime = sleepTime;
        this.generateAppName();
    }

    public static DaprIntegrationTestingRunner createDaprIntegrationTestingRunner(String successMessage, Class serviceClass, Boolean isGrpc, int port, int sleepTime) {
        return new DaprIntegrationTestingRunner(successMessage, serviceClass, isGrpc, port, sleepTime);
    }


    public void initializeDapr() throws IOException, InterruptedException, TimeoutException, ExecutionException {

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
        future.get(2, TimeUnit.MINUTES);
        Thread.sleep(sleepTime);

    }

    private static final String DAPR_RUN = "dapr run --app-id %s ";
    private static final String DAPR_COMMAND = " -- mvn exec:java -D exec.mainClass=%s -D exec.classpathScope=\"test\"";

    private String buildDaprCommand(){
        StringBuilder stringBuilder= new StringBuilder(String.format(DAPR_RUN,this.appName))
                .append(this.isGrpc ? " --grpc-port ": " --port ")
                .append(this.port)
                .append(String.format(DAPR_COMMAND, this.serviceClass.getCanonicalName()));
        return stringBuilder.toString();
    }

    private void generateAppName(){

        this.appName=UUID.randomUUID().toString();
    }

    public  void destroyDapr() throws IOException {
        rt.exec("dapr stop --app-id " + this.appName);
        proc.destroy();
    }
}
