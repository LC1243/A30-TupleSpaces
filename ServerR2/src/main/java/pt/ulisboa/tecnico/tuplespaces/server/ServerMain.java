package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.*;

import java.io.IOException;
import static io.grpc.Status.INVALID_ARGUMENT;

import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;

import java.util.concurrent.CountDownLatch;

public class ServerMain {
    private static int port;

    public static void main(String[] args) throws IOException, InterruptedException{
        boolean debugMode = false;

        // Check arguments.
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: mvn exec:java -Dexec.args=<port> <qualifier>");
            return;
        }

        port = Integer.valueOf(args[0]);

        // Check if debug mode is enabled
        if (args.length == 3 && "-debug".equals(args[2])) {
            debugMode = true;
        }
        try {
            //register the server in the Name Server
            final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();

            NameServerServiceGrpc.NameServerServiceBlockingStub stub = NameServerServiceGrpc.newBlockingStub(channel);
            NameServer.RegisterRequest request = NameServer.RegisterRequest.newBuilder().setService("TupleSpace").
                    setQualifier(args[1]).setAddress("localhost:" + args[0]).build();

            try {
                // Send a register request with service: TupleSpace, address: localhost:args[0] and qualifier: args[1]
                NameServer.RegisterResponse response = stub.register(request);

                // Exception caught
            } catch (StatusRuntimeException e) {
                System.err.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }

            // A Channel should be shutdown before stopping the process.
            channel.shutdownNow();
        } catch (Exception e) {
            System.err.println("Error during server registration: " + e.getMessage());
            return;
        }
        final BindableService impl = new TupleSpacesReplicaImplBase(debugMode, args[1]);

        try {

            // Create a new server to listen on port.
            Server server = ServerBuilder.forPort(port).addService(impl).build();
            // Start the server.
            server.start();
            // Server threads are running in the background.
            System.out.println("Server started");
            System.out.println("To Stop the Server, please press CTRL+C");

            /*
             * HANDLE CTRL+C -> when it's pressed, first we unregister the server from the Name Server,
             * and then we shutdown the server
             * */

            // Create a latch to wait for termination signal
            CountDownLatch shutdownLatch = new CountDownLatch(1);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

                // Shutdown the server when CTRL+C is pressed

                try {
                    // Shutdown the server when CTRL+C is pressed
                    final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();

                    NameServerServiceGrpc.NameServerServiceBlockingStub stub1 = NameServerServiceGrpc.newBlockingStub(channel);
                    NameServer.DeleteRequest request2 = NameServer.DeleteRequest.newBuilder().setService("TupleSpace").
                            setAddress("localhost:" + args[0]).build();

                    try {
                        //send delete request to the Name Server, and handle response
                        NameServer.DeleteResponse response = stub1.delete(request2);

                        // Exception caught
                    } catch (StatusRuntimeException e) {
                        System.err.println("Caught exception with description: " +
                                e.getStatus().getDescription());
                    }

                    // A Channel should be shutdown before stopping the process.
                    channel.shutdownNow();
                    server.shutdown();
                    System.out.println("Server terminated.");
                    shutdownLatch.countDown();
                } catch (Exception e) {
                    System.err.println("Error during server shutdown: " + e.getMessage());
                }

            }));

            // Wait for the termination signal
            shutdownLatch.await();

            // Do not exit the main thread. Wait until server is terminated.
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error during server startup: " + e.getMessage());
        }
    }
}

