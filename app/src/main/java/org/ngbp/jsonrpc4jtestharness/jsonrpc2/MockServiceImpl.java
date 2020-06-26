package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import android.util.Log;

import com.github.nmuzhichin.jsonrpc.internal.exceptions.InternalProcessException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MockServiceImpl implements MockService {
//    private static final Logger log = LoggerFactory.getLogger(MockService.class);

    @Override
    public List<String> of(String single, Integer... varargs) {
        final List<String> collect = Arrays.stream(varargs).map(String::valueOf).collect(Collectors.toList());
        collect.add(0, single);
        return collect;
    }

    @Override
    public String self(String single) {
        return single;
    }

    @Override
    public void invoice(MockModel model) {
       Log.d("TAG",model.toString());
    }

    @Override
    public String voice(final MockModel model) {
        return model.toString();
    }

    @Override
    public String randomize() {
        return" 10";
    }

    @Override
    public long randomizeV2(final byte[] seed) {
        return 11;
    }

    @Override
    public String randomizeV3(final Long id) {
        return ""+id;
    }

    @Override
    public String strictCall(final String name, final Long age) {
        return String.format("\nName: %s\nAge: %d", name, age);
    }

    @Override
    public Integer withCustomError() {
        throw new RuntimeException("Business err.");
    }

    @Override
    public Integer withCustomErrorV2() {
        throw new InternalProcessException("Internal err.");
    }

    @Override
    public Integer withCustomErrorThrow() {
        throw new RuntimeException("Business err.");
    }
}