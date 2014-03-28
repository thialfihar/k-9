
package com.fsck.k9.mail.internet;

import android.util.Log;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.K9;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;

public class TextBody implements Body {

    /**
     * Immutable empty byte array
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private String mBody;
    private String mEncoding;
    private String mCharset = "utf-8";
    // Length of the message composed (as opposed to quoted). I don't like the name of this variable and am open to
    // suggestions as to what it should otherwise be. -achen 20101207
    private Integer mComposedMessageLength;
    // Offset from position 0 where the composed message begins.
    private Integer mComposedMessageOffset;
    private boolean rawOutput = false;

    public TextBody(String body) {
        this.mBody = body;
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException {
        if (mBody != null) {
            byte[] bytes = mBody.getBytes(mCharset);
            if (rawOutput || MimeUtil.ENC_8BIT.equalsIgnoreCase(mEncoding) || MimeUtil.ENC_7BIT.equalsIgnoreCase(mEncoding)) {
                out.write(bytes);
            } else {
                QuotedPrintableOutputStream qp = new QuotedPrintableOutputStream(out, false);
                qp.write(bytes);
                qp.flush();
                qp.close();
            }
        }
    }

    public void setRawOutput( boolean rawOutput ) {
    	this.rawOutput = rawOutput;
    }

    public boolean isRawOutput() {
    	return rawOutput;
    }

    /**
     * Get the text of the body in it's unencoded format.
     * @return
     */
    public String getText() {
        return mBody;
    }

    /**
     * Returns an InputStream that reads this body's text.
     */
    public InputStream getInputStream() throws MessagingException {
        try {
            byte[] b;
            if (mBody != null) {
                b = mBody.getBytes(mCharset);
            } else {
                b = EMPTY_BYTE_ARRAY;
            }
            return new ByteArrayInputStream(b);
        } catch (UnsupportedEncodingException uee) {
            return null;
        }
    }

    public void setEncoding(String encoding) {
        mEncoding = encoding;
    }

    public String getEncoding() {
    	return mEncoding;
    }

    public void setCharset(String charset) {
        mCharset = charset;
    }

    public Integer getComposedMessageLength() {
        return mComposedMessageLength;
    }

    public void setComposedMessageLength(Integer composedMessageLength) {
        this.mComposedMessageLength = composedMessageLength;
    }

    public Integer getComposedMessageOffset() {
        return mComposedMessageOffset;
    }

    public void setComposedMessageOffset(Integer composedMessageOffset) {
        this.mComposedMessageOffset = composedMessageOffset;
    }
}
