package client;

public class Main {

  public static void main(String[] args) {
    akka.Main.main(new String[] {
            Client.class.getName()
    });
  }
}
