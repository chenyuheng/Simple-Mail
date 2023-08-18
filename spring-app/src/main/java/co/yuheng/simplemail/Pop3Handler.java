package co.yuheng.simplemail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocket;

public class Pop3Handler extends MailProtocolHandler {

    private volatile static Pop3Handler instance;

    private Pop3Handler() {
        super();
    }

    public static Pop3Handler getInstance() {
        if (instance == null) {
            synchronized (Pop3Handler.class) {
                if (instance == null) {
                    instance = new Pop3Handler();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean validate(MailboxCredential credential) throws RuntimeException {
        try {
            SSLSocket socket = login(credential);
            try {
                socket.close();
            } catch (IOException e) {
            }
            return true;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getTotalNum(MailboxCredential credential) throws RuntimeException {
        try {
            SSLSocket socket = login(credential);
            return readTotalNum(socket);
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public List<Mail> list(MailboxCredential credential, int page, int pageSize) throws RuntimeException {
        try {
            SSLSocket socket = login(credential);
            int total = readTotalNum(socket);
            int start = total - (page - 1) * pageSize;
            int end = total - page * pageSize + 1;
            if (end < 1) {
                end = 1;
            }
            if (start < end || start > total) {
                throw new RuntimeException("Invalid parameter, total mail count: " + total);
            }
            List<Mail> mails = new ArrayList<>();
            for (int i = start; i >= end; i--) {
                mails.add(readMail(socket, i));
            }
            return mails;
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Mail getMail(MailboxCredential credential, int id) throws RuntimeException {
        try {
            SSLSocket socket = login(credential);
            return readMail(socket, id);
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int readTotalNum(SSLSocket socket) throws RuntimeException, IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        out.write(("STAT\r\n").getBytes());
        String res = receive(in, false);
        if (!res.startsWith("+OK")) {
            throw new RuntimeException(res);
        }
        String[] stat = res.split(" ");
        return Integer.parseInt(stat[1]);
    }

    private Mail readMail(SSLSocket socket, int id) throws RuntimeException, IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        out.write(("RETR " + id + "\r\n").getBytes());
        String res = receive(in, true);
        if (!res.startsWith("+OK")) {
            throw new RuntimeException(res);
        }
        Mail mail = MimePart.parse(res, id);
        return mail;
    }

    private SSLSocket login(MailboxCredential credential) throws RuntimeException {
        try {
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(credential.getHost(), credential.getPort());
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            String res = receive(in, false);
            if (!res.startsWith("+OK")) {
                throw new RuntimeException(res);
            }
            out.write(("USER " + credential.getUsername() + "\r\n").getBytes());
            res = receive(in, false);
            if (!res.startsWith("+OK")) {
                throw new RuntimeException(res);
            }
            out.write(("PASS " + credential.getPassword() + "\r\n").getBytes());
            res = receive(in, false);
            if (!res.startsWith("+OK")) {
                throw new RuntimeException(res);
            }
            return socket;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String receive(InputStream in, boolean dotTerminated) {
        StringBuilder sb = new StringBuilder();
        int data;
        try {
            while (sb.length() == 0 || in.available() > 0) {
                data = in.read();
                sb.append((char) data);
                if (sb.length() == 3 && sb.toString().startsWith("-ERR")) {
                    dotTerminated = false;
                }
                if (dotTerminated && sb.toString().endsWith("\r\n.\r\n")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
