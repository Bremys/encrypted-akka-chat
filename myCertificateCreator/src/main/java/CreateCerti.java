import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.security.*;

public class CreateCerti {
    public static void main (String[] args) {
        if (args.length != 1) {
            System.out.println("Please enter createCerti  <PathToWrite>");
            return;
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, new SecureRandom());
            KeyPair pair = generator.generateKeyPair();

            FileOutputStream file = new FileOutputStream(new File(args[0]));
            ObjectOutputStream objStream = new ObjectOutputStream(file);

            // Write objects to file
            objStream.writeObject(pair);
            objStream.close();
            file.close();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
