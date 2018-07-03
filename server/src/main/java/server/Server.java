package server;

import ServerOnlyMessages.EmailRequest;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import messages.*;

import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server extends AbstractActor {
    private ConcurrentHashMap<String, User> regUsers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, User> loggedUsers = new ConcurrentHashMap<>();
    private ActorRef mailer;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RegMsg.class, m -> {
                    if (m.getCerti().verify()){
                        String toVal = getRandomString();
                        regUsers.put(m.getEmail(), new User (toVal, m.getEmail(), m.getCerti()));
                        mailer.tell(new EmailRequest(m.getEmail(), toVal), self());
                        scheduler.schedule(() -> {
                            if(!this.regUsers.get(m.getEmail()).isValidated()){
                                regUsers.remove(m.getEmail());
                            }
                        }, 3, TimeUnit.MINUTES);
                        sender().tell(new SimpleMsg ("You will soon receive an email with a verification key, type /validate *email* *key*\n"), self());
                    }
                    else {
                        sender().tell(new SimpleMsg("Certificate verification failed!\n"), self());
                    }

                })
                .match(ValMsg.class, m -> {
                    Message toSend = null;
                    if (regUsers.containsKey(m.getEmail())) {
                        User requestingUser = regUsers.get(m.getEmail());
                        if(requestingUser.getValString().equals(m.getValString())){
                            requestingUser.setValidated();
                            toSend = new SimpleMsg("Validated! You can try to log in.\n");
                        }
                        else {
                            regUsers.remove(m.getEmail());
                            toSend = new SimpleMsg("Validation failed! removing from waiting list.\n");
                        }
                    }
                    else {
                        toSend = new SimpleMsg("Validation failed - timeout! removing from waiting list.\n");
                    }
                    sender().tell(toSend, self());
                })
                .match(LoginMsg.class, m -> {
                    Message toSend = null;
                    if (regUsers.containsKey(m.getEmailToLog()) && !loggedUsers.containsKey(m.getEmailToLog())) {
                        User requestingUser = regUsers.get(m.getEmailToLog());
                        requestingUser.setChallange(getRandomString());
                        toSend = new PuzzleMsg(requestingUser.getChallange());
                    }
                    else {
                        toSend = new SimpleMsg("Email not found, log in failed.\n");
                    }
                    sender().tell(toSend, self());
                })
                .match(PuzzleAnswerMsg.class, m -> {
                    Message toSend = null;
                    if (regUsers.containsKey(m.getEmail())) {
                        User requestingUser = regUsers.get(m.getEmail());
                        if (verify(requestingUser.getChallange(), m.getSignedChallenge(), requestingUser.getCert().getPubKey())){
                            requestingUser.setUserActor(sender());
                            loggedUsers.put(m.getEmail(), requestingUser);
                            toSend = new SimpleMsg("Puzzle solved successfully!\n");
                        }
                        else {
                            toSend = new SimpleMsg("Puzzle can't be verified!\n");
                        }
                    }
                    else {
                        toSend = new SimpleMsg("Email not found, puzzle failed.\n");
                    }
                    sender().tell(toSend, self());
                })
                .match(LogoutMsg.class, m -> {
                    Message toSend = null;
                    if (loggedUsers.containsKey(m.getEmailToDisc())) {
                        User requestingUser = loggedUsers.get(m.getEmailToDisc());
                        requestingUser.setLoggedIn(false);
                        loggedUsers.remove(m.getEmailToDisc());
                        toSend = new SimpleMsg("Logged out successfully!\n");
                    }
                    else {
                        toSend = new SimpleMsg("Email not found, log out failed.\n");
                    }
                    sender().tell(toSend, self());
                })
                .match(UserListMsg.class, m -> {
                    String usersPrint = "Currently logged in users are:\n";
                    for (String email : loggedUsers.keySet()) {
                        usersPrint += "\t" + email + "\n";
                    }
                    sender().tell(new SimpleMsg(usersPrint), self());
                })
                .match(StartSessionMsg.class, m -> {
                    if (loggedUsers.containsKey(m.getRequestEmail())) {
                        User requestedUser = loggedUsers.get(m.getRequestEmail());
                        User askingUser = loggedUsers.get(m.getAskerEmail());

                        requestedUser.getUserActor().tell(new SessionCertificate(m.getAskerEmail(), askingUser.getCert()), self());
                        askingUser.getUserActor().tell(new SessionCertificate(m.getRequestEmail(), requestedUser.getCert()), self());


                    }
                    else {
                        sender().tell(new SimpleMsg("Email not found, session failed.\n"), self());
                    }
                })
                .match(SessionCertificateAck.class, m -> {
                    User requestedUser = loggedUsers.get(m.getRequestedEmail());
                    User askingUser = loggedUsers.get(m.getAskerEmail());
                    askingUser.setAckSession(m.isValidCerti());
                    if (!m.isValidCerti()){
                        requestedUser.getUserActor().tell(new SessionFail(), self());
                        askingUser.getUserActor().tell(new SessionFail(), self());
                    }
                    else if (requestedUser.isAckSession()) {
                        requestedUser.setSessionUser(askingUser);
                        askingUser.setSessionUser(requestedUser);
                        requestedUser.getUserActor().tell(new SessionSuccsses(), self());
                        requestedUser.getUserActor().tell(new SimpleMsg("Started session with " + m.getAskerEmail()  + "\n"), self());
                        askingUser.getUserActor().tell(new SessionSuccsses(), self());
                        askingUser.getUserActor().tell(new SimpleMsg("Started session with " + m.getRequestedEmail()  + "\n"), self());

                    }
                })
                .match(EndSessionMsg.class, m -> {
                    User requestingUser = loggedUsers.get(m.getEmail());
                    if(requestingUser.getSessionUser() != null) {
                        User sessUser = requestingUser.getSessionUser();
                        requestingUser.setSessionUser(null);
                        sessUser.setSessionUser(null);
                        requestingUser.getUserActor().tell(new SimpleMsg("Ended session with " + sessUser.getEmail()  + "\n"), self());
                        sessUser.getUserActor().tell(new SimpleMsg("Ended session with " + requestingUser.getEmail()  + "\n"), self());
                    }
                    else {
                        sender().tell(new SimpleMsg("Not currently in session.\n"), self());
                    }
                })
                .match(ChatMsg.class, m -> {
                    User sendingUser = loggedUsers.get(m.getEmail());
                    if (sendingUser.getSessionUser() != null) {
                        User requestedUser = sendingUser.getSessionUser();
                        requestedUser.getUserActor().tell(m, self());
                        System.out.println(m.getChatString());
                    }
                    else {
                        sender().tell(new SimpleMsg("Currently not in session!\n"), self());
                    }
                })
                .match(IgnMsg.class, m -> {})
                .matchAny(m -> {
                    System.out.println("Do I get here");
                })
                .build();
    }



    @Override
    public void preStart() throws Exception {
        mailer= getContext().actorOf(Props.create(EmailActor.class), "Mailer");

    }

    private String getRandomString() {
        return UUID.randomUUID().toString().replace("-", "");
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


}
