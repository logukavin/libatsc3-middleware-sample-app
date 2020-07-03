package org.ngbp.jsonrpc4jtestharness.core.ws;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

interface IGenerateSSLContext {
    SSLContext getInitializedSSLContext(String password) throws GeneralSecurityException, IOException;
}
