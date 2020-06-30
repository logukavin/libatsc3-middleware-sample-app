package org.ngbp.jsonrpc4jtestharness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer;
import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder;
import com.github.nmuzhichin.jsonrpc.normalizer.JacksonNormalization;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class UseConstraintTest {
    private static final AtomicInteger counter = new AtomicInteger();

    @Test
    public void test()  {
        final RpcConsumer consumer = new ConsumerBuilder()
                .valueNormalizer(new JacksonNormalization(new ObjectMapper()))
                .build();

        Assert.assertNotNull(consumer);
        Assert.assertNotNull(consumer.getContext());
    }
}