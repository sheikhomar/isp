Vagrant.configure(2) do |config|
  config.vm.box = "box-cutter/ubuntu1404-desktop"
  config.vm.provider "virtualbox" do |vb|
    # Display the VirtualBox GUI when booting the machine
     vb.gui = true
  
     # Customize the amount of memory on the VM:
     vb.memory = "1024"
  end
  
  # Prepare for ansible 
  config.vm.provision "shell", inline: "sudo apt-get install -y python-pip python-dev && sudo pip install ansible==1.9.2 && sudo cp /usr/local/bin/ansible /usr/bin/ansible"
  
  # Run ansible provisioning script
  config.vm.provision "ansible_local" do |ansible|
    ansible.playbook = "provisioning.yml"
  end
end
