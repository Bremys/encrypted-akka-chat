#! /bin/bash
mkdir dist
cd ./dist
mkdir client
mkdir server
mkdir ./server/jars
mkdir ./client/jars
cd ..

cd ./client
mvn package
cp ./target/client.jar ../dist/client/jars/client.jar
cp ./client ../dist/client/client
cd ..

cd ./myCertificateCreator
mvn package
cp ./target/createCerti.jar ../dist/client/jars/createCerti.jar
cp ./createCerti ../dist/client/createCerti
cd ..

cd ./server
mvn package
cp ./target/server.jar ../dist/server/jars/server.jar
cp ./server ../dist/server/server
cd ..