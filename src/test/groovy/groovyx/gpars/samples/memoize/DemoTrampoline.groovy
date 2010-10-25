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

def fact
fact = {int n, BigInteger accumulator ->
    println 'AAAAAAAAAAAA ' + n + ":" + accumulator
    n > 1 ? fact.curry(n - 1, n * accumulator) : accumulator
}
def factorial = {int n -> fact(n, 1)}

def funB
def funA = {int num -> num==0 ? 0 : funB.curry(num-1)}
funB = {int num -> num==0 ? 0 : funA.curry(num-1)}

class Trampoline {
    def static trampoline(Closure func, Object... args) {
        trampoline func.curry(*args)
    }

    def static trampoline(Closure func) {
        def currentFunction = func
        for(;;) {
            final def result = currentFunction.call();
            if (result instanceof Closure) {
                currentFunction = result;
            } else return result
        }
        throw new IllegalStateException("The trampoline did not find a result")
    }
}

//println(Trampoline.trampoline(fact.curry(50, 1)))
//println(Trampoline.trampoline(factorial.curry(50)))
//println(Trampoline.trampoline(funA, 10))

use(Trampoline) {
    println factorial.trampoline(1000)
    println(funA.trampoline(1000))
}

//println funA(1000)
//todo test spreading arguments
//todo enable funA.trampoline(10)