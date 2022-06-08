package com.github.nmuzhichin.jsonrpc.model.response.errors;

import java.util.EnumSet;

/**
 * When a rpc call encounters an error,
 * the Response Object MUST contain the error member with a value that is a Object.
 *
 * @see MeaningError
 * @see StacktraceError
 */
public interface Error {
    /**
     * A Number that indicates the error type that occurred.
     * This MUST be an integer.
     */
    int getCode();

    /**
     * A String providing a short description of the error.
     * The message SHOULD be limited to a concise single sentence.
     */
    String getMessage();

    /**
     * A Primitive or Structured value that contains additional information about the error.
     * This may be omitted.
     * The value of this member is defined by the Server (e.g. detailed error information, nested errors etc.).
     */
    Object getData();

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    public enum Predefine {
        //@formatter:off
        PARSE_ERROR(-32700, "Parse error", "Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text"),
        INVALID_REQUEST(-32600, "Invalid Request", "The JSON sent is not a valid Request object"),
        METHOD_NOT_FOUND(-32601, "Method not found", "The method does not exist / is not available"),
        INVALID_PARAMS(-32602, "Invalid params", "Invalid method parameter(s)"),
        INTERNAL_ERROR(-32603, "Internal error", "Internal JSON-RPC error"),
        SERVER_ERROR(-32000, "Server error", "Reserved for implementation-defined server-errors");
        //@formatter:on

        private final int code;
        private final String message;
        private final String meaning;

        Predefine(int code, String message, String meaning) {
            this.code = code;
            this.message = message;
            this.meaning = meaning;
        }

        public static Predefine findByCode(int code) {
            return EnumSet
                    .allOf(Predefine.class)
                    .stream()
                    .filter(it -> it.getCode() == code)
                    .findAny()
                    .orElse(SERVER_ERROR);
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getMeaning() {
            return meaning;
        }
    }
}
