package org.ngbp.jsonrpc4jtestharness.core.ws;

import android.content.Context;

import org.ngbp.jsonrpc4jtestharness.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class GenerateSSLContext implements IGenerateSSLContext {
    private Context context;

    public GenerateSSLContext(Context context) {
        this.context = context;
    }

    @Override
    public SSLContext getInitializedSSLContext(String password) throws GeneralSecurityException, IOException {

        InputStream i = context.getResources().openRawResource(R.raw.mykey);
        KeyStore keystore;
        try {
            keystore = KeyStore.getInstance("PKCS12");
            keystore.load(i, password.toCharArray());

        } finally {
            i.close();
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(keystore, password.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(keystore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;
    }
}
