package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import com.google.protobuf.LazyStringArrayList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import io.grpc.StatusRuntimeException;
public class ClientService {

    /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service.
     */
    private String target;
    private boolean debugMode = false;

    public ClientService(boolean debugMode) {
        this.debugMode = debugMode;
        this.registerName();
    }

    public void registerName() {
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
        this.registerName();

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
        this.registerName();

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
        this.registerName();

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
        this.registerName();

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
