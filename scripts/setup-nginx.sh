apt-get update
apt-get install -y nginx

rm -rf /etc/nginx/sites-enabled/*
ln -s /vagrant/config/loadbalance.conf /etc/nginx/sites-enabled/balance.conf

sudo service nginx restart
