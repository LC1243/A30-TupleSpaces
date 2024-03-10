package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import com.google.protobuf.LazyStringArrayList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

public class ClientService {

    /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service.
     */

    OrderedDelayer delayer;

    public ClientService(int numServers) {

        /* TODO: create channel/stub for each server */

        /* The delayer can be used to inject delays to the sending of requests to the
            different servers, according to the per-server delays that have been set  */
        delayer = new OrderedDelayer(numServers);
    }

    /* This method allows the command processor to set the request delay assigned to a given server */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);

        /* TODO: Remove this debug snippet */
        System.out.println("[Debug only]: After setting the delay, I'll test it");
        for (Integer i : delayer) {
            System.out.println("[Debug only]: Now I can send request to stub[" + i + "]");
        }
        System.out.println("[Debug only]: Done.");
    }

    /* TODO: individual methods for each remote operation of the TupleSpaces service */

    /* Example: How to use the delayer before sending requests to each server
     *          Before entering each iteration of this loop, the delayer has already
     *          slept for the delay associated with server indexed by 'id'.
     *          id is in the range 0..(numServers-1).

        for (Integer id : delayer) {
            //stub[id].some_remote_method(some_arguments);
        }

    */

    private String target;
    private boolean debugMode = false;

    public ClientService(boolean debugMode) {
        this.debugMode = debugMode;
        this.lookupServer();
    }

    // Lookup for a Server that can satisfy the service TupleSpace, by default with qualifier A
    public void lookupServer() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();

        NameServerServiceGrpc.NameServerServiceBlockingStub stub = NameServerServiceGrpc.newBlockingStub(channel);
        NameServer.LookupRequest request = NameServer.LookupRequest.newBuilder().setService("TupleSpace").setQualifier("A").build();

        // Get the ip and port where the server is running
        NameServer.LookupResponse response = stub.lookup(request);

        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();

        com.google.protobuf.ProtocolStringList servers = new LazyStringArrayList();
        servers = response.getServerList();

        if(!servers.isEmpty()) {
            target = response.getServer(0);
        } else {
            System.err.println("There aren't servers available. Please try run again");
            return;
        }
    }

    public void sendPutRequest(String tuple) {
        this.lookupServer();

        if(debugMode){
            System.err.println("DEBUG: Put Request initialized correctly\n");
        }

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);

        //Send a Put Request with tuple
        TupleSpacesCentralized.PutRequest request = TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build();

        try {
            TupleSpacesCentralized.PutResponse response = stub.put(request);
            System.out.println("OK\n");
            if (debugMode){
                System.err.println("DEBUG: PutRequest finished correctly\n");
            }
        // Exception caught
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();
    }

    public void sendReadRequest(String tuple) {
        this.lookupServer();

        if(debugMode){
            System.err.println("DEBUG: ReadRequest initialized correctly\n");
        }
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);
        //Creates a read request
        TupleSpacesCentralized.ReadRequest request = TupleSpacesCentralized.ReadRequest.newBuilder().setSearchPattern(tuple).build();
        try {
            //Answers the read request and displays the result
            TupleSpacesCentralized.ReadResponse response = stub.read(request);
            System.out.println("OK");
            System.out.printf("%s%n\n", response.getResult());

            if(debugMode){
                System.err.println("DEBUG: ReadRequest finished correctly\n");
            }
        // Exception caught
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();
    }

    public void sendTakeRequest(String tuple) {
        this.lookupServer();

        if(debugMode){
            System.err.println("DEBUG: TakeRequest initialized correctly\n");
        }

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);

        // Sends a take request for a given tuple
        TupleSpacesCentralized.TakeRequest request = TupleSpacesCentralized.TakeRequest.newBuilder().setSearchPattern(tuple).build();

        try {
            TupleSpacesCentralized.TakeResponse response = stub.take(request);
            System.out.println("OK");
            System.out.println(response.getResult() + "\n");
            if(debugMode){
                System.err.println("DEBUG: TakeRequest finished correctly\n");
            }
        // Exception caught
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();
    }

    public com.google.protobuf.ProtocolStringList sendGetTupleSpacesStateRequest() {
        this.lookupServer();

        if(debugMode){
            System.err.println("DEBUG: GetTupleSpaceStateRequest initialized correctly\n");
        }
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);

        TupleSpacesCentralized.getTupleSpacesStateRequest request = TupleSpacesCentralized.getTupleSpacesStateRequest.newBuilder().build();

        // Stores the tuples list
        com.google.protobuf.ProtocolStringList tuples = new LazyStringArrayList();

        try {
            TupleSpacesCentralized.getTupleSpacesStateResponse response = stub.getTupleSpacesState(request);

            // Receives the list
            tuples = response.getTupleList();

            System.out.println("OK");
            if (debugMode){
                System.err.println("DEBUG: GetTupleSpacesStateRequest finished correctly\n");
            }
            // Exception caught
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();

        // Returns the list
        return tuples;
    }
}
