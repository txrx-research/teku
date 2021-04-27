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

import java.util.Optional;
import java.util.function.Consumer;
import tech.pegasys.teku.spec.config.SpecConfig;
import tech.pegasys.teku.spec.config.SpecConfigRayonism;
import tech.pegasys.teku.spec.datastructures.blocks.Eth1Data;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBodyBuilder;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBodySchema;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.common.BlockBodyFields;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.operations.Attestation;
import tech.pegasys.teku.spec.datastructures.operations.AttesterSlashing;
import tech.pegasys.teku.spec.datastructures.operations.Deposit;
import tech.pegasys.teku.spec.datastructures.operations.ProposerSlashing;
import tech.pegasys.teku.spec.datastructures.operations.SignedVoluntaryExit;
import tech.pegasys.teku.spec.datastructures.sharding.ShardProposerSlashing;
import tech.pegasys.teku.spec.datastructures.sharding.SignedShardBlobHeader;
import tech.pegasys.teku.spec.datastructures.type.SszSignature;
import tech.pegasys.teku.spec.datastructures.type.SszSignatureSchema;
import tech.pegasys.teku.ssz.SszList;
import tech.pegasys.teku.ssz.containers.ContainerSchema11;
import tech.pegasys.teku.ssz.primitive.SszBytes32;
import tech.pegasys.teku.ssz.schema.SszListSchema;
import tech.pegasys.teku.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.tree.TreeNode;

