package com.github.nmuzhichin.jsonrpc.model;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface Versionable {
    public static final String JSON_RPC = "2.0";

    /**
     * A String specifying the version of the JSON-RPC protocol. MUST be exactly "2.0".
     */
    String getJsonrpc();
}
