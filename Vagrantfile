# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  config.vm.box = "precise64"
  config.vm.box_url = "http://files.vagrantup.com/precise64.box"

  config.vm.define "redis" do |redis|
    redis.vm.box = "precise64"
    redis.vm.network "private_network", ip: "10.111.1.100"
    redis.vm.provision "shell", path: "scripts/setup-redis.sh"
  end

  config.vm.define "app0" do |app|
    app.vm.box = "precise64"
    app.vm.network "private_network", ip: "10.111.1.101"
    app.vm.provision "shell", path: "scripts/setup-appserver.sh"
  end

  config.vm.define "app1" do |app|
    app.vm.box = "precise64"
    app.vm.network "private_network", ip: "10.111.1.102"
    app.vm.provision "shell", path: "scripts/setup-appserver.sh"
  end

  config.vm.define "lb" do |lb|
    lb.vm.box = "precise64"
    lb.vm.network "private_network", ip: "10.111.1.103"
    lb.vm.provision "shell", path: "scripts/setup-nginx.sh"
  end
end
