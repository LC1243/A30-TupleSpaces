package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import com.google.protobuf.LazyStringArrayList;
import com.google.protobuf.ProtocolStringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.ClientObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.*;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.tuplespaces.client.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

import java.util.*;

import static io.grpc.Status.INVALID_ARGUMENT;


public class ClientService {

    private ArrayList<String> targets = new ArrayList<String>();
    private ArrayList<String> qualifiers = new ArrayList<String>();

    private boolean debugMode = false;

    OrderedDelayer delayer;

    private final int numServers;

    private ManagedChannel[] channels;
    private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

    public ClientService(boolean debugMode, int numServers) {

        this.numServers = numServers;
        delayer = new OrderedDelayer(numServers);
        this.debugMode = debugMode;
        // Get the servers addresses
        this.lookupServer();

        channels = new ManagedChannel[numServers];
        stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];

        // Creates a channel and stub for every server
        for (int i = 0; i < numServers; i++) {
            channels[i] = ManagedChannelBuilder.forTarget(targets.get(i)).usePlaintext().build();
            stubs[i] = TupleSpacesReplicaGrpc.newStub(channels[i]);
        }


    }

    /* This method allows the command processor to set the request delay assigned to a given server */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);
    }

    public void closeChannels() {
        if(debugMode)
            System.err.println("DEBUG: Shutting down client\n");

        for (ManagedChannel ch : channels)
            ch.shutdown();
    }


    // Lookup for Servers that can satisfy the service TupleSpace
    public void lookupServer() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        try {
            NameServerServiceGrpc.NameServerServiceBlockingStub stub = NameServerServiceGrpc.newBlockingStub(channel);
            NameServer.LookupRequest request = NameServer.LookupRequest.newBuilder().setService("TupleSpace").build();

            // Get the ip and port where the server is running
            NameServer.LookupResponse response = stub.lookup(request);

            com.google.protobuf.ProtocolStringList servers = new LazyStringArrayList();
            servers = response.getServerList();

            // Found servers
            if (!servers.isEmpty()) {
                for(int i = 0; i < response.getServerCount() - 1; i += 2) {
                    String target = response.getServer(i);
                    targets.add(target);
                    qualifiers.add(response.getServer(i+1));
                }

            } else {
                System.err.println("There aren't servers available. Please try run again");
                return;
            }
        } catch (StatusRuntimeException e) {
            System.err.println("Error communicating with the NameServer during lookup: " + e.getStatus().getDescription());
        } finally {
            channel.shutdownNow();
        }
    }


    public void sendPutRequest(String tuple) {

        ResponseCollector c = new ResponseCollector();

        if(debugMode){
            System.err.println("DEBUG: Put Request initialized correctly\n");
        }

        ArrayList<TupleSpacesReplicaTotalOrder.PutRequest> requests = new ArrayList<TupleSpacesReplicaTotalOrder.PutRequest>();

        for(int i = 0; i < numServers; i++) {
            // FIXME: Send sequence Number Also
            //create requests for each server, with the new tuple
            TupleSpacesReplicaTotalOrder.PutRequest request = TupleSpacesReplicaTotalOrder.PutRequest.newBuilder().setNewTuple(tuple).build();
            requests.add(request);
        }

        for(Integer id : delayer) {

            try {
                stubs[id].put(requests.get(id), new ClientObserver<TupleSpacesReplicaTotalOrder.PutResponse>(c));

            // Exception caught
            } catch (StatusRuntimeException e) {
                System.err.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }


        }

        //wait all responses
        try {
            c.waitUntilAllReceived(numServers);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("OK\n");

        if (debugMode) {
            System.err.println("DEBUG: PutRequest finished correctly\n");
        }

    }

    public void sendReadRequest(String tuple) {

        ResponseCollector c = new ResponseCollector();

        if(debugMode){
            System.err.println("DEBUG: Read Request initialized correctly\n");
        }

        ArrayList<TupleSpacesReplicaTotalOrder.ReadRequest> requests = new ArrayList<TupleSpacesReplicaTotalOrder.ReadRequest>();


        for(int i = 0; i < numServers; i++) {
            //create requests for each server, with the new tuple
            TupleSpacesReplicaTotalOrder.ReadRequest request = TupleSpacesReplicaTotalOrder.ReadRequest.newBuilder().setSearchPattern(tuple).build();
            requests.add(request);

        }


        for(Integer id : delayer) {
            try {
                // Send the request to the server with the id
                stubs[id].read(requests.get(id), new ClientObserver<TupleSpacesReplicaTotalOrder.ReadResponse>(c));

            } catch (StatusRuntimeException e) {
                // Handle status runtime exception
                System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            }

        }

        //wait for the first response
        try {
            c.waitUntilAllReceived(1);
            System.out.println("OK");
            System.out.println(c.getFirstCollectedResponse() + "\n");
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (debugMode) {
            System.err.println("DEBUG: Read Request finished correctly\n");
        }
    }

    public void sendTakeRequest(String tuple) {
        //TODO: Implement me with sequence Number
    }


    public List<String> sendGetTupleSpacesStateRequest(String qualifier) {

        ResponseCollector c = new ResponseCollector();

        if (targets.isEmpty() || qualifiers.isEmpty()) {
            return new ArrayList<>(); // Return an empty list or handle accordingly
        }

        int index = this.qualifiers.indexOf(qualifier);

        if(debugMode){
            System.err.println("DEBUG: GetTupleSpaceStateRequest initialized correctly\n");
        }

        TupleSpacesReplicaTotalOrder.getTupleSpacesStateRequest request = TupleSpacesReplicaTotalOrder.getTupleSpacesStateRequest.newBuilder().build();

        try {

            stubs[index].getTupleSpacesState(request,  new ClientObserver<TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse>(c));
            c.waitUntilAllListsAreReceived(1);

            System.out.println("OK");
            if (debugMode){
                System.err.println("DEBUG: GetTupleSpacesStateRequest finished correctly\n");
            }
            // Exception caught
        } catch (StatusRuntimeException | InterruptedException e) {
            System.err.println("Caught exception with description: " +
                    ((e instanceof StatusRuntimeException) ? ((StatusRuntimeException) e).getStatus().getDescription() : e.getMessage()));
            }

        // Returns the list
        return c.getLastListAndRemove();
    }

}
