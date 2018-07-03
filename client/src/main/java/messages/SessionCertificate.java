package messages;

public class SessionCertificate extends Message {
    private String certiEmail;
    private MyCertificate certificate;

    public SessionCertificate(String certiEmail, MyCertificate certificate) {
        this.certiEmail = certiEmail;
        this.certificate = certificate;
    }

    public String getCertiEmail() {
        return certiEmail;
    }

    public MyCertificate getCertificate() {
        return certificate;
    }
}
