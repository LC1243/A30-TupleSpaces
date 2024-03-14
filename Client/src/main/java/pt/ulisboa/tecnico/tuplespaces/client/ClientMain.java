package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

    static final int numServers = 3;
    public static void main(String[] args) {
        boolean debugMode = false;
        int clientId;

        // check arguments
        if (args.length > 1 && "-debug".equals(args[1])) {
            debugMode = true;
        } else if (args.length > 1) {
            System.err.println("Client only needs an Id! The other given arguments will be ignored");
        } else if (args.length < 1) {
            System.err.println("Client needs an unique Id to run!");
        }

        clientId = Integer.parseInt(args[0]);

        //we don't allow the clientId to be zero, since the server initializes the ids associated to tuples as zero
        if (clientId == 0) {
            System.err.println("\nClientId cannot be zero! Please run the program again, with another id!\n");
            return;
        }

        CommandProcessor parser = new CommandProcessor(new ClientService(debugMode, ClientMain.numServers, clientId));
        parser.parseInput();

    }
}
