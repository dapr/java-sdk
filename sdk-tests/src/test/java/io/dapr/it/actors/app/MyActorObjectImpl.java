/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.actors.app;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorType;
import io.dapr.actors.runtime.ActorRuntimeContext;
import io.dapr.actors.runtime.Remindable;
import io.dapr.utils.TypeRef;

@ActorType(name = "MyActorObjectTest")
public class MyActorObjectImpl extends MyActorBase<MyObject> implements MyActor, Remindable<MyObject> {

  public MyActorObjectImpl(ActorRuntimeContext runtimeContext, ActorId id) {
    super(runtimeContext, id, TypeRef.get(MyObject.class));
  }

}