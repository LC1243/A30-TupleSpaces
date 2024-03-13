package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse;

import java.util.List;


public class ClientObserver<R> implements StreamObserver<R>  {

    ResponseCollector collector;

    public ClientObserver (ResponseCollector c) {
        collector = c;
    }

    @Override
    public void onNext(R response) {
        // Checks the response type
        if (response instanceof PutResponse) {
            PutResponse putResponse = (PutResponse) response;
            collector.addString("OK");

        } else if (response instanceof ReadResponse) {
            ReadResponse readResponse = (ReadResponse) response;
            collector.addString(readResponse.getResult());

        } else if (response instanceof TakePhase1Response) {
            // Handle TakePhase1Response
            TakePhase1Response takePhase1Response = (TakePhase1Response) response;

            List<String> matchingTuples = takePhase1Response.getReservedTuplesList();

            // each list is delimited by a "|" in the beginning and in the end "|"
            // Example: [a,b,c] -> [|,a,b,c,|]
            collector.addAllStrings(matchingTuples);

        } else if (response instanceof TakePhase1ReleaseResponse) {
            TakePhase1ReleaseResponse takePhase1ReleaseResponse = (TakePhase1ReleaseResponse) response;
            collector.addString("OK");

        } else if (response instanceof  TakePhase2Response) {
            TakePhase2Response takePhase2Response = (TakePhase2Response) response;
            collector.addString("OK");

        } else if (response instanceof getTupleSpacesStateResponse) {
            // Handle getTupleSpacesStateResponse
            getTupleSpacesStateResponse getTupleSpacesStateResponse = (getTupleSpacesStateResponse) response;

            List<String> tuples = getTupleSpacesStateResponse.getTupleList();

            collector.addAllStrings(tuples);

       } //else {
            // Handle other types of responses
  //      }*/
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {}

}
