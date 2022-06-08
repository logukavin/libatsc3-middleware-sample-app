package com.github.nmuzhichin.jsonrpc.model.response.errors;

public class MeaningError extends AbstractError<String> {
    private String meaning; // MAY omitted

    public MeaningError(int code, String message, String meaning) {
        super(code, message);
        this.meaning = meaning;
    }

    public MeaningError(Predefine predefineError) {
        this(predefineError.getCode(), predefineError.getMessage(), predefineError.getMeaning());
    }

    public MeaningError(int code, String message) {
        this(code, message, null);
    }

    @Override
    public String getData() {
        return meaning;
    }

    @Override
    public void dropData() {
        meaning = null;
    }
}
