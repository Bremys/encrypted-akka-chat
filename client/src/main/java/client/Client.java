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

    @Override
    public void preStart() throws Exception {
        server = getContext().actorSelection("akka.tcp://ChatSystem@127.0.0.1:3553/user/Server");
        createReaderTerminal();
        getContext().actorOf(Props.create(inputActor.class, reader, terminal), "inActor");
        outActor = getContext().actorOf(Props.create(outputActor.class, reader, terminal), "outActor");
        outActor.tell("Hint: use Tab for auto completion\n", self());
        outActor.tell("You need to start with /load <RSA key pair path>\n", self());

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, m -> {
                    server.tell(parseInput(m), self());
                })
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
                .match(SimpleMsg.class, m -> {
                    outActor.tell(m.getMsg(), self());
                })
                .match(PuzzleMsg.class, m -> {
                    sender().tell(new PuzzleAnswerMsg(this.currEmail, signString(m.getChallenge())), self());
                })
                .match(SessionCertificate.class, m-> {
                    this.session = Embryo;
                    this.sessionCerti = m.getCertificate();
                    sender().tell(new SessionCertificateAck(this.currEmail, m.getCertiEmail(), m.getCertificate().verify()), self());
                })
                .match(SessionFail.class, m-> {
                    this.session = Dead;
                    this.sessionCerti = null;
                })
                .match(SessionSuccsses.class, m-> {
                    session = Running;
                })
                .matchAny(m -> {
                    outActor.tell("DEBUGGING: " + m.toString() + "\n", self());
                })
                .build();
    }


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
        // else if (userInput[0].equals("/loggedlist")){
        //     msg = new UserListMsg();
        // }
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

    private void createReaderTerminal () {
        TerminalBuilder builder = TerminalBuilder.builder();
        Completer completer = new ArgumentCompleter(new StringsCompleter(
            "/load",
            "/login",
            "/register",
            "/disc",
            "/validate",
            "/loggedlist",
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
