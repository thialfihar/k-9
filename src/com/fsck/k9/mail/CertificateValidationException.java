
package com.fsck.k9.mail;

public class CertificateValidationException extends MessagingException {
    public static final long serialVersionUID = -1;

    private String mHost;
    private int mPort;

    public CertificateValidationException(String message, Throwable throwable, String host, int port) {
        super(message, throwable);
        this.mHost = host;
        this.mPort = port;
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }
}