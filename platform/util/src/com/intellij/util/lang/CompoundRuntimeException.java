/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.lang;

import com.intellij.util.Consumer;
import com.intellij.util.EmptyConsumer;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mike
 */
public class CompoundRuntimeException extends RuntimeException {
  private final List<? extends Throwable> myExceptions;

  public CompoundRuntimeException(@NotNull List<? extends Throwable> throwables) {
    myExceptions = throwables;
  }

  @Override
  public synchronized Throwable getCause() {
    return ContainerUtil.getFirstItem(myExceptions);
  }

  public List<Throwable> getExceptions() {
    return new ArrayList<>(myExceptions);
  }

  @Override
  public String getMessage() {
    return processAll(Throwable::getMessage, EmptyConsumer.getInstance());
  }

  @Override
  public String getLocalizedMessage() {
    return processAll(Throwable::getLocalizedMessage, EmptyConsumer.getInstance());
  }

  @Override
  public String toString() {
    return processAll(Throwable::toString, EmptyConsumer.getInstance());
  }

  @Override
  public void printStackTrace(final PrintStream s) {
    processAll(throwable -> {
      throwable.printStackTrace(s);
      return "";
    }, s::print);
  }

  @Override
  public void printStackTrace(final PrintWriter s) {
    processAll(throwable -> {
      throwable.printStackTrace(s);
      return "";
    }, s::print);
  }

  private String processAll(@NotNull Function<? super Throwable, String> exceptionProcessor, @NotNull Consumer<? super String> stringProcessor) {
    if (myExceptions.size() == 1) {
      Throwable throwable = myExceptions.get(0);
      String s = exceptionProcessor.fun(throwable);
      stringProcessor.consume(s);
      return s;
    }

    StringBuilder sb = new StringBuilder();
    String line = "CompositeException (" + myExceptions.size() + " nested):\n------------------------------\n";
    stringProcessor.consume(line);
    sb.append(line);

    for (int i = 0; i < myExceptions.size(); i++) {
      Throwable exception = myExceptions.get(i);

      line = "[" + i + "]: ";
      stringProcessor.consume(line);
      sb.append(line);

      line = exceptionProcessor.fun(exception);
      if (line == null) {
        line = "null\n";
      }
      else if (!line.endsWith("\n")) line += '\n';
      stringProcessor.consume(line);
      sb.append(line);
    }

    line = "------------------------------\n";
    stringProcessor.consume(line);
    sb.append(line);

    return sb.toString();
  }

  public static void throwIfNotEmpty(@Nullable List<? extends Throwable> throwables) {
    if (ContainerUtil.isEmpty(throwables)) {
      return;
    }

    if (throwables.size() == 1) {
      ExceptionUtil.rethrow(throwables.get(0));
    }
    else {
      throw new CompoundRuntimeException(throwables);
    }
  }
}
