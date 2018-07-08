package client;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import messages.*;
import org.jline.reader.Completer;
import org.jline.builtins.Completers;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import static client.SessionState.Dead;
import static client.SessionState.Embryo;
import static client.SessionState.Running;
import static java.nio.charset.StandardCharsets.UTF_8;

enum SessionState {
    Embryo, Running, Dead
}


public class Client extends AbstractActor {
    private ActorSelection server;
    private ActorRef outActor;
    private Terminal terminal;
    private LineReader reader;
    private String currEmail;
    private KeyPair keyPair;
    private SessionState session;
    private MyCertificate sessionCerti;


    //Runs on start up of actor, creates the output and input actors.
    @Override
    public void preStart() throws Exception {
        server = getContext().actorSelection("akka.tcp://ChatSystem@127.0.0.1:3553/user/Server");
        createReaderTerminal();
        getContext().actorOf(Props.create(inputActor.class, reader, terminal), "inActor");
        outActor = getContext().actorOf(Props.create(outputActor.class, reader, terminal), "outActor");
        outActor.tell("Hint: use Tab for auto completion\n", self());
        outActor.tell("You need to start with /load <RSA key pair path>\n", self());

    }
    
    /*
    Our main function, it is built in a match case
    in the following format:

        .match(MessageClass.class, a function that receives a message m of class MessageClass)

    In which the function is run in case a message of that type is received.
    A similar function appears in the server.
    */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
        /*
        Receiveing input strings from an input Actor 
        (can be viewed essentialy as a different thread)
        parses them, and sends the correct message to the server
        */
        .match(String.class, m -> { 
            server.tell(parseInput(m), self());
        })
        /**
         * A simple chat message sent between two users which is verified, 
         * decrypted and printed
         */
        .match(ChatMsg.class, m-> {
            String toPrint;
            if(verify(m.getChatString(), m.getSignedString(), sessionCerti.getPubKey())){
                toPrint = getTimeString() + m.getEmail() + ": "
                        + decrypt(m.getChatString(), keyPair.getPrivate()) + "\n";
            }
            else {
                toPrint = "Can't verify!\n";
            }
            self().tell(new SimpleMsg(toPrint), self());
        })
        /**
         * A simple msg is a request to print a string, the string itself
         * is sent to an output actor
         */
        .match(SimpleMsg.class, m -> {
            outActor.tell(m.getMsg(), self());
        })
        /**
         * In use in a login in order to verify that the email
         * requesting login indeed has access to his private key
         */
        .match(PuzzleMsg.class, m -> {
            sender().tell(new PuzzleAnswerMsg(this.currEmail, signString(m.getChallenge())), self());
        })
        /**
         * Receiving a certificate of the other user we are requesting a session
         * with.
         */
        .match(SessionCertificate.class, m-> {
            this.session = Embryo;
            this.sessionCerti = m.getCertificate();
            sender().tell(new SessionCertificateAck(this.currEmail, m.getCertiEmail(), m.getCertificate().verify()), self());
        })
        /**
         * Cancellation of session in case the certificate verification
         * failed.
         */
        .match(SessionFail.class, m-> {
            this.session = Dead;
            this.sessionCerti = null;
        })
        /**
         * Notifying of a sucessful session
         */
        .match(SessionSuccsses.class, m-> {
            session = Running;
        })
        .matchAny(m -> {
            outActor.tell("DEBUGGING: " + m.toString() + "\n", self());
        })
        .build();
    }

    //Runs on every input string and returns a message to send to the server
    private Message parseInput(String fullMsg) {
        Message msg = null;
        String [] userInput = fullMsg.split("\\s+");
        if (userInput[0].equals("/register")) {
            MyCertificate certi = new MyCertificate(keyPair.getPublic(), keyPair.getPrivate());
            msg = new RegMsg(userInput[1], certi);
        }
        else if (userInput[0].equals("/validate")){
            msg = new ValMsg (userInput[1], userInput[2]); // /validate email key
        }
        else if (userInput[0].equals("/login")) {
            this.currEmail = userInput[1];
            msg = new LoginMsg(userInput[1]);
        }
        else if (userInput[0].equals("/disc")){
            msg = new LogoutMsg(this.currEmail);
        }
        else if (userInput[0].equals("/chat")){
            msg = new StartSessionMsg (userInput[1], this.currEmail);
        }
        else if (userInput[0].equals("/endchat")){
            msg = new EndSessionMsg (this.currEmail);
        }
        else if (userInput[0].equals("/load")) {
            FileInputStream file = null;
            try {
                file = new FileInputStream(new File(userInput[1]));
                ObjectInputStream oi = new ObjectInputStream(file);
                keyPair = (KeyPair) oi.readObject();
                oi.close();
                file.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            msg = new IgnMsg();
        }
        else {
            self().tell(new SimpleMsg(getTimeString() + this.currEmail + ": " + fullMsg + "\n"), self());
            //Encryption of the message 
            try {
                Cipher encryptCipher = Cipher.getInstance("RSA");
                encryptCipher.init(Cipher.ENCRYPT_MODE, sessionCerti.getPubKey());
                byte[] cipherText = encryptCipher.doFinal(fullMsg.getBytes(UTF_8));
                String toSend = Base64.getEncoder().encodeToString(cipherText);
                msg = new ChatMsg(this.currEmail, toSend, signString(toSend));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
        return msg;
    }


    /**
     * The terminal interface used in the project
     */
    private void createReaderTerminal () {
        TerminalBuilder builder = TerminalBuilder.builder();
        Completer completer = new ArgumentCompleter(new StringsCompleter(
            "/load",
            "/login",
            "/register",
            "/disc",
            "/validate",
            "/chat",
            "/endchat"
    ),
    new Completers.FileNameCompleter()
    );
        terminal = null;
        try {
            terminal = builder.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .build();
    }

    //Sign using the public key loaded earlier
    private String signString(String plaintext) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(keyPair.getPrivate());
            sign.update(plaintext.getBytes(UTF_8));
            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return "";
    }


    // Verification function
    public boolean verify(String plaintext, String signed, PublicKey pubKey) {

        Signature publicSignature = null;
        try {
            publicSignature = Signature.getInstance("SHA256withRSA");
            publicSignature.initVerify(pubKey);
            publicSignature.update(plaintext.getBytes());
            byte[] signatureBytes = Base64.getDecoder().decode(signed);

            return publicSignature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return false;
    }


    // Decryption function
    private String decrypt (String cipherText, PrivateKey privateKey) {
        byte[] bytes = Base64.getDecoder().decode(cipherText);

        Cipher decriptCipher = null;
        try {
            decriptCipher = Cipher.getInstance("RSA");
            decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(decriptCipher.doFinal(bytes), UTF_8);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getTimeString () {
        String timeRecv = new SimpleDateFormat("HH:mm:ss").format(new Date());
        return "[" + timeRecv + "] ";
    }

}
