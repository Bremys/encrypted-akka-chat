package messages;

public class PuzzleMsg extends Message {
    private String challenge;

    public String getChallenge() {
        return challenge;
    }

    public PuzzleMsg(String challenge) {

        this.challenge = challenge;
    }
}
