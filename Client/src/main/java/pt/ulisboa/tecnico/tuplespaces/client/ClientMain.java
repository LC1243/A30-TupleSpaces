package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {
    public static void main(String[] args) {
        boolean debugMode = false ;
        //System.out.println(ClientMain.class.getSimpleName());

        // receive and print arguments
        //System.out.printf("Received %d arguments%n", args.length);
        //for (int i = 0; i < args.length; i++) {
        //    System.out.printf("arg[%d] = %s%n", i, args[i]);
        //}

        // check arguments
        if (args.length > 0 && "-debug".equals(args[0])) {
            debugMode = true;
        }

        else if (args.length > 0){
            System.err.println("No arguments needed to run Client! The given arguments will be ignored");
            //System.err.println("Usage: mvn exec:java -Dexec.args=<host> <port>");
        }


        CommandProcessor parser = new CommandProcessor(new ClientService(debugMode));
        parser.parseInput();

    }
}
