package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer;
import com.github.nmuzhichin.jsonrpc.cache.CacheProvider;
import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.normalizer.JacksonNormalization;
import com.github.nmuzhichin.jsonrpc.normalizer.ValueNormalization;

import java.util.concurrent.Executors;
