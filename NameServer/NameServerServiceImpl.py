import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
import grpc
import re


def isValidAddress(address):
    #separate host:port
    parts = address.split(':')

    # Check if there are exactly two parts
    if len(parts) != 2 or not parts[0] or not parts[1].isdigit():
        return False

    # Check if the port number is within the valid range (0-65535)
    port = int(parts[1])
    if not 0 <= port <= 65535:
        return False

    return True

class ServerEntry:
    #Address must have the form host:port
    def __init__(self, address, qualifier):
        self.address = address
        self.qualifier = qualifier

class ServiceEntry:
    def __init__(self, service):
        self.service = service
        self.servers = []

    def addServer(self, server_entry):
        self.servers.append(server_entry)

class NamingServer:
    def __init__(self):
        self.services = {}

    def registerServer(self, service, qualifier, address):
        # Adds service to the list
        if service not in self.services:
            self.services[service] = ServiceEntry(service)

        self.services[service].addServer(ServerEntry(address, qualifier))

    def deleteServer(self, service, address):

        if service in self.services:
            for server_entry in self.services[service].servers:
                if server_entry.address == address:
                    self.services[service].servers.remove(server_entry)
                    return True

        return False


class NameServerServiceImpl(pb2_grpc.NameServerServiceServicer):
    debugMode = False
    def __init__(self, debugMode, *args, **kwargs):
        self.server = NamingServer()
        self.debugMode = debugMode

    def register(self, request, context):
        if self.debugMode:
            print("[REGISTER REQUEST]:", request.address, request.qualifier, request.service)

        # Checks if address is valid
        if isValidAddress(request.address):
            self.server.registerServer(request.service, request.qualifier, request.address)
            if self.debugMode:
                print("[REGISTER RESPONSE]: SUCCESS")
            return pb2.RegisterResponse()
        else:
            context.abort(grpc.StatusCode.INVALID_ARGUMENT, "Not possible to register the server!")


    def lookup(self, request, context):
        if self.debugMode:
            print("[LOOKUP REQUEST]:", request.service, request.qualifier)
        service = request.service
        qualifier = request.qualifier

        # Gets the lookup response
        response = pb2.LookupResponse()

        if service in self.server.services:
            service_entry = self.server.services[service]

            if qualifier:
                for server_entry in service_entry.servers:

                    if server_entry.qualifier == qualifier:
                        response.server.append(server_entry.address)
            # No qualifier
            else:
                response.server.extend([server_entry.address for server_entry in service_entry.servers])
        if self.debugMode:
            print("[LOOKUP RESPONSE]:", response)

        return response


    def delete(self, request, context):
        if self.debugMode:
            print("[DELETE REQUEST]:", request.service, request.address)

        result = self.server.deleteServer(request.service, request.address)

        if result:
            if self.debugMode:
                print("[DELETE RESPONSE]: SUCCESS")
            return pb2.DeleteResponse()

        context.abort(grpc.StatusCode.INVALID_ARGUMENT, "Not possible to remove the server")


