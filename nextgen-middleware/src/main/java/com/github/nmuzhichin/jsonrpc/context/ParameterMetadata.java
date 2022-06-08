package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.annotation.Constraint;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public final class ParameterMetadata implements Metadata {
    private final String parameterName;
    private final boolean parameterNullable;

    private final Class<?> parameterClass;
    private final boolean parameterPredefine;
    private final Map<Constraint.Type, String> constraints;

    ParameterMetadata(@Nonnull final String parameterName,
                      @Nonnull final boolean parameterNullable,
                      @Nonnull final Class<?> parameterClass,
                      boolean parameterPredefine,
                      @Nonnull final Map<Constraint.Type, String> constraints) {

        this.parameterName = parameterName;
        this.parameterNullable = parameterNullable;
        this.parameterClass = parameterClass;
        this.parameterPredefine = parameterPredefine;
        this.constraints = constraints;
    }

    public String getParameterName() {
        return parameterName;
    }

    public boolean getParameterNullable() { return parameterNullable; }

    public Class<?> getParameterClass() {
        return parameterClass;
    }

    public boolean isParameterPredefine() {
        return parameterPredefine;
    }

    public boolean isConstraintPresent() {
        return !constraints.isEmpty();
    }

    public String getConstraintByType(final Constraint.Type type) {
        return constraints.get(type);
    }

    public boolean containsConstraintType(final Constraint.Type type) {
        return constraints.containsKey(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterMetadata that = (ParameterMetadata) o;

        if (parameterPredefine != that.parameterPredefine) return false;
        if (!Objects.equals(parameterName, that.parameterName)) return false;
        return Objects.equals(parameterClass, that.parameterClass);
    }

    @Override
    public int hashCode() {
        int result = parameterName != null ? parameterName.hashCode() : 0;
        result = 31 * result + (parameterClass != null ? parameterClass.hashCode() : 0);
        result = 31 * result + (parameterPredefine ? 1 : 0);
        result = 31 * result + (constraints != null ? constraints.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String cacheName(final String postfix) {
        return parameterName + '_' + parameterClass + '_' + parameterPredefine + '_' + postfix;
    }
}
