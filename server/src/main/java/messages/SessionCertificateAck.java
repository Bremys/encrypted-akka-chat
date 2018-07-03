package messages;

public class SessionCertificateAck extends Message {
    private String askerEmail;
    private String requestedEmail;
    private boolean validCerti;

    public String getAskerEmail() {
        return askerEmail;
    }

    public String getRequestedEmail() {
        return requestedEmail;
    }

    public boolean isValidCerti() {
        return validCerti;
    }

    public SessionCertificateAck(String askerEmail, String requestedEmail, boolean validCerti) {

        this.askerEmail = askerEmail;
        this.requestedEmail = requestedEmail;
        this.validCerti = validCerti;
    }
}