public class BeaconBlockBodySchemaRayonism
    extends ContainerSchema11<
        BeaconBlockBodyRayonism,
        SszSignature,
        Eth1Data,
        SszBytes32,
        SszList<ProposerSlashing>,
        SszList<AttesterSlashing>,
        SszList<Attestation>,
        SszList<Deposit>,
        SszList<SignedVoluntaryExit>,
        ExecutionPayload,
        SszList<ShardProposerSlashing>,
        SszList<SignedShardBlobHeader>>
    implements BeaconBlockBodySchema<BeaconBlockBodyRayonism> {

  private BeaconBlockBodySchemaRayonism(
      NamedSchema<SszSignature> randaoRevealSchema,
      NamedSchema<Eth1Data> eth1DataSchema,
      NamedSchema<SszBytes32> graffitiSchema,
      NamedSchema<SszList<ProposerSlashing>> proposerSlashingsSchema,
      NamedSchema<SszList<AttesterSlashing>> attesterSlashingsSchema,
      NamedSchema<SszList<Attestation>> attestationsSchema,
      NamedSchema<SszList<Deposit>> depositsSchema,
      NamedSchema<SszList<SignedVoluntaryExit>> voluntaryExitsSchema,
      NamedSchema<ExecutionPayload> executionPayloadSchema,
      NamedSchema<SszList<ShardProposerSlashing>> shardProposerSlashingsSchema,
      NamedSchema<SszList<SignedShardBlobHeader>> shardHeadersSchema) {
    super(
        "BeaconBlockBody",
        randaoRevealSchema,
        eth1DataSchema,
        graffitiSchema,
        proposerSlashingsSchema,
        attesterSlashingsSchema,
        attestationsSchema,
        depositsSchema,
        voluntaryExitsSchema,
        executionPayloadSchema,
        shardProposerSlashingsSchema,
        shardHeadersSchema);
  }

  public static BeaconBlockBodySchemaRayonism create(final SpecConfig specConfig) {
    return SpecConfigRayonism.required(
        specConfig,
        config ->
            create(
                config.getMaxProposerSlashings(),
                config.getMaxAttesterSlashings(),
                config.getMaxAttestations(),
                config.getMaxDeposits(),
                config.getMaxVoluntaryExits(),
                config.getMaxShardProposerSlashings(),
                config.getMaxShards(),
                config.getMaxShardHeadersPerShard()));
  }

  private static BeaconBlockBodySchemaRayonism create(
      final long maxProposerSlashings,
      final long maxAttesterSlashings,
      final long maxAttestations,
      final long maxDeposits,
      final long maxVoluntaryExits,
      final long maxShardProposerSlashings,
      final long maxShards,
      final long maxShardHeadersPerShard) {

    return new BeaconBlockBodySchemaRayonism(
        namedSchema(BlockBodyFields.RANDAO_REVEAL.name(), SszSignatureSchema.INSTANCE),
        namedSchema(BlockBodyFields.ETH1_DATA.name(), Eth1Data.SSZ_SCHEMA),
        namedSchema(BlockBodyFields.GRAFFITI.name(), SszPrimitiveSchemas.BYTES32_SCHEMA),
        namedSchema(
            BlockBodyFields.PROPOSER_SLASHINGS.name(),
            SszListSchema.create(ProposerSlashing.SSZ_SCHEMA, maxProposerSlashings)),
        namedSchema(
            BlockBodyFields.ATTESTER_SLASHINGS.name(),
            SszListSchema.create(AttesterSlashing.SSZ_SCHEMA, maxAttesterSlashings)),
        namedSchema(
            BlockBodyFields.ATTESTATIONS.name(),
            SszListSchema.create(Attestation.SSZ_SCHEMA, maxAttestations)),
        namedSchema(
            BlockBodyFields.DEPOSITS.name(), SszListSchema.create(Deposit.SSZ_SCHEMA, maxDeposits)),
        namedSchema(
            BlockBodyFields.VOLUNTARY_EXITS.name(),
            SszListSchema.create(SignedVoluntaryExit.SSZ_SCHEMA, maxVoluntaryExits)),
        namedSchema(BlockBodyFields.EXECUTION_PAYLOAD.name(), ExecutionPayload.SSZ_SCHEMA),
        namedSchema(
            BlockBodyFields.SHARD_PROPOSER_SLASHINGS.name(),
            SszListSchema.create(ShardProposerSlashing.SSZ_SCHEMA, maxShardProposerSlashings)),
        namedSchema(
            BlockBodyFields.SHARD_HEADERS.name(),
            SszListSchema.create(
                SignedShardBlobHeader.SSZ_SCHEMA, maxShards * maxShardHeadersPerShard)));
  }

  @Override
  public BeaconBlockBodyRayonism createBlockBody(
      final Consumer<BeaconBlockBodyBuilder> builderConsumer) {
    final BeaconBlockBodyBuilderRayonism builder =
        new BeaconBlockBodyBuilderRayonism().schema(this);
    builderConsumer.accept(builder);
    return builder.build();
  }

  @Override
  public BeaconBlockBodyRayonism createEmpty() {
    return new BeaconBlockBodyRayonism(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public SszListSchema<ProposerSlashing, ?> getProposerSlashingsSchema() {
    return (SszListSchema<ProposerSlashing, ?>) getFieldSchema3();
  }

  @SuppressWarnings("unchecked")
  @Override
  public SszListSchema<AttesterSlashing, ?> getAttesterSlashingsSchema() {
    return (SszListSchema<AttesterSlashing, ?>) getFieldSchema4();
  }

  @SuppressWarnings("unchecked")
  @Override
  public SszListSchema<Attestation, ?> getAttestationsSchema() {
    return (SszListSchema<Attestation, ?>) getFieldSchema5();
  }

  @SuppressWarnings("unchecked")
  @Override
  public SszListSchema<Deposit, ?> getDepositsSchema() {
    return (SszListSchema<Deposit, ?>) getFieldSchema6();
  }

  @SuppressWarnings("unchecked")
  @Override
  public SszListSchema<SignedVoluntaryExit, ?> getVoluntaryExitsSchema() {
    return (SszListSchema<SignedVoluntaryExit, ?>) getFieldSchema7();
  }

  @SuppressWarnings("unchecked")
  public SszListSchema<ShardProposerSlashing, ?> getShardProposerSlashingsSchema() {
    return (SszListSchema<ShardProposerSlashing, ?>) getFieldSchema9();
  }

  @SuppressWarnings("unchecked")
  public SszListSchema<SignedShardBlobHeader, ?> getShardBlobHeadersSchema() {
    return (SszListSchema<SignedShardBlobHeader, ?>) getFieldSchema10();
  }

  @Override
  public BeaconBlockBodyRayonism createFromBackingNode(TreeNode node) {
    return new BeaconBlockBodyRayonism(this, node);
  }

  @Override
  public Optional<BeaconBlockBodySchemaRayonism> toVersionRayonism() {
    return Optional.of(this);
  }
}