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

    @Override
    public void getTupleSpacesState(TupleSpacesCentralized.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesCentralized.getTupleSpacesStateResponse> responseObserver) {

        //Get list from server
        java.util.List<java.lang.String> tuples = server.getTupleSpacesState();

        TupleSpacesCentralized.getTupleSpacesStateResponse response = TupleSpacesCentralized.getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();

        System.out.println("GET_TUPLE_SPACES_STATE COMMAND EXECUTED WITH SUCCESS");

    }

    @Override
    public void read(TupleSpacesCentralized.ReadRequest request, StreamObserver<TupleSpacesCentralized.ReadResponse> responseObserver){

        String pattern = request.getSearchPattern();

        String tuple = server.read(pattern);

        TupleSpacesCentralized.ReadResponse response = TupleSpacesCentralized.ReadResponse.newBuilder().setResult(tuple).build();

        responseObserver.onNext(response);

        responseObserver.onCompleted();

        System.out.println("READ COMMAND EXECUTED WITH SUCCESS");
    }

    @Override
    public void take(TupleSpacesCentralized.TakeRequest request, StreamObserver<TupleSpacesCentralized.TakeResponse> responseObserver) {

        String searchPattern = request.getSearchPattern();

        boolean isValid = server.tuppleIsValid(searchPattern);

        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());

        } else {

            TupleSpacesCentralized.TakeResponse response = TupleSpacesCentralized.TakeResponse.newBuilder().setResult(server.take(searchPattern)).build();
            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();
            System.out.println(server.ListToString());
            System.out.println("TAKE COMMAND EXECUTED WITH SUCCESS");
        }

    }

}
