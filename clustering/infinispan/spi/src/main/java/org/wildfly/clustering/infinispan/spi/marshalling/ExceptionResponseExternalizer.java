/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.infinispan.spi.marshalling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.Ids;
import org.infinispan.marshall.exts.ThrowableExternalizer;
import org.infinispan.remoting.responses.ExceptionResponse;
import org.jboss.modules.Module;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class ExceptionResponseExternalizer implements AdvancedExternalizer<ExceptionResponse> {
    private static final long serialVersionUID = -3836546973615308515L;

    @Override
    public ExceptionResponse readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new ExceptionResponse((Exception) ThrowableExternalizer.INSTANCE.readObject(input));
    }

    @Override
    public void writeObject(ObjectOutput output, ExceptionResponse response) throws IOException {
        // Ensure exception does not contain any generic throwables
        Throwable exception = response.getException();
        ThrowableExternalizer.INSTANCE.writeObject(output, createMarshallable(exception, Module.forClass(exception.getClass())));
    }

    private static Throwable createMarshallable(Throwable exception, Module module) throws IOException {
        // If cause was loaded from a different module, marshal as a RemoteException
        Class<? extends Throwable> targetClass = Objects.equals(module, Module.forClass(exception.getClass())) ? exception.getClass() : RemoteException.class;
        Throwable result = createThrowable(targetClass, exception.getMessage(), (exception.getCause() != null) ? createMarshallable(exception.getCause(), module) : null);
        result.setStackTrace(exception.getStackTrace());
        for (Throwable suppressed : exception.getSuppressed()) {
            result.addSuppressed(createMarshallable(suppressed, module));
        }
        return result;
    }

    private static Throwable createThrowable(Class<? extends Throwable> exceptionClass, String message, Throwable cause) throws IOException {
        Constructor<? extends Throwable> emptyConstructor = getConstructor(exceptionClass);
        Constructor<? extends Throwable> messageConstructor = getConstructor(exceptionClass, String.class);
        Constructor<? extends Throwable> causeConstructor = getConstructor(exceptionClass, Throwable.class);
        Constructor<? extends Throwable> messageCauseConstructor = getConstructor(exceptionClass, String.class, Throwable.class);
        try {
            if (cause != null) {
                if (message != null) {
                    if (messageCauseConstructor != null) {
                        return messageCauseConstructor.newInstance(message, cause);
                    }
                } else {
                    if (causeConstructor != null) {
                        return causeConstructor.newInstance(cause);
                    }
                }
            }
            Throwable exception = (message != null) ? ((messageConstructor != null) ? messageConstructor.newInstance(message) : null) : ((emptyConstructor != null) ? emptyConstructor.newInstance() : null);
            if (exception == null) {
                throw new NoSuchMethodException(String.format("%s(%s)", exceptionClass.getName(), (message != null) ? String.class.getName() : ""));
            }
            if (cause != null) {
                exception.initCause(cause);
            }
            return exception;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }

    private static <T> Constructor<T> getConstructor(Class<T> targetClass, Class<?>... parameterTypes) {
        try {
            return targetClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public Integer getId() {
        return Ids.EXCEPTION_RESPONSE;
    }

    @Override
    public Set<Class<? extends ExceptionResponse>> getTypeClasses() {
        return Collections.singleton(ExceptionResponse.class);
    }
}
