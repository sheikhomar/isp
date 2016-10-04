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

        for i in range(1,6):
            server = self.addHost("Server-{}".format(i))
            self.addLink(switch, server)

def run_exercise():
    #Create and start a new network with our custom topology
    topo = TLSTopo()
    net = Mininet(topo=topo)
    net.start()
    net.pingAll()

    #Start Nginx HTTP-server
    net["Server-1"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_1.conf')
    net["Server-2"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_2.conf')
    net["Server-3"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_3.conf')
    net["Server-4"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_4.conf')
    net["Server-5"].popen('nginx -p /home/vagrant/assignments/ssl_tls/nginx -c nginx_5.conf')

    #Open wireshark
    net["A"].popen('wireshark')

    #Open terminals
    makeTerms([net["A"]], title="Student terminal")

if __name__ == '__main__':
    run_exercise()
