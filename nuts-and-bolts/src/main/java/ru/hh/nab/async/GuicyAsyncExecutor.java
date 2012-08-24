package ru.hh.nab.async;

import static com.google.common.collect.Iterables.reverse;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import com.google.inject.Key;
import static java.util.Arrays.asList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.health.monitoring.TimingsLogger;
import ru.hh.nab.hibernate.Transactional;
import ru.hh.nab.hibernate.TxInterceptor;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.scopes.ScopeClosure;

public class GuicyAsyncExecutor {
  private final Executor executor;
  private final Logger LOG = LoggerFactory.getLogger(GuicyAsyncExecutor.class);
  public static final ThreadLocal<Boolean> killThisThread = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };
  private final Injector inj;

  public GuicyAsyncExecutor(Injector inj, String name, int threads) {
    ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat(name + "-%d").build();
    this.inj = inj;
    this.executor = Executors.newFixedThreadPool(threads, tf);
  }

  public <T> Async<T> async(Callable<T> body, ScopeClosure... closures) {
    return new DeferredAsync<T>(body, null, closures);
  }

  public <T> Async<T> asyncWithRequestScope(Callable<T> body, RequestScope.RequestScopeClosure requestScopeClosure, ScopeClosure... closures) {
    return new DeferredAsync<T>(body, requestScopeClosure, closures);
  }

  public <T> Async<T> asyncWithTransferredRequestScope(Callable<T> body) {
    return new DeferredAsync<T>(body, null, RequestScope.currentClosure());
  }

  static void killThisThreadAfterExecution() {
    killThisThread.set(true);
  }

  private class InjectableAsyncDecorator<T> extends Async<T> {

    private final Async<T> target;
    private final ScopeClosure[] closures;

    public InjectableAsyncDecorator(Async<T> target, ScopeClosure[] closures) {
      this.target = target;
      this.closures = closures;
    }

    @Override
    protected void runExposed(Callback<T> onSuccess, Callback<Throwable> onError) throws Exception {
      for (ScopeClosure c : closures)
        c.enter();
      try {
        inj.injectMembers(target);
        target.runExposed(onSuccess, onError);
      } finally {
        for (ScopeClosure closure : reverse(asList(closures)))
          closure.leave();
      }
    }
  }

  private class DeferredAsync<T> extends Async<T> {
    private final Callable<T> body;
    private final @Nullable RequestScope.RequestScopeClosure requestScopeClosure;
    private final ScopeClosure[] closures;

    public DeferredAsync(Callable<T> body, @Nullable RequestScope.RequestScopeClosure requestScopeClosure, ScopeClosure... closures) {
      this.body = body;
      this.requestScopeClosure = requestScopeClosure;
      this.closures = closures;
    }

    @Override
    protected void runExposed(final Callback<T> onSuccess, final Callback<Throwable> onError) throws Exception {
      if (requestScopeClosure != null)
        requestScopeClosure.enter();
      final TimingsLogger logger;
      try {
        logger = inj.getInstance(TimingsLogger.class);
      } finally {
        if (requestScopeClosure != null)
          requestScopeClosure.leave();
      }
      logger.probe("async-submission");
      executor.execute(new Runnable() {
        @Override
        public void run() {
          logger.probe("async-execution");
          try {
            final Callable<T> injCallable = new Callable<T>() {
              @Override
              public T call() throws Exception {
                inj.injectMembers(body);
                return body.call();
              }
            };
            Callable<T> callable = new Callable<T>() {
              @Override
              public T call() throws Exception {
                for (ScopeClosure c : closures)
                  c.enter();
                try {
                  Transactional ann = body.getClass().getMethod("call").getAnnotation(Transactional.class);
                  if (ann == null)
                    return injCallable.call();
                  TxInterceptor interceptor = inj.getInstance(Key.get(TxInterceptor.class, ann.value()));
                  return interceptor.invoke(ann, injCallable);
                } finally {
                  for (ScopeClosure closure : reverse(asList(closures)))
                    closure.leave();
                  logger.probe("async-finish");
                }
              }
            };
            onSuccess.call(callable.call());
          } catch (Throwable e) {
            logger.setErrorState();
            try {
              onError.call(e);
            } catch (Throwable ee) {
              LOG.error("Exception in error handler", ee);
              LOG.error("Original exception was", e);
            }
          }
        }
      });
      if (GuicyAsyncExecutor.killThisThread.get()) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
