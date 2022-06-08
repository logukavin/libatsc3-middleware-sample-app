package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.api.MutateListener;
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer;
import com.github.nmuzhichin.jsonrpc.cache.CacheProvider;
import com.github.nmuzhichin.jsonrpc.cache.NoOpCache;
import com.github.nmuzhichin.jsonrpc.internal.asserts.Assert;
import com.github.nmuzhichin.jsonrpc.internal.logger.Logger;
import com.github.nmuzhichin.jsonrpc.normalizer.ValueNormalization;
import com.github.nmuzhichin.jsonrpc.normalizer.WithoutNormalization;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ConsumerBuilder {
    private static final Logger log = Logger.of(ConsumerBuilder.class);

    private CacheProvider cacheProvider;
    private ExecutorService executorService;
    private ValueNormalization normalization;
    private MutateListener mutateListener;
    private Map<String, MethodMetadata> methodMetadataMap;

    public ConsumerBuilder() {
        this.cacheProvider = NoOpCache.NO_OP_CACHE; // No cache provider by default
        this.executorService = Executors.newWorkStealingPool(); // ForkJoin pool by default
        this.normalization = WithoutNormalization.WITHOUT_NORMALIZATION; // Without normalization (return same object)
        this.methodMetadataMap = new HashMap<>(1 << 8);
        this.mutateListener = result -> result; // Simple re-translation
    }

    public ConsumerBuilder cacheProvider(@Nonnull CacheProvider cacheProvider) {
        Assert.requireNotNull(cacheProvider, "CacheProvider must be present.");
        this.cacheProvider = cacheProvider;
        return this;
    }

    public ConsumerBuilder valueNormalizer(@Nonnull ValueNormalization valueNormalization) {
        Assert.requireNotNull(valueNormalization, "ValueNormalization must be present.");
        this.normalization = valueNormalization;
        return this;
    }

    public ConsumerBuilder threadPool(@Nonnull ExecutorService executorService) {
        Assert.requireNotNull(executorService, "ThreadPool must be present.");
        this.executorService = executorService;
        return this;
    }

    public ConsumerBuilder methodMetadataStorageMap(@Nonnull Map<String, MethodMetadata> methodMetadataMap) {
        Assert.requireNotNull(methodMetadataMap, "Map must be present.");
        this.methodMetadataMap = methodMetadataMap;
        return this;
    }

    public ConsumerBuilder resultListener(@Nonnull MutateListener resultMutateListener) {
        Assert.requireNotNull(resultMutateListener, "Listener must be present.");
        this.mutateListener = resultMutateListener;
        return this;
    }

    public RpcConsumer build() {
        if (normalization.getClass().equals(WithoutNormalization.class)) {
            log.warn("Custom normalization hasn't been set, uses default (without) normalization.");
        }

        return new RpcConsumerContextual(
                new InvocationContextProcessor(methodMetadataMap, cacheProvider, normalization, mutateListener),
                executorService
        );
    }
}
