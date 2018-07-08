# End to end encrypted chat
An encrypted chat created for a mini project course in Computer Security

##  Pre-requisites
If you only need to run you need to first install java by using the command:

        sudo apt install default-jdk

If you want to build and compile the code you need to also install Maven:

        sudo apt install maven

## Building the project

We have provided a script shell that creates a dist folder which contains an executeable server, an executeable client and a certificate creator for testing purposes which can be run using the command:

        ./install

## Running the project

First, you need to run the server by running the following commands from the main directory:

        cd ./dist/server
        ./server

Then, in case you havent already, create some Key Pairs as a certificate using the certificate creators.
That is done by running the following commands from the main directory:

        cd ./dist/client
        ./createCerti <keyPairPath>

After creating some Key Pairs (should be generally corresponding to the amount of clients) you need to run some clients

That is done by running the following commands from the main directory:

        cd ./dist/client
        ./client

## Using the program

For the following section, using the Tab auto-complete will help greatly.

Assuming the server is running, after running a client there is a series of commands to be done in the chat in order to register:

        /load <chosenKeyPair>
        /register <email>
        /validate <email> <validation string sent to email>

At this point the user is registered and assuming the server didn't shutdown anytime, you can close the client and open it again and you will still be able to log in with the email, assuming the same loaded key pair. We will assume the client has already loaded the correct key pair and continue:

        /login <email>
        /chat <requested other email>
        <chat messages>
        .
        .
        .
        /endchat
        /disc

In order to close the Akka client you need to press Ctrl+D followed by Ctrl+C.