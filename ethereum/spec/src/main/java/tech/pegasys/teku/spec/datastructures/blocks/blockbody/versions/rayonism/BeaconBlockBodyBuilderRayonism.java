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

package tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.rayonism;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.function.Supplier;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBodyBuilder;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.common.AbstractBeaconBlockBodyBuilder;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair.SyncAggregate;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.sharding.ShardProposerSlashing;
import tech.pegasys.teku.spec.datastructures.sharding.SignedShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.type.SszSignature;
import tech.pegasys.teku.ssz.SszList;
import tech.pegasys.teku.ssz.primitive.SszBytes32;

public class BeaconBlockBodyBuilderRayonism extends AbstractBeaconBlockBodyBuilder {

  private BeaconBlockBodySchemaRayonism schema;
  private ExecutionPayload executionPayload;

  private SszList<ShardProposerSlashing> shardProposerSlashings;
  private SszList<SignedShardBlobHeader> shardHeaders;

  public BeaconBlockBodyBuilderRayonism schema(final BeaconBlockBodySchemaRayonism schema) {
    this.schema = schema;
    shardProposerSlashings = schema.getShardProposerSlashingsSchema().getDefault();
    shardHeaders = schema.getShardBlobHeadersSchema().getDefault();
    return this;
  }

  @Override
  public BeaconBlockBodyBuilder syncAggregate(final Supplier<SyncAggregate> syncAggregateSupplier) {
    // No sync aggregate in Merge
    return this;
  }

  @Override
  public BeaconBlockBodyBuilder executionPayload(
      Supplier<ExecutionPayload> executionPayloadSupplier) {
    this.executionPayload = executionPayloadSupplier.get();
    return this;
  }

  @Override
  public BeaconBlockBodyBuilderRayonism shardProposerSlashings(
      List<ShardProposerSlashing> shardProposerSlashings) {
    this.shardProposerSlashings = schema.getShardProposerSlashingsSchema()
        .createFromElements(shardProposerSlashings);
    return this;
  }

  @Override
  public BeaconBlockBodyBuilderRayonism shardHeaders(List<SignedShardBlobHeader> shardHeaders) {
    this.shardHeaders = schema.getShardBlobHeadersSchema().createFromElements(shardHeaders);
    return this;
  }

  @Override
  protected void validate() {
    super.validate();
    checkNotNull(schema, "schema must be specified");
    checkNotNull(executionPayload, "executionPayload must be specified");
  }

  public BeaconBlockBodyRayonism build() {
    validate();
    return new BeaconBlockBodyRayonism(
        schema,
        new SszSignature(randaoReveal),
        eth1Data,
        SszBytes32.of(graffiti),
        proposerSlashings,
        attesterSlashings,
        attestations,
        deposits,
        voluntaryExits,
        executionPayload,
        shardProposerSlashings,
        shardHeaders);
  }
}
