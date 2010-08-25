// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars

import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.util.PoolUtils
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import jsr166y.forkjoin.RecursiveTask

/**
 * Enables a ParallelArray-based (from JSR-166y) DSL on collections. In general cases the Parallel Array implementation
 * shows to be much faster (10 - 20 times) compared to the executor service implementation in GParsExecutorsPool.
 * E.g.
 <pre>
 GParsPool.withPool(5) {final AtomicInteger result = new AtomicInteger(0)
 [1, 2, 3, 4, 5].eachParallel {result.addAndGet(it)}assertEquals 15, result}GParsPool.withPool(5) {final List result = [1, 2, 3, 4, 5].collectParallel {it * 2}assert ([2, 4, 6, 8, 10].equals(result))}GParsPool.withPool(5) {assert [1, 2, 3, 4, 5].everyParallel {it > 0}assert ![1, 2, 3, 4, 5].everyParallel {it > 1}}</pre>
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class GParsPool {

    /**
     * Maps threads to their appropriate thread pools
     */
    private static final ThreadLocalPools currentPoolStack = new ThreadLocalPools()

    /**
     * Caches the default pool size.
     */
    private static final int defaultPoolSize = PoolUtils.retrieveDefaultPoolSize()

    /**
     * Retrieves the pool assigned to the current thread.
     */
    protected static retrieveCurrentPool() {
        currentPoolStack.current
    }

    /**
     * Creates a new pool with the default size()
     */
    private static createPool() {
        return createPool(PoolUtils.retrieveDefaultPoolSize())
    }

    /**
     * Creates a new pool with the given size()
     */
    private static createPool(int poolSize) {
        return createPool(poolSize, createDefaultUncaughtExceptionHandler())
    }

    private static createPool(int poolSize, UncaughtExceptionHandler handler) {
        if (!(poolSize in 1..Integer.MAX_VALUE)) throw new IllegalArgumentException("Invalid value $poolSize for the pool size has been specified. Please supply a positive int number.")
        final jsr166y.forkjoin.ForkJoinPool pool = new jsr166y.forkjoin.ForkJoinPool(poolSize)
        pool.uncaughtExceptionHandler = handler
        return pool
    }

    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>GParsPoolUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * GParsPool.withPool {GParsPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withPool(Closure cl) {
        return withPool(defaultPoolSize, cl)
    }

    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>GParsPoolUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * GParsPool.withPool(5) {GParsPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withPool(int numberOfThreads, Closure cl) {
        return withPool(numberOfThreads, createDefaultUncaughtExceptionHandler(), cl)
    }

    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>GParsPoolUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * GParsPool.withPool(5, handler) {GParsPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param handler Handler for uncaught exceptions raised in code performed by the pooled threads
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withPool(int numberOfThreads, UncaughtExceptionHandler handler, Closure cl) {
        final jsr166y.forkjoin.ForkJoinPool pool = createPool(numberOfThreads, handler)
        try {
            return withExistingPool(pool, cl)
        } finally {
            pool.shutdown()
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * Reuses an instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>GParsPoolUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * GParsPool.withExistingPool(anotherPool) {GParsPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}*  </pre>
     * @param pool The thread pool to use, the pool will not be shutdown after this method returns
     */
    public static withExistingPool(jsr166y.forkjoin.ForkJoinPool pool, Closure cl) {

        currentPoolStack << pool
        def result = null
        try {
            use(GParsPoolUtil) {
                result = cl(pool)
            }
        } finally {
            currentPoolStack.pop()
        }
        return result
    }

    /**
     * Just like withExistingPool() registers a thread pool, but doesn't install the GParsPoolUtil category.
     * Used by ParallelEnhancer's Parallel mixins. 
     */
    static ensurePool(final jsr166y.forkjoin.ForkJoinPool pool, final Closure cl) {
        currentPoolStack << pool
        try {
            return cl(pool)
        } finally {
            currentPoolStack.pop()
        }
    }

    /**
     * Starts multiple closures in separate threads, collecting their return values
     * Reuses the pool defined by the surrounding withPool() call.
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return The result values of all closures
     * @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static List<Object> executeAsyncAndWait(Closure... closures) {
        return GParsExecutorsPoolUtil.processResult(executeAsync(closures))
    }

    /**
     * Starts multiple closures in separate threads, collecting their return values
     * Reuses the pool defined by the surrounding withPool() call.
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return The result values of all closures
     * @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static List<Object> executeAsyncAndWait(List<Closure> closures) {
        return executeAsyncAndWait(* closures)
    }

    /**
     * Starts multiple closures in separate threads, collecting Futures for their return values
     * Reuses the pool defined by the surrounding withPool() call.
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return Futures for the result values or exceptions of all closures
     */
    public static List<Future<Object>> executeAsync(Closure... closures) {
        jsr166y.forkjoin.ForkJoinPool pool = retrieveCurrentPool()
        if (pool == null) throw new IllegalStateException("No active Fork/Join thread pool available to execute closures asynchronously. Consider wrapping the function call with GParsPool.withPool().")
        List<Future<Object>> result = closures.collect {cl ->
            pool.submit([compute: { cl.call() }] as RecursiveTask)
        }
        result
    }

    /**
     * Starts multiple closures in separate threads, collecting Futures for their return values
     * Reuses the pool defined by the surrounding withPool() call.
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return Futures for the result values or exceptions of all closures
     */
    public static List<Future<Object>> executeAsync(List<Closure> closures) {
        return executeAsync(* closures)
    }

    /**
     * Runs the supplied closures asynchronously and in parallel, returning the first result obtained and cancelling the other (slower) calculations.
     * Typically used to run several different calculations in parallel, all of which are supposed to give the same result,
     * but may last different amount of time each. If the system has enough threads available, the calculations can be test-run
     * in parallel and the fastest result is then used, while the other results are cancelled or discarded.
     * @param alternatives All the functions to invoke in parallel
     * @return The fastest result obtained
     */
    public static def speculate(List<Closure> alternatives) {
        speculate(* alternatives)
    }

    /**
     * Runs the supplied closures asynchronously and in parallel, returning the first result obtained and cancelling the other (slower) calculations.
     * Typically used to run several different calculations in parallel, all of which are supposed to give the same result,
     * but may last different amount of time each. If the system has enough threads available, the calculations can be test-run
     * in parallel and the fastest result is then used, while the other results are cancelled or discarded.
     * @param alternatives All the functions to invoke in parallel
     * @return The fastest result obtained
     */
    public static def speculate(Closure... alternatives) {
        def result = new DataFlowVariable()
        def futures = new DataFlowVariable()
        futures << GParsPool.executeAsync(alternatives.collect {
            original ->
            {->
                //noinspection GroovyEmptyCatchBlock
                try {
                    def localResult = original()
                    futures.val*.cancel(true)
                    result << localResult
                } catch (Exception ignore) { }
            }
        })
        return result.val
    }

    private static UncaughtExceptionHandler createDefaultUncaughtExceptionHandler() {
        return {Thread failedThread, Throwable throwable ->
            System.err.println "Error processing background thread ${failedThread.name}: ${throwable.message}"
            throwable.printStackTrace(System.err)
        } as UncaughtExceptionHandler
    }

    /**
     * Starts a ForkJoin calculation with the supplied root worker and waits for the result.
     * @param rootWorker The worker that calculates the root of the Fork/Join problem
     * @return The result of the whole calculation
     */
    public static <T> T runForkJoin(final AbstractForkJoinWorker<T> rootWorker) {
        def pool = GParsPool.retrieveCurrentPool()
        if (pool == null) throw new IllegalStateException("Cannot initialize ForkJoin. The pool has not been set. Perhaps, we're not inside a GParsPool.withPool() block.")
        return pool.submit(rootWorker).get()
    }

    /**
     * Starts a ForkJoin calculation with the supplied root worker and waits for the result.
     * @param rootWorker The worker that calculates the root of the Fork/Join problem
     * @return The result of the whole calculation
     */
    public static <T> T runForkJoin(final Object... args) {
        if (args.size() == 0) throw new IllegalArgumentException("No arguments specified to the runForkJoin() method.")
        if (!(args[-1] instanceof Closure)) throw new IllegalArgumentException("A closure to run implementing the requested Fork/Join algorithm must be specified as the last argument passed to the runForkJoin() method.")
        Closure code = args[-1] as Closure
        if (args.size() - 1 != code.maximumNumberOfParameters) throw new IllegalArgumentException("The supplied Fork/Join closure expects ${code.maximumNumberOfParameters} arguments while only ${args.size()} arguments have been supplied to the orchestrate() method.")
        return runForkJoin(new FJWorker<T>(args))
    }
}

final class FJWorker<T> extends AbstractForkJoinWorker<T> {
    private final Closure code
    private final Object[] args

    def FJWorker(final Object... args) {
        final def size = args.size()
        assert size > 0
        this.args = size > 1 ? args[0..-2] : ([] as Object[])
        this.code = args[-1].clone();
        this.code.delegate = this
    }

    protected void forkOffChild(Object... childArgs) {
        if (childArgs.size() != args.size()) throw new IllegalArgumentException("The forkOffChild() method requires ${args.size()} arguments, but received ${childArgs.size()} parameters.")
        forkOffChild new FJWorker(* childArgs, code)
    }

    protected T computeTask() {
        return code.call(* args)
    }
}
