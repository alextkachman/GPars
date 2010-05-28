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

package groovyx.gpars.active

import groovyx.gpars.actor.Actor
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 * Date: May 24, 2010
 */
class ActiveObjects {
    static void activate(def original, boolean asynchronousInvocation = false) {
        original.setMetaClass(new ActiveMetaClass(original.getMetaClass(), asynchronousInvocation))
    }
}

abstract class ActiveObjectMessage {
    abstract def execute()
}

final class MethodMessage extends ActiveObjectMessage {
    final Object object
    final String methodName
    final Object arguments

    def MethodMessage(final object, final methodName, final arguments) {
        this.object = object;
        this.methodName = methodName;
        this.arguments = arguments;
    }

    Object execute() {
        return object.invokeMethod(methodName, arguments)
    }
}
final class GetPropertyMessage extends ActiveObjectMessage {
    final Object object
    final String propertyName

    def GetPropertyMessage(final object, final propertyName) {
        this.object = object;
        this.propertyName = propertyName;
    }

    Object execute() {
        return object."$propertyName"
    }
}

final class SetPropertyMessage extends ActiveObjectMessage {
    final Object object
    final String propertyName
    final Object newValue

    def SetPropertyMessage(final object, final propertyName, final newValue) {
        this.object = object;
        this.propertyName = propertyName;
        this.newValue = newValue;
    }

    Object execute() {
        object."$propertyName" = newValue
    }
}

class ActiveMetaClass extends DelegatingMetaClass {
    private static final PGroup activeObjectGroup = new NonDaemonPGroup()

    private final boolean asynchronousInvocation

    private List errors

    private Actor actor = activeObjectGroup.reactor {
        //todo remove
        println 'AAAAAAAAAAAAAAAAAAAAAA ' + it.dump()
        try {
            return it.execute()
        } catch (Exception e) {
            registerError e
            null
        }
    }

    def ActiveMetaClass(final MetaClass delegate, final boolean asynchronousInvocation) {
        super(delegate);
        this.asynchronousInvocation = asynchronousInvocation
    }

    @Override
    public Object invokeMethod(Object object, String methodName, Object arguments) {
        if (actor.isActorThread() || methodName in ['hasActiveErrors', 'registerError']) return super.invokeMethod(object, methodName, arguments);
        else {
            submitMethodToActor(methodName, arguments, object)
        }
    }

    @Override
    public Object invokeMethod(Object object, String methodName, Object[] arguments) {
        if (actor.isActorThread() || methodName in ['hasActiveErrors', 'registerError']) return super.invokeMethod(object, methodName, arguments);
        else {
            submitMethodToActor(methodName, arguments, object)
        }
    }

    private def submitMethodToActor(String methodName, arguments, object) {
        final MetaMethod method = getMetaMethod(methodName, arguments)
        if (method?.returnType == void) {
            actor.send new MethodMessage(object, methodName, arguments)
        } else if (asynchronousInvocation) {
            final DataFlowVariable result = new DataFlowVariable()
            actor.sendAndContinue(new MethodMessage(object, methodName, arguments), {result << it})
            return result
        } else actor.sendAndWait new MethodMessage(object, methodName, arguments)
    }

    @Override
    public Object getProperty(Object object, String property) {
        if (actor.isActorThread() || property in ['activeErrors']) return super.getProperty(object, property)
        else {
            actor.sendAndWait(new GetPropertyMessage(object, property))
        }
    }

    @Override
    public void setProperty(Object object, String property, Object newValue) {
        if (actor.isActorThread()) super.setProperty(object, property, newValue)
        else {
            actor.send(new SetPropertyMessage(object, property, newValue))
        }
    }

    //todo exception handling in active objects
    //todo should not synchronize on the meta-class
    //todo javadoc
    //todo demos
    /**
     * Adds the exception to the list of thrown exceptions
     *
     * @param e The exception to store
     */
    private synchronized void registerError(final Exception e) {
        if (errors == null) errors = new ArrayList<Exception>();
        errors.add(e);
    }

    /**
     * Retrieves a list of exception thrown within the agent's body.
     * Clears the exception history
     *
     * @return A detached collection of exception that have occurred in the agent's body
     */
    public def getActiveErrors = {->
        if (errors == null) return Collections.emptyList();
        try {
            return errors;
        } finally {
            errors = null;
        }
    }

    public def hasActiveErrors = {-> errors?.size() > 0}
}
