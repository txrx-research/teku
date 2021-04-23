/*
 * Copyright 2021 ConsenSys AG.
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

package tech.pegasys.teku.spec.logic.versions.rayonism.helpers;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.rayonism.BeaconStateRayonism;
import tech.pegasys.teku.spec.logic.common.helpers.BeaconStateAccessors;
import tech.pegasys.teku.spec.logic.common.helpers.MiscHelpers;
import tech.pegasys.teku.spec.logic.common.helpers.Predicates;

public class BeaconStateAccessorsRayonism extends BeaconStateAccessors {
  private final SpecConfigRayonism configRayonism;
  private final MiscHelpers miscHelpers;

  public BeaconStateAccessorsRayonism(
      final SpecConfig config, final Predicates predicates, final MiscHelpers miscHelpers) {
    super(config, predicates, miscHelpers);
    configRayonism =
        config
            .toVersionRayonism()
            .orElseThrow(() -> new IllegalArgumentException("Expected Rayonism spec version"));
    this.miscHelpers = miscHelpers;
  }

  // Custom accessors
  @Override
  public int getPreviousEpochAttestationCapacity(final BeaconState genericState) {
    final BeaconStateRayonism state = BeaconStateRayonism.required(genericState);
    final int absoluteMax =
        Math.toIntExact(
            state.getBeaconStateSchema().getPreviousEpochAttestationsSchema().getMaxLength());
    return absoluteMax - state.getPrevious_epoch_attestations().size();
  }

  public int getActiveShardCount(final BeaconState state, final UInt64 epoch) {
    return configRayonism.getInitialActiveShards();
  }
}
