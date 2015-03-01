apt-get install -y redis-server

mkdir -p /opt/redis/run/
mkdir -p /opt/redis/log/

chown -R redis:redis /opt/redis

rm -rf /etc/redis/redis.conf
ln -s /vagrant/config/redis.conf /etc/redis/redis.conf

sudo service redis-server restart
