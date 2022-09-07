package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.api.Interceptor;
import com.github.nmuzhichin.jsonrpc.internal.logger.Logger;
import com.github.nmuzhichin.jsonrpc.normalizer.ValueNormalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class InvokeArgumentInterceptor implements Interceptor<List<ParameterMetadata>, Map<String, Object>, List<Object>> {
    private static final Logger log = Logger.of(InvokeArgumentInterceptor.class);

    private final ValueNormalization normalization;

    InvokeArgumentInterceptor(final ValueNormalization normalization) {
        this.normalization = normalization;
    }

    @Override
    public List<Object> intercept(final List<ParameterMetadata> args, final Map<String, Object> params) {
        final ArrayList<Object> orderedArgs = new ArrayList<>(args.size());

//        if (params.size() < args.size()) {
//            log.error("Not found {} parameter(s).", params.size() - args.size());
//        }

        for (ParameterMetadata arg : args) {
            Object value = params.get(arg.getParameterName());
            //jjustman-2022-06-08 - if our parameter has nullable=true and we don't have this parameter name present, add a null entry
            if(arg.getParameterNullable() && value == null) {
                orderedArgs.add(null);
            } else {
                if (arg.isConstraintPresent()) {
                    ParameterValidate.validate(arg, value);
                }

                if (!Map.class.isAssignableFrom(arg.getParameterClass()) && Map.class.isAssignableFrom(value.getClass())) {
                    orderedArgs.add(normalization.normalize(value, arg.getParameterClass()));
                } else {
                    //jjustman-2022-08-30 - super hack...
                    if(arg.getParameterClass().getName().equalsIgnoreCase("java.lang.Double") && value.getClass().getName().equalsIgnoreCase("java.lang.Integer")) {
                        value = Double.valueOf(((Integer)value).doubleValue());
                    }
                    orderedArgs.add(value);
                }
            }
        }

        return orderedArgs;
    }
}
