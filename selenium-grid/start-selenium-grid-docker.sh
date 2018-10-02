#!/bin/bash
#确保shell 切换到当前shell 脚本文件夹
current_file_path=$(cd "$(dirname "$0")"; pwd)
cd ${current_file_path}

echo "命令格式 ./start-selenium-grid-docker.sh {浏览器docker容器数量}"
echo "命令格式 ./start-selenium-grid-docker.sh {浏览器docker容器数量}"
echo "将运行$1个浏览器docker,按CTRL+C 中断退出"

if [ ! -n "$1" ] ;then
    echo "请输入浏览器docker容器数量"
    exit 1
else
    echo "浏览器docker容器数量 是 $1"
fi

sleep 10


docker stop $(docker ps -a | grep 'selenium' | awk '{print $1 }')
docker rm $(docker ps -a | grep 'selenium' | grep 'Exited' | awk '{print $1 }')

#docker stop $(docker ps -a | grep 'selenium' | awk '{print $1 }')
#docker rm $(docker ps -a | grep 'selenium' | awk '{print $1 }')

#docker stop selenium-hub
#docker rm   selenium-hub

dockerimage_compressed_tgz="selenium-hub-20180930.image.tgz"
dockerimage_name_version="selenium/hub:latest"
dockerimagelist=$(docker images | grep -v REPOSITORY | awk '{a=$1;b=$2;c=(a":"b);print c}')
found=0
#判断dockerimagelist里是否包含$dockerimage_name_version
case "${dockerimagelist}" in
  *${dockerimage_name_version}*)
    found=1;;
esac

if found==1 ; then
   echo "${dockerimage_name_version}已经存在."
else
   echo "本系统尚未加载${dockerimage_name_version} docker镜像,马上加载,请稍等一会儿..."
   gunzip -c ${dockerimage_compressed_tgz} | docker load
fi
#gunzip -c selenium-hub-20180930.image.tgz | docker load
#gunzip -c selenium-node-chrome-20180930.image.tgz | docker load
#gunzip -c selenium-node-firefox-20180930.image.tgz | docker load

firewall-cmd --add-port=4444/tcp --zone=public --permanent
firewall-cmd --reload

docker run -d -p 4444:4444 --name selenium-hub selenium/hub:latest

COUNT=$1
for i in $(seq 1 $COUNT);
    do 
        echo $i;
        #docker run -d --link selenium-hub:hub selenium/node-firefox:latest
        docker run -d --link selenium-hub:hub selenium/node-chrome:latest
    done

echo "http://localhost:4444/grid/console"
