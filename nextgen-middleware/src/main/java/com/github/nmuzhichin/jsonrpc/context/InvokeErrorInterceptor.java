package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcError;
import com.github.nmuzhichin.jsonrpc.api.Interceptor;
import com.github.nmuzhichin.jsonrpc.internal.exceptions.InternalProcessException;
import com.github.nmuzhichin.jsonrpc.internal.exceptions.ValidationException;
import com.github.nmuzhichin.jsonrpc.internal.function.ExceptionSuppressor;
import com.github.nmuzhichin.jsonrpc.internal.logger.Logger;
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;
import com.github.nmuzhichin.jsonrpc.model.response.errors.MeaningError;
import com.github.nmuzhichin.jsonrpc.model.response.errors.StacktraceError;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;

import static com.github.nmuzhichin.jsonrpc.model.response.errors.Error.Predefine.*;

final class InvokeErrorInterceptor implements Interceptor<ExceptionSuppressor<?>, CustomErrorMetadata, Object> {
    private static final Logger log = Logger.of(InvokeErrorInterceptor.class);

    @Override
    public Object intercept(final ExceptionSuppressor<?> suppressor, final CustomErrorMetadata customErrorMetadata) {
        final Error err;
        try {
            return suppressor.apply();
        } catch (WrongMethodTypeException e) {
            log.error(e.getMessage(), e);
            err = new StacktraceError(INVALID_PARAMS, e);
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            err = new MeaningError(INVALID_PARAMS);
        } catch (InternalProcessException e) {
            log.error(e.getMessage(), e);
            err = new StacktraceError(INTERNAL_ERROR, e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            err = new MeaningError(SERVER_ERROR.getCode(), e.getMessage());
        } catch (Throwable thr) {
            log.error(thr.getMessage(), thr);
            err = new MeaningError(SERVER_ERROR);
        }

        return customErrorMetadata.isCustomError() ? customErrorHandler(err, customErrorMetadata) : err;
    }

    @SuppressWarnings("ConstantConditions")
    private Object customErrorHandler(@Nonnull final Error v, @Nonnull final CustomErrorMetadata customErrorMetadata) {

        final JsonRpcError.Mode mode = customErrorMetadata.getErrorMode();
        final Class<? extends Throwable> customErrorClass = customErrorMetadata.getCustomErrorClass();

        final Throwable suppress;
        if (customErrorMetadata.getAcquiredException() != null) {
            suppress = customErrorMetadata.getAcquiredException();
        } else {
            final ExceptionSuppressor<Throwable> e = () -> (Throwable) MethodHandles
                    .publicLookup()
                    .findConstructor(customErrorClass, MethodType.methodType(void.class))
                    .invoke();

            suppress = e.suppress();
            customErrorMetadata.setAcquiredException(suppress);
        }

        if (mode.equals(JsonRpcError.Mode.THROW)) {
            ExceptionSuppressor<?> t =
                    () -> MethodHandles.throwException(Throwable.class, customErrorClass).invoke(suppress);

            t.rethrow(); // throw exception
        } else {
            return new StacktraceError(v.getCode(), v.getMessage(), suppress);
        }

        return v;
    }
}
