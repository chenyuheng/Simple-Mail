package co.yuheng.simplemail;

import java.util.List;
import javax.net.ssl.SSLSocketFactory;

public abstract class MailProtocolHandler {
    protected SSLSocketFactory sslSocketFactory;

    public MailProtocolHandler() {
        sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }
    
    public abstract boolean validate(MailboxCredential credential) throws RuntimeException;
    public abstract int getTotalNum(MailboxCredential credential) throws RuntimeException;
    public abstract List<Mail> list(MailboxCredential credential, int page, int pageSize) throws RuntimeException;
    public abstract Mail getMail(MailboxCredential credential, int id) throws RuntimeException;
}