#!/bin/bash

./gradlew build
#java -cp ./build/libs/netty-hello-world.jar com.test.netty.server.AppServer &> /databricks/server_stdout &
#java -cp ./build/libs/netty-hello-world.jar com.test.netty.client.Client &> /databricks/client_stdout

java -cp ./build/libs/netty-hello-world.jar com.test.netty.server.AppServer &
java -cp ./build/libs/netty-hello-world.jar com.test.netty.client.Client