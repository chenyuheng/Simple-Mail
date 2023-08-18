package co.yuheng.simplemail;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;

enum MailProtocal {
    POP3, IMAP
}

public class MailboxCredential {
    String host;
    int port;
    MailProtocal protocal;
    String username;
    transient String password;
    String token;

    public MailboxCredential(String host, int port, MailProtocal protocal, String username, String password) {
        this.host = host;
        this.port = port;
        this.protocal = protocal;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public MailProtocal getProtocal() {
        return protocal;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String hashString() {
        String str =  host + "\n" + port + "\n" + protocal + "\n" + username + "\n" + password;
        token = calculateSHA256(str);
        return token;
    }

    public static String calculateSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] inputBytes = input.getBytes();
            byte[] sha256Bytes = md.digest(inputBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : sha256Bytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}