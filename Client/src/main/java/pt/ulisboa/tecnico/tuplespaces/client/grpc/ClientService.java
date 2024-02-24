package pt.ulisboa.tecnico.tuplespaces.client.grpc;

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

}
