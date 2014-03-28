package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.apache.james.mime4j.util.MimeUtil;

import java.io.*;

/**
 * A Body that is backed by a temp file. The Body exposes a getOutputStream method that allows
 * the user to write to the temp file. After the write the body is available via getInputStream
 * and writeTo one time. After writeTo is called, or the InputStream returned from
 * getInputStream is closed the file is deleted and the Body should be considered disposed of.
 */
public class BinaryTempFileBody implements Body {
    private static File mTempDirectory;
   
    /*
     * Whether we should just write our body out without any processing on the <b>next</b>
     * call to writeTo() only. We only need rawOutput for incoming pgp/mime signed message
     * parts when we initially store them.
     */
    private boolean rawOutput = false;
    
    private File mFile;

    String mEncoding = null;
    
    public static void setTempDirectory(File tempDirectory) {
        mTempDirectory = tempDirectory;
    }

    public void setRawOutput( boolean rawOutput ) {
    	this.rawOutput = rawOutput;
    }
    
    public boolean isRawOutput() {
    	return rawOutput;
    }
    
    public void setEncoding(String encoding) throws MessagingException {
        mEncoding  = encoding;
    }

    public String getEncoding() {
    	return mEncoding;
    }
    
    public BinaryTempFileBody() {
        if (mTempDirectory == null) {
            throw new
            RuntimeException("setTempDirectory has not been called on BinaryTempFileBody!");
        }
    }

    public OutputStream getOutputStream() throws IOException {
        mFile = File.createTempFile("body", null, mTempDirectory);
        mFile.deleteOnExit();
        return new FileOutputStream(mFile);
    }
    
    public File getFile() {
    	return mFile;
    }
    
    public int getSize() {
    	
    	int size = 0;
    	if( mFile != null ) {
    		size = ( int )mFile.length();
    	}
    	
    	return size;
    	
    }

    public InputStream getInputStream() throws MessagingException {
        try {
            return new BinaryTempFileBodyInputStream(new FileInputStream(mFile));
        } catch (IOException ioe) {
            throw new MessagingException("Unable to open body", ioe);
        }
    }

    public void writeTo(OutputStream out) throws IOException, MessagingException {
        InputStream in = getInputStream();
        try {
            boolean closeStream = false;
            if( !rawOutput ) {
            	if (MimeUtil.isBase64Encoding(mEncoding)) {
            		out = new Base64OutputStream(out);
		            closeStream = true;
		        } else if (MimeUtil.isQuotedPrintableEncoded(mEncoding)){
		            out = new QuotedPrintableOutputStream(out, false);
		            closeStream = true;
		        }
            } /*else {
            	rawOutput = false;
            }*/
            try {
                IOUtils.copy(in, out);
            } finally {
                if (closeStream) {
                    out.close();
                }
            }
        } finally {
            in.close();
        }
    }

    class BinaryTempFileBodyInputStream extends FilterInputStream {
        public BinaryTempFileBodyInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
            	/*
            	if( decoded ) {
            		mFile.delete();
            	}
            	*/
            }
        }

        public void closeWithoutDeleting() throws IOException {
            super.close();
        }
    }
}
