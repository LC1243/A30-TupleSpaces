package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import com.google.protobuf.LazyStringArrayList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import io.grpc.StatusRuntimeException;
public class ClientService {

    /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service.
     */
    private String target;
    public ClientService(String host, String port) {
        target = host + ":" + Integer.parseInt(port);
    }
    public void sendPutRequest(String tuple) {

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);

        TupleSpacesCentralized.PutRequest request = TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build();
        try {
            TupleSpacesCentralized.PutResponse response = stub.put(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();
    }

    public com.google.protobuf.ProtocolStringList sendGetTupleSpacesStateRequest() {

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);

        TupleSpacesCentralized.getTupleSpacesStateRequest request = TupleSpacesCentralized.getTupleSpacesStateRequest.newBuilder().build();

        // Armazena a lista de tuplos
        com.google.protobuf.ProtocolStringList tuples = new LazyStringArrayList();

        try {
            TupleSpacesCentralized.getTupleSpacesStateResponse response = stub.getTupleSpacesState(request);

            // Recebe a lista de tuplos
            tuples = response.getTupleList();

            System.out.println("OK");

        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();

        return tuples;
    }

    public void sendReadRequest(String tuple) {

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);

        TupleSpacesCentralized.ReadRequest request = TupleSpacesCentralized.ReadRequest.newBuilder().setSearchPattern(tuple).build();
        try {
            TupleSpacesCentralized.ReadResponse response = stub.read(request);
            System.out.println("OK");
            System.out.printf("%s%n", response.getResult());
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();
    }

    public void sendTakeRequest(String tuple) {

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        TupleSpacesGrpc.TupleSpacesBlockingStub stub = TupleSpacesGrpc.newBlockingStub(channel);

        TupleSpacesCentralized.TakeRequest request = TupleSpacesCentralized.TakeRequest.newBuilder().setSearchPattern(tuple).build();

        try {
            TupleSpacesCentralized.TakeResponse response = stub.take(request);
            System.out.println("OK");
            System.out.println(response.getResult());
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
        }
        // A Channel should be shutdown before stopping the process.
        channel.shutdownNow();
    }

}
