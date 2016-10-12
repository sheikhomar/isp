#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.term import makeTerms
from mininet.node import Node
import random
import re
from shutil import copyfile

import signal 
import sys
import threading
import os

def signal_handler(signal, frame):
    print("got CTRL+C")

signal.signal(signal.SIGINT, signal_handler)

class ExerciseState():
    def __init__(self, net, pids):
        self.pids= pids;
        self.net = net;

class SSHTopo(Topo):
    def build(self):
        #Single switch
        switch = self.addSwitch('s1')
   	
        you     = self.addHost('you', ip='10.0.5.1/24')
        ssh1    = self.addHost('ssh1', ip='10.0.5.11/24')
        ssh2    = self.addHost('ssh2', ip='10.0.5.12/24')
        ssh3    = self.addHost('ssh3', ip='10.0.5.13/24')
        httpd   = self.addHost('httpd', ip='10.0.5.80/24')
        for i in range(1,20):
            proxy = self.addHost('proxy%i'%i, ip='10.0.5.%i/24'%(20+i))
            self.addLink(proxy,switch)

        #Add links between each of the hosts and the switch
        self.addLink(you,switch)
        self.addLink(ssh1,switch)
        self.addLink(ssh2,switch)
        self.addLink(ssh3,switch)
        self.addLink(httpd,switch)
	


def run_exercise():
    #Create and start a new network with our custom topology
    topo = SSHTopo()
    net = Mininet(topo=topo)
    net.start()
    pids = []

    net['httpd'].cmd('ip addr add fc00::5:80/64 dev httpd-eth0')
    net['httpd'].cmd('ip addr add fc00::5:88/64 dev httpd-eth0')
    net['httpd'].cmd('ip addr add fc00::5:8080/64 dev httpd-eth0')
    net['httpd'].cmd('ip addr add fc00::5:8888/64 dev httpd-eth0')
    net['httpd'].cmd('ip addr add fc00::5:8889/64 dev httpd-eth0')
    for i in range(1,20):
        net['proxy%i'%i].cmd('ip addr add fc00::5:%x/64 dev proxy%i-eth0'%(i*111, i))
        pids.append(net["proxy%i"%i].popen('/usr/sbin/sshd', '-o', 'PermitTTY=no', '-o', 'ForceCommand=/usr/sbin/nologin').pid)

    httpd_pid = net["httpd"].popen('nginx', '-c', '/home/vagrant/assignments/ssh/nginx.conf').pid
    ssh1_sshd_pid = net["ssh1"].popen('/usr/sbin/sshd', '-o', 'Ciphers=3des-cbc', '-o', 'PrintMotd=yes').pid #, '-f', '/home/vagrant/assignments/ssh/ssh1_sshd.conf').pid
    ssh3_sshd_pid = net["ssh3"].popen('/usr/sbin/sshd', '-o', 'PrintMotd=no').pid

    pids += [httpd_pid, ssh1_sshd_pid, ssh3_sshd_pid]

    makeTerms([net["you"]], title="term1")
    makeTerms([net["you"]], title="term2")

    state = ExerciseState(net, pids)
    return state
     
def cleanup(state):
    for pid in state.pids:
        print "going to kill ", pid
        try:
            os.kill(pid, signal.SIGTERM)
        except Exception as e:
            print("Oops: ", e)
    os.system('sudo killall nginx')
    os.system('sudo mn -c')

if __name__ == '__main__':
    state = run_exercise()

    print("Exercise started, press CTRL+C to stop and clean up")
    signal.pause()
    cleanup(state);
