package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import android.util.Log;

public class SomeServiceImpl implements SomeService {

    @Override
    public void doSomething(String value) {
        Log.d("SomeServiceImpl",""+value);
    }

    @Override
    public Model action(String v0, Long v1, AnotherModel model) {
        // Some action

        return new Model();
    }
}