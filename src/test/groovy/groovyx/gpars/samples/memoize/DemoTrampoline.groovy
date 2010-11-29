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

package groovyx.gpars.samples.memoize

final class TrampolineClosure extends Closure {
    final Closure original

    def TrampolineClosure(final original) {
        super(original.owner, original.delegate);
        this.original = original;
    }

    def int getMaximumNumberOfParameters() {
        return original.maximumNumberOfParameters
    }

    def Class[] getParameterTypes() {
        return original.parameterTypes
    }

    public def call() {
        return loop(getOriginal().call())
    }

    public def call(Object arguments) {
        return loop(getOriginal().call(arguments))
    }

    public def call(Object... args) {
        return loop(getOriginal().call(* args))
    }

    private def loop(lastResult) {
        def result = lastResult

        for (;;) {
            if (result instanceof TrampolineClosure) {
                def currentFunction = result.getOriginal();
                result = currentFunction.call();
            } else return result
        }
    }

    def trampoline(Object... args) {
        return new TrampolineClosure(getOriginal().curry(* args))
    }

    def trampoline() {
        return this
    }
}

class Trampoline {
    def static trampoline(Closure func, Object... args) {
        if (func instanceof TrampolineClosure) {
            return new TrampolineClosure(func.getOriginal().curry(* args))
        } else {
            return new TrampolineClosure(func.curry(* args))
        }
    }

    def static trampoline(Closure func) {
        if (func instanceof TrampolineClosure) {
            return func
        } else {
            return new TrampolineClosure(func)
        }
    }
}

use(Trampoline) {
    def fact
    fact = {int n, BigInteger accumulator ->
//        println 'AAAAAAAAAAAA ' + n + ":" + accumulator
        n > 1 ? fact.trampoline(n - 1, n * accumulator) : accumulator
    }.trampoline()
    def factorial = {int n -> fact(n, 1)}

    def funB
    def funA = {int num -> num == 0 ? 0 : funB.trampoline(num - 1)}
    funB = {int num -> num == 0 ? 0 : funA.trampoline(num - 1)}

    println factorial(1)
    println factorial(2)
    println factorial(3)
    println factorial(6)
    println factorial(10)
    println factorial(1000)
    println(funA.trampoline()(1000))
    println(funA.trampoline(1000)())

    def funD
    def funC = {int num -> num == 0 ? 0 : funD.trampoline(num - 1)}.trampoline()
    funD = {int num -> num == 0 ? 0 : funC.trampoline(num - 1)}.trampoline()

    println(funC(1000))
    println(funD(1000))
}

//println funA(1000)