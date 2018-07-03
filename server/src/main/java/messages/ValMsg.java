package messages;

public class ValMsg extends Message {
    private String email;
    private String valString;

    public ValMsg(String email, String valString) {
        this.email = email;
        this.valString = valString;
    }

    public String getEmail() {
        return email;
    }

    public String getValString() {
        return valString;
    }
}
