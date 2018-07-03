package messages;

public class PuzzleAnswerMsg extends Message {
    private String email;
    private String signedChallenge;

    public String getEmail() {
        return email;
    }

    public String getSignedChallenge() {
        return signedChallenge;
    }

    public PuzzleAnswerMsg(String email, String signedChallenge) {

        this.email = email;
        this.signedChallenge = signedChallenge;
    }
}
