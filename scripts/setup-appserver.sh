apt-get update
apt-get install -y openjdk-7-jre

cp /vagrant/config/appserver.conf /etc/init/appserver.conf

sudo service appserver restart
