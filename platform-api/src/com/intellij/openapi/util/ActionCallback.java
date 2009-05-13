/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.openapi.util;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class ActionCallback implements Disposable {
  private final ExecutionCallback myDone;
  private final ExecutionCallback myRejected;

  public ActionCallback() {
    myDone = new ExecutionCallback();
    myRejected = new ExecutionCallback();
  }

  public ActionCallback(int countToDone) {
    assert countToDone >= 1;

    myDone = new ExecutionCallback(countToDone);
    myRejected = new ExecutionCallback();
  }

  public void setDone() {
    myDone.setExecuted();
    Disposer.dispose(this);
  }

  public boolean isDone() {
    return myDone.isExecuted();
  }

  public boolean isRejected() {
    return myRejected.isExecuted();
  }

  public boolean isProcessed() {
    return isDone() || isRejected();
  }

  public void setRejected() {
    myRejected.setExecuted();
    Disposer.dispose(this);
  }

  public final ActionCallback doWhenDone(@NotNull final Runnable runnable) {
    myDone.doWhenExecuted(runnable);
    return this;
  }

  public final ActionCallback doWhenRejected(@NotNull final Runnable runnable) {
    myRejected.doWhenExecuted(runnable);
    return this;
  }

  public final ActionCallback doWhenProcessed(@NotNull final Runnable runnable) {
    doWhenDone(runnable);
    doWhenRejected(runnable);
    return this;
  }

  public final ActionCallback notifyWhenDone(final ActionCallback child) {
    return doWhenDone(new Runnable() {
      public void run() {
        child.setDone();
      }
    });
  }

  public final ActionCallback processOnDone(Runnable runnable, boolean requiresDone) {
    if (requiresDone) {
      return doWhenDone(runnable);
    } else {
      runnable.run();
      return this;
    }
  }

  public static class Done extends ActionCallback {
    public Done() {
      setDone();
    }
  }

  public static class Rejected extends ActionCallback {
    public Rejected() {
      setRejected();
    }
  }

  public static class Chunk {

    Set<ActionCallback> myCallbacks = new LinkedHashSet<ActionCallback>();

    public void add(ActionCallback callback) {
      myCallbacks.add(callback);
    }

    public ActionCallback getWhenProcessed() {
      final ActionCallback result = new ActionCallback(myCallbacks.size());
      for (ActionCallback each : myCallbacks) {
        each.doWhenProcessed(new Runnable() {
          public void run() {
            result.setDone();
          }
        });
      }
      return result;
    }
  }


  public void dispose() {
  }
}
