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

package groovyx.gpars.pa;

import groovy.lang.Closure;
import jsr166y.forkjoin.Ops;

/**
 * A PA mapper built around a closure
 *
 * @author Vaclav Pech
 */
public final class ClosureMapper implements Ops.Mapper<Object, Object> {
    private final Closure code;

    public ClosureMapper(final Closure code) {
        this.code = code;
    }

    @Override
    public Object map(final Object o) {
        return code.call(o);
    }
}
