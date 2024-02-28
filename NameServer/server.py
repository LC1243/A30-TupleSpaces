import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NameServerServiceImpl import NameServerServiceImpl
from concurrent import futures

# define the port
PORT = 5001

if __name__ == '__main__':
    debugMode = False
    try:

        """
        # print received arguments
        print("Received arguments:")
        for i in range(1, len(sys.argv)):
            print("  " + sys.argv[i])
        """
        for i in range(1, len(sys.argv)):
            if (sys.argv[i] == "-debug"):
                debugMode = True

        # Check if arguments are provided and raise an exception
        if len(sys.argv) > 1:
            print("No arguments needed to run the Name Server! The given arguments will be ignored")

        #create Name Server
        nameServer = grpc.server(futures.ThreadPoolExecutor(max_workers = 1))
        #add Service
        pb2_grpc.add_NameServerServiceServicer_to_server(NameServerServiceImpl(debugMode), nameServer)
        # listen on port
        nameServer.add_insecure_port('[::]:' + str(PORT))
        # start server
        nameServer.start()
        # print message
        print("Name Server listening on port " + str(PORT))
        # print termination message
        print("Press CTRL+C to terminate")
        # wait for server to finish
        nameServer.wait_for_termination()

    except KeyboardInterrupt:
        print("NameServer stopped\n")
        exit(0)
