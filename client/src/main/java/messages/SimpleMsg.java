package messages;

public class SimpleMsg extends Message {
    private String msg;

    public String getMsg() {
        return msg;
    }

    public SimpleMsg(String msg) {

        this.msg = msg;
    }
}
