package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

enum ERROR_CODES {
    PARSING_ERROR_CODE(-1);
    private final int value;

    private ERROR_CODES(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}