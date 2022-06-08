package com.github.nmuzhichin.jsonrpc.model.response.errors;

public class StacktraceError extends AbstractError<Throwable> {
    private Throwable throwable;

    public StacktraceError(int code, String message, Throwable throwable) {
        super(code, message);
        this.throwable = throwable;
    }

    public StacktraceError(Predefine predefineError, Throwable throwable) {
        this(predefineError.getCode(), predefineError.getMessage(), throwable);
    }

    @Override
    public Throwable getData() {
        return throwable;
    }

    @Override
    public void dropData() {
        throwable = null;
    }
}
