package ServerOnlyMessages;
import java.io.Serializable;

public class EmailRequest implements Serializable {
    private String email;
    private String randomizedString;

    public String getEmail() {
        return email;
    }

    public String getRandomizedString() {
        return randomizedString;
    }

    public EmailRequest(String email, String randomizedString) {

        this.email = email;
        this.randomizedString = randomizedString;
    }
}
