package server;

import akka.actor.ActorRef;
import messages.MyCertificate;

import java.util.concurrent.atomic.AtomicBoolean;


public class User {
    private ActorRef userActor;
    private User sessionUser;
    private boolean ackSession;

    public boolean isAckSession() {
        return ackSession;
    }

    public void setAckSession(boolean ackSession) {
        this.ackSession = ackSession;
    }

    private String email;
    private String challange;

    public String getChallange() {
        return challange;
    }

    public void setChallange(String challange) {
        this.challange = challange;
    }

    private MyCertificate cert;
    private String valString;
    private boolean loggedIn;
    private AtomicBoolean validated = new AtomicBoolean(false);

    public String getEmail() {
        return email;
    }

    public void setUserActor(ActorRef userActor) {
        this.userActor = userActor;
    }

    public User(String valString, String email, MyCertificate cert) {
        this.email = email;
        this.valString = valString;
        this.sessionUser = null;
        this.loggedIn = false;
        this.cert = cert;
    }

    public ActorRef getUserActor() {
        return userActor;
    }

    public MyCertificate getCert() {
        return cert;
    }

    public String getValString() {
        return valString;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isValidated() {
        return validated.get();
    }

    public void setCert(MyCertificate cert) {
        this.cert = cert;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void setValidated() {
        this.validated.set(true);
    }

    public User getSessionUser() {
        return sessionUser;
    }

    public void setSessionUser(User sessionUser) {
        this.sessionUser = sessionUser;
    }
}
