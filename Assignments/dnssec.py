#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.term import makeTerms

class DDoSTopo(Topo):
    def build(self):
        #Single switch
        switch = self.addSwitch('s1')
   	
	#Add attacker, dns resolver and victim host nodes
        attacker = self.addHost('A')
        dnsresolver = self.addHost('B')
        victim = self.addHost('C')

        #Add links between each of the hosts and the switch
        self.addLink(attacker,switch)
        self.addLink(dnsresolver,switch)
        self.addLink(victim,switch)
	

def run_exercise():
    #Create and start a new network with our custom topology
    topo = DDoSTopo()
    net = Mininet(topo=topo)
    net.start()

    #Configure switch so that packets reach the right port (to prevent l2 learning from affecting the exercise)
    net["s1"].dpctl("del-flows")
    net["s1"].dpctl("add-flow", "dl_type=0x0800,nw_dst=10.0.0.1,actions=output:1")
    net["s1"].dpctl("add-flow", "dl_type=0x0800,nw_dst=10.0.0.2,actions=output:2")
    net["s1"].dpctl("add-flow", "dl_type=0x0800,nw_dst=10.0.0.3,actions=output:3")

    #Verify connectivity
    net.pingAll()

    #Start BIND DNS-server
    net["B"].popen('named', '-g')

 #   makeTerms([net["B"]], title="DNS")	

    #Open terminals
    makeTerms([net["A"]], title="Attacker terminal")
#    makeTerms([net["C"]], title="Victim terminal")

if __name__ == '__main__':
    run_exercise()
