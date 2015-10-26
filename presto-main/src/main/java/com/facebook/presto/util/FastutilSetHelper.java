/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.util;

import com.facebook.presto.metadata.FunctionRegistry;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.type.Type;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.booleans.BooleanOpenHashSet;
import it.unimi.dsi.fastutil.doubles.DoubleHash;
import it.unimi.dsi.fastutil.doubles.DoubleOpenCustomHashSet;
import it.unimi.dsi.fastutil.longs.LongHash;
import it.unimi.dsi.fastutil.longs.LongOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Set;

import static com.facebook.presto.metadata.OperatorType.EQUAL;
import static com.facebook.presto.metadata.OperatorType.HASH_CODE;
import static com.facebook.presto.spi.StandardErrorCode.INTERNAL_ERROR;

public final class FastutilSetHelper
{
    private FastutilSetHelper() {}

    @SuppressWarnings({"unchecked"})
    public static Set<?> toFastutilHashSet(Set<?> set, Type type, FunctionRegistry registry)
    {
        Class<?> javaElementType = type.getJavaType();
        if (javaElementType == long.class) {
            return new LongOpenCustomHashSet((Collection<Long>) set, new LongStrategy(registry, type));
        }
        if (javaElementType == double.class) {
            return new DoubleOpenCustomHashSet((Collection<Double>) set, new DoubleStrategy(registry, type));
        }
        if (javaElementType == boolean.class) {
            return new BooleanOpenHashSet((Collection<Boolean>) set);
        }
        else if (!type.getJavaType().isPrimitive()) {
            return new ObjectOpenCustomHashSet(set, new ObjectStrategy(registry, type));
        }
        else {
            throw new UnsupportedOperationException("Unsupported native type in set: " + type.getJavaType() + " with type " + type.getTypeSignature());
        }
    }

    public static boolean in(boolean booleanValue, BooleanOpenHashSet set)
    {
        return set.contains(booleanValue);
    }

    public static boolean in(double doubleValue, DoubleOpenCustomHashSet set)
    {
        return set.contains(doubleValue);
    }

    public static boolean in(long longValue, LongOpenCustomHashSet set)
    {
        return set.contains(longValue);
    }

    public static boolean in(Object objectValue, ObjectOpenCustomHashSet<?> set)
    {
        return set.contains(objectValue);
    }

    private static final class LongStrategy
            implements LongHash.Strategy
    {
        private final MethodHandle hashCodeHandle;
        private final MethodHandle equalsHandle;

        private LongStrategy(FunctionRegistry registry, Type type)
        {
            hashCodeHandle = registry.getScalarFunctionImplementation(registry.resolveOperator(HASH_CODE, ImmutableList.of(type))).getMethodHandle();
            equalsHandle = registry.getScalarFunctionImplementation(registry.resolveOperator(EQUAL, ImmutableList.of(type, type))).getMethodHandle();
        }

        @Override
        public int hashCode(long value)
        {
            try {
                return Ints.checkedCast((long) hashCodeHandle.invokeExact(value));
            }
            catch (Throwable t) {
                Throwables.propagateIfInstanceOf(t, Error.class);
                Throwables.propagateIfInstanceOf(t, PrestoException.class);
                throw new PrestoException(INTERNAL_ERROR, t);
            }
        }

        @Override
        public boolean equals(long a, long b)
        {
            try {
                return (boolean) equalsHandle.invokeExact(a, b);
            }
            catch (Throwable t) {
                Throwables.propagateIfInstanceOf(t, Error.class);
                Throwables.propagateIfInstanceOf(t, PrestoException.class);
                throw new PrestoException(INTERNAL_ERROR, t);
            }
        }
    }

    private static final class DoubleStrategy
            implements DoubleHash.Strategy
    {
        private final MethodHandle hashCodeHandle;
        private final MethodHandle equalsHandle;

        private DoubleStrategy(FunctionRegistry registry, Type type)
        {
            hashCodeHandle = registry.getScalarFunctionImplementation(registry.resolveOperator(HASH_CODE, ImmutableList.of(type))).getMethodHandle();
            equalsHandle = registry.getScalarFunctionImplementation(registry.resolveOperator(EQUAL, ImmutableList.of(type, type))).getMethodHandle();
        }

        @Override
        public int hashCode(double value)
        {
            try {
                return Ints.checkedCast((long) hashCodeHandle.invokeExact(value));
            }
            catch (Throwable t) {
                Throwables.propagateIfInstanceOf(t, Error.class);
                Throwables.propagateIfInstanceOf(t, PrestoException.class);
                throw new PrestoException(INTERNAL_ERROR, t);
            }
        }

        @Override
        public boolean equals(double a, double b)
        {
            try {
                return (boolean) equalsHandle.invokeExact(a, b);
            }
            catch (Throwable t) {
                Throwables.propagateIfInstanceOf(t, Error.class);
                Throwables.propagateIfInstanceOf(t, PrestoException.class);
                throw new PrestoException(INTERNAL_ERROR, t);
            }
        }
    }

    private static final class ObjectStrategy
            implements Hash.Strategy
    {
        private final MethodHandle hashCodeHandle;
        private final MethodHandle equalsHandle;

        private ObjectStrategy(FunctionRegistry registry, Type type)
        {
            hashCodeHandle = registry.getScalarFunctionImplementation(registry.resolveOperator(HASH_CODE, ImmutableList.of(type)))
                    .getMethodHandle()
                    .asType(MethodType.methodType(long.class, Object.class));
            equalsHandle = registry.getScalarFunctionImplementation(registry.resolveOperator(EQUAL, ImmutableList.of(type, type)))
                    .getMethodHandle()
                    .asType(MethodType.methodType(boolean.class, Object.class, Object.class));
        }

        @Override
        public int hashCode(Object value)
        {
            try {
                return Ints.checkedCast((long) hashCodeHandle.invokeExact(value));
            }
            catch (Throwable t) {
                Throwables.propagateIfInstanceOf(t, Error.class);
                Throwables.propagateIfInstanceOf(t, PrestoException.class);
                throw new PrestoException(INTERNAL_ERROR, t);
            }
        }

        @Override
        public boolean equals(Object a, Object b)
        {
            try {
                return (boolean) equalsHandle.invokeExact(a, b);
            }
            catch (Throwable t) {
                Throwables.propagateIfInstanceOf(t, Error.class);
                Throwables.propagateIfInstanceOf(t, PrestoException.class);
                throw new PrestoException(INTERNAL_ERROR, t);
            }
        }
    }
}
