Vagrant.configure(2) do |config|
  config.vm.box = "box-cutter/ubuntu1404-desktop"
  config.vm.provider "virtualbox" do |vb|
    # Display the VirtualBox GUI when booting the machine
     vb.gui = true
  
     # Customize the amount of memory on the VM:
     vb.memory = "1024"
  end

  # Shared folder
  config.vm.synced_folder "Assignments", "/home/vagrant/assignments"

  # Prepare for ansible 
  config.vm.provision "shell", inline: "sudo apt-get install -y python-pip python-dev python-cffi"
  
  # Run ansible provisioning script
  config.vm.provision "ansible_local" do |ansible|
  	ansible.install = true
    ansible.playbook = "provisioning.yml"
  end
end
