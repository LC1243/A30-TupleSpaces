package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

    static final int numServers = 3;
    public static void main(String[] args) {
        boolean debugMode = false ;


        // check arguments
        if (args.length > 0 && "-debug".equals(args[0])) {
            debugMode = true;
        }

        else if (args.length > 0){
            System.err.println("No arguments needed to run Client! The given arguments will be ignored");
        }

        CommandProcessor parser = new CommandProcessor(new ClientService(debugMode));
        //CommandProcessor parser = new CommandProcessor(new ClientService(ClientMain.numServers));
        parser.parseInput();

    }
}
