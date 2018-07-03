package messages;

public class ChatMsg extends Message {
    private String email;
    private String chatString;
    private String signedString;

    public ChatMsg(String email, String chatString, String signedString) {

        this.email = email;
        this.chatString = chatString;
        this.signedString = signedString;
    }

    public String getEmail() {
        return email;
    }

    public String getChatString() {
        return chatString;
    }

    public String getSignedString() {
        return signedString;
    }
}
