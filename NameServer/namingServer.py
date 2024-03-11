from serviceEntry import ServiceEntry
from serverEntry import ServerEntry

class NamingServer:
    def __init__(self):
        self.services = {}

    def registerServer(self, service, qualifier, address):
        # Adds service to the list if not already in the Name Server
        if service not in self.services:
            self.services[service] = ServiceEntry(service)

        #Associate server entry with respective service
        self.services[service].addServer(ServerEntry(address, qualifier))


    def serverAlreadyExists(self, service, qualifier, address):
        #server doesn't exist
        if service not in self.services:
            return False

        #service exists
        service_entry = self.services[service]

        for server_entry in service_entry.servers:
            if server_entry.address == address:
                return True

        return False

    def deleteServer(self, service, address):

        if service in self.services:
            for server_entry in self.services[service].servers:
                # if the service and address given correspond, remove the server
                if server_entry.address == address:
                    self.services[service].servers.remove(server_entry)
                    return True

        return False