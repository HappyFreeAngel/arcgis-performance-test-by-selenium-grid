#!/bin/bash
#确保shell 切换到当前shell 脚本文件夹
current_file_path=$(cd "$(dirname "$0")"; pwd)
cd ${current_file_path}

sudo firewall-cmd --add-port=8777/tcp --zone=public --permanent
sudo firewall-cmd --add-port=443/tcp --zone=public --permanent
sudo firewall-cmd --reload
sudo firewall-cmd --list-all

docker stop testnginx
docker rm   testnginx

docker run \
--restart=always \
--name=testnginx \
-p 8777:80 \
-p 443:443 \
-v `pwd`/www:/usr/share/nginx/html  \
-d nginx:1.13-alpine

echo "打开http://localhost:8777/index.html 可以浏览"
