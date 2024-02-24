package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import static io.grpc.Status.INVALID_ARGUMENT;

public class TupleSpacesImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private ServerState server = new ServerState();

    @Override
    public void put(TupleSpacesCentralized.PutRequest request, StreamObserver<TupleSpacesCentralized.PutResponse> responseObserver) {

        //get tuple sent by client
        String newTuple = request.getNewTuple();
        //Validate the new Tuple, invoking a Server's State method
        boolean isValid = server.tuppleIsValid(newTuple);

        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());
        } else {
            //Add new tuple to Server
            server.put(newTuple);
            TupleSpacesCentralized.PutResponse response = TupleSpacesCentralized.PutResponse.newBuilder().build();
            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();
            System.out.println(server.ListToString());
            System.out.println("PUT COMMAND EXECUTED WITH SUCCESS");
        }

    }

}
