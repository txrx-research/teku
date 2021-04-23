/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.spec.datastructures.state.beaconstate.common;

import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconStateCache;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconStateSchema;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.MutableBeaconState;
import tech.pegasys.teku.ssz.SszContainer;
import tech.pegasys.teku.ssz.SszData;
import tech.pegasys.teku.ssz.cache.IntCache;
import tech.pegasys.teku.ssz.cache.SoftRefIntCache;
import tech.pegasys.teku.ssz.impl.SszContainerImpl;
import tech.pegasys.teku.ssz.schema.SszCompositeSchema;
import tech.pegasys.teku.ssz.schema.impl.AbstractSszContainerSchema;
import tech.pegasys.teku.ssz.tree.TreeNode;

public abstract class AbstractBeaconState<TMutable extends MutableBeaconState>
    extends SszContainerImpl implements BeaconState, BeaconStateCache {

  private final TransitionCaches transitionCaches;

  protected AbstractBeaconState(final BeaconStateSchema<?, ?> schema) {
    super(schema);
    transitionCaches = TransitionCaches.createNewEmpty();
  }

  protected AbstractBeaconState(
      SszCompositeSchema<?> type,
      TreeNode backingNode,
      IntCache<SszData> cache,
      TransitionCaches transitionCaches) {
    super(type, backingNode, cache);
    this.transitionCaches = transitionCaches;
  }

  protected AbstractBeaconState(
      AbstractSszContainerSchema<? extends SszContainer> type, TreeNode backingNode) {
    super(type, backingNode);
    transitionCaches = TransitionCaches.createNewEmpty();
  }

  @Override
  public <E1 extends Exception, E2 extends Exception, E3 extends Exception> BeaconState updated(
      Mutator<MutableBeaconState, E1, E2, E3> mutator) throws E1, E2, E3 {
    MutableBeaconState writableCopy = createWritableCopyPriv();
    mutator.mutate(writableCopy);
    return writableCopy.commitChanges();
  }

  @Override
  public TransitionCaches getTransitionCaches() {
    return transitionCaches;
  }

  @Override
  protected IntCache<SszData> createCache() {
    return new SoftRefIntCache<>(super::createCache);
  }

  protected abstract TMutable createWritableCopyPriv();
}
