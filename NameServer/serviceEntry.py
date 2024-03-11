from serverEntry import ServerEntry

class ServiceEntry:
    def __init__(self, service):
        self.service = service
        self.servers = []

    def addServer(self, server_entry):
        self.servers.append(server_entry)