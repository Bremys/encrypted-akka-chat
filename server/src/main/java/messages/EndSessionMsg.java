package messages;

public class EndSessionMsg extends Message {
    private String email;

    public String getEmail() {
        return email;
    }

    public EndSessionMsg(String email) {

        this.email = email;
    }
}
