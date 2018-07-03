package messages;


public class RegMsg extends Message {
    private String email;
    private MyCertificate certi;

    public String getEmail() {
        return email;
    }

    public MyCertificate getCerti() {
        return certi;
    }

    public RegMsg(String email, MyCertificate certi) {
        this.email = email;
        this.certi = certi;
    }
}
