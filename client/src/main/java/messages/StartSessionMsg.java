package messages;

public class StartSessionMsg extends Message{
    private String requestEmail;
    private String askerEmail;

    public StartSessionMsg(String requestEmail, String askerEmail) {
        this.requestEmail = requestEmail;
        this.askerEmail = askerEmail;
    }

    public String getRequestEmail() {
        return requestEmail;
    }

    public String getAskerEmail() {
        return askerEmail;
    }
}
