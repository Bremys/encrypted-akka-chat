# Project Structure

## Used Libraries

* Akka - Enables concurrency and message sending using the actor model
* JLine - A java library enabling the use of a sophisticated terminal with auto completion.
* SimpleJavaMail - Enables sending an email through Java.

We have also used a simple Akka hello world project as the basis for our project.

## Client

The client contains a main class called Client.java which handles and defines how to act according to each received message.

It also contains some helpful classes such as an output actor and input actor that enable concurrent message handling and printing. Some of its code is taken directly from github.

We will later expand on a large package called Messages since it is shared with the server.

## Server

The server contains a main class called Server.java which handles and defines how to act according to messages from the user. It keeps information regarding all registered users and all logged in users.

It also contains a class called User.java which which has the relevant fields we need to save for a user through his time in the system. 

Another actor called EmailActor.java is used for sending emails concurrently.

## Messages

It contains the following classes:

* ChatMsg.java - A simple message representing the content sent between two users.
* SessionFail.java - indication that starting the session failed.
* EndSessionMsg.java - A request to end the current session
* PuzzleAnswerMsg.java - An answer to the puzzle sent by the server when logging in
* SessionSuccsses.java - indication that starting the session has succeded.
* IgnMsg.java - A message that is to be ignored, added for technical reasons.
* PuzzleMsg.java - A riddle sent by the server to verify that the user requesting log in has the required private key.
* SimpleMsg.java - Sent to the user with information required to print
* LoginMsg.java - A request by the user to log in.
* RegMsg.java - A request by the user to register.
* StartSessionMsg.java - A request to start a session.
* LogoutMsg.java - A request to logout.
* SessionCertificateAck.java - Acknowlodging that the certificate is verified
* UserListMsg.java - Was used for debugging, obsolete.
* Message.java - All messages extend this class.
* SessionCertificate.java - The certificate of the user in session.
* ValMsg.java - Validating the register by using a string sent to the user's email.