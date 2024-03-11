class ServerEntry:
    #Address must have the form host:port
    def __init__(self, address, qualifier):
        self.address = address
        self.qualifier = qualifier