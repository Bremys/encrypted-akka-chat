package messages;

public class LoginMsg extends Message {
    private String emailToLog;

    public LoginMsg(String emailToLog) {
        this.emailToLog = emailToLog;
    }

    public String getEmailToLog() {
        return emailToLog;
    }
}
