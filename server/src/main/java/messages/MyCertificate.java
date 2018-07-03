package messages;

import java.io.Serializable;
import java.security.*;
import java.util.Base64;

public class MyCertificate implements Serializable {
    private PublicKey pubKey;
    private String signedPubKey;

    public PublicKey getPubKey() {
        return pubKey;
    }

    public MyCertificate(PublicKey pubKey, PrivateKey priKey) {
        this.pubKey = pubKey;
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(priKey);
            sign.update(pubKey.getEncoded());
            this.signedPubKey = Base64.getEncoder().encodeToString(sign.sign());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

    }

    public boolean verify() {

        Signature publicSignature = null;
        try {
            publicSignature = Signature.getInstance("SHA256withRSA");
            publicSignature.initVerify(pubKey);
            publicSignature.update(pubKey.getEncoded());
            byte[] signatureBytes = Base64.getDecoder().decode(signedPubKey);

            return publicSignature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return false;
    }

}
