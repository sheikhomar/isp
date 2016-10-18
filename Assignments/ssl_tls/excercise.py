#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.term import makeTerms
from mininet.clean import Cleanup

class TLSTopo(Topo):
    def build(self):
        #Single switch
        switch = self.addSwitch('s1')
   	
        #Add nodes
        student = self.addHost('A')
        self.addLink(switch, student)

        for i in range(1,6):
            server = self.addHost("Server-{}".format(i))
            self.addLink(switch, server)

def run_exercise():
    #Create and start a new network with our custom topology
    topo = TLSTopo()
    net = Mininet(topo=topo)
    net.start()
    net.pingAll()

    processes = []

    #Start Nginx HTTP-server
    processes.append(net["Server-1"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_1.conf'))
    processes.append(net["Server-2"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_2.conf'))
    processes.append(net["Server-3"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_3.conf'))
    processes.append(net["Server-4"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_4.conf'))
    processes.append(net["Server-5"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_5.conf'))

    #Open wireshark
    processes.append(net["A"].popen('wireshark'))

    #Open terminals
    processes.append(makeTerms([net["A"]], title="Student terminal")[0])

    raw_input("Press Enter to exit....")
    for process in processes:
        process.kill()
    Cleanup.cleanup()

if __name__ == '__main__':
    run_exercise()
