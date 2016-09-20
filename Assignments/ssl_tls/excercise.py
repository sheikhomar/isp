#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.term import makeTerms

class TLSTopo(Topo):
    def build(self):
        #Single switch
        switch = self.addSwitch('s1')
   	
        #Add nodes
        student = self.addHost('A')
        self.addLink(switch, student)

        for i in range(1,10):
            server = self.addHost("Server-{}".format(i))
            self.addLink(switch, server)

def run_exercise():
    #Create and start a new network with our custom topology
    topo = TLSTopo()
    net = Mininet(topo=topo)
    net.start()

    #Verify connectivity
    net.pingAll()

    #Start BIND DNS-server
    #net["B"].popen('named', '-g', '-c', '/home/vagrant/assignments/DNS/config/named.conf')

    #Open terminals
    makeTerms([net["A"]], title="Student terminal")
    #makeTerms([net["D"]], title="Capture terminal")

if __name__ == '__main__':
    run_exercise()
