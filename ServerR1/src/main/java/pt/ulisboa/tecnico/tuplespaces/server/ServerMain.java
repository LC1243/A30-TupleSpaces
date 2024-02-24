package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerMain {
    // added for first delivery ig
    private static int port;

    public static void main(String[] args) throws IOException, InterruptedException{
        //teste
        System.out.println(ServerMain.class.getSimpleName());

        // Print received arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++){
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // Check arguments.
        if (args.length < 4) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port%n", Server.class.getName());
            return;
        }

        port = Integer.valueOf(args[1]);

        final BindableService impl = new TupleSpacesImpl();

        // Create a new server to listen on port.
        //Server server = ServerBuilder.forPort(port).addService(impl).build();
        Server server = ServerBuilder.forPort(port).addService(impl).build();
        // Start the server.
        server.start();
        // Server threads are running in the background.
        System.out.println("Server started");

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();

    }
}

