package messages;

import messages.Message;

public class LogoutMsg extends Message {
    private String emailToDisc;

    public String getEmailToDisc() {
        return emailToDisc;
    }

    public LogoutMsg(String emailToDisc) {

        this.emailToDisc = emailToDisc;
    }
}
