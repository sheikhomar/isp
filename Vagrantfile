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
  config.vm.provision "shell", inline: "sudo sed -i.old s#us\.archive\.ubuntu\.com#ftp.snt.utwente.nl/pub/os/linux#g /etc/apt/sources.list"
  config.vm.provision "shell", inline: "sudo apt-add-repository -y ppa:ansible/ansible"
  config.vm.provision "shell", inline: "sudo apt-get update"
 
  config.vm.provision "shell", inline: "sudo apt-get install -y python-pip python-dev ansible" 
  config.vm.provision :shell, inline: <<-SCRIPT
  GALAXY=/usr/local/bin/ansible-galaxy
  echo '#!/usr/bin/env bash
  /usr/bin/ansible-galaxy "$@"
  exit 0
  ' | sudo tee $GALAXY
  sudo chmod 0755 $GALAXY
  SCRIPT

  # Run ansible provisioning script
  config.vm.provision "ansible_local" do |ansible|
    ansible.install = false
    ansible.playbook = "provisioning.yml"
  end
end
