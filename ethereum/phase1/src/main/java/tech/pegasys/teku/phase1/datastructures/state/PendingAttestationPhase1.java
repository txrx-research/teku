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

package tech.pegasys.teku.phase1.datastructures.state;

import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ssz.SSZ;
import tech.pegasys.teku.datastructures.Copyable;
import tech.pegasys.teku.phase1.datastructures.operations.AttestationDataPhase1;
import tech.pegasys.teku.ssz.SSZTypes.Bitlist;
import tech.pegasys.teku.ssz.SSZTypes.SSZContainer;
import tech.pegasys.teku.ssz.backing.tree.TreeNode;
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes;
import tech.pegasys.teku.ssz.backing.type.ContainerViewType;
import tech.pegasys.teku.ssz.backing.type.ListViewType;
import tech.pegasys.teku.ssz.backing.view.AbstractImmutableContainer;
import tech.pegasys.teku.ssz.backing.view.BasicViews.BitView;
import tech.pegasys.teku.ssz.backing.view.BasicViews.UInt64View;
import tech.pegasys.teku.ssz.backing.view.ViewUtils;
import tech.pegasys.teku.ssz.sos.SimpleOffsetSerializable;
import tech.pegasys.teku.util.config.Constants;
import tech.pegasys.teku.util.hashtree.Merkleizable;

public class PendingAttestationPhase1 extends AbstractImmutableContainer
    implements Copyable<PendingAttestationPhase1>,
        Merkleizable,
        SimpleOffsetSerializable,
        SSZContainer {

  // The number of SimpleSerialize basic types in this SSZ Container/POJO.
  public static final int SSZ_FIELD_COUNT = 4;

  public static final ContainerViewType<PendingAttestationPhase1> TYPE =
      new ContainerViewType<>(
          List.of(
              new ListViewType<BitView>(
                  BasicViewTypes.BIT_TYPE, Constants.MAX_VALIDATORS_PER_COMMITTEE),
              AttestationDataPhase1.TYPE,
              BasicViewTypes.UINT64_TYPE,
              BasicViewTypes.UINT64_TYPE,
              BasicViewTypes.BIT_TYPE),
          PendingAttestationPhase1::new);

  @SuppressWarnings("unused")
  private final Bitlist aggregation_bits =
      new Bitlist(
          0,
          Constants
              .MAX_VALIDATORS_PER_COMMITTEE); // bitlist bounded by MAX_VALIDATORS_PER_COMMITTEE

  @SuppressWarnings("unused")
  private final AttestationDataPhase1 data = null;

  @SuppressWarnings("unused")
  private final UnsignedLong inclusion_delay = null;

  @SuppressWarnings("unused")
  private final UnsignedLong proposer_index = null;

  @SuppressWarnings("unused")
  private final Boolean crosslink_success = null;

  private PendingAttestationPhase1(
      ContainerViewType<PendingAttestationPhase1> type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public PendingAttestationPhase1(
      Bitlist aggregation_bitfield,
      AttestationDataPhase1 data,
      UnsignedLong inclusion_delay,
      UnsignedLong proposer_index,
      Boolean crosslink_success) {
    super(
        TYPE,
        ViewUtils.createBitlistView(aggregation_bitfield),
        data,
        new UInt64View(inclusion_delay),
        new UInt64View(proposer_index),
        new BitView(crosslink_success));
  }

  public PendingAttestationPhase1() {
    super(TYPE);
  }

  public PendingAttestationPhase1(PendingAttestationPhase1 pendingAttestation) {
    super(TYPE, pendingAttestation.getBackingNode());
  }

  @Override
  public PendingAttestationPhase1 copy() {
    return new PendingAttestationPhase1(this);
  }

  @Override
  public int getSSZFieldCount() {
    return getData().getSSZFieldCount() + SSZ_FIELD_COUNT;
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    List<Bytes> fixedPartsList = new ArrayList<>();
    fixedPartsList.addAll(List.of(Bytes.EMPTY));
    fixedPartsList.addAll(getData().get_fixed_parts());
    fixedPartsList.addAll(
        List.of(
            SSZ.encodeUInt64(getInclusion_delay().longValue()),
            SSZ.encodeUInt64(getProposer_index().longValue()),
            SSZ.encodeBoolean(getCrosslink_success())));
    return fixedPartsList;
  }

  @Override
  public List<Bytes> get_variable_parts() {
    List<Bytes> variablePartsList = new ArrayList<>();
    // TODO The below lines are a hack while Tuweni SSZ/SOS is being upgraded. To be uncommented
    // once we shift from Bytes to a real bitlist type.
    // Bytes serialized_aggregation_bits =
    // Bytes.fromHexString("0x01").shiftLeft(aggregation_bits.bitLength()).or(aggregation_bits);
    // variablePartsList.addAll(List.of(serialized_aggregation_bits));
    variablePartsList.addAll(List.of(getAggregation_bits().serialize()));
    variablePartsList.addAll(Collections.nCopies(getData().getSSZFieldCount(), Bytes.EMPTY));
    variablePartsList.addAll(List.of(Bytes.EMPTY, Bytes.EMPTY, Bytes.EMPTY));
    return variablePartsList;
  }

  @Override
  public String toString() {
    return "PendingAttestation{"
        + "aggregation_bits="
        + getAggregation_bits()
        + ", data="
        + getData()
        + ", inclusion_delay="
        + getInclusion_delay()
        + ", proposer_index="
        + getProposer_index()
        + ", crosslink_success="
        + getCrosslink_success()
        + '}';
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public Bitlist getAggregation_bits() {
    return ViewUtils.getBitlist(getAny(0));
  }

  public AttestationDataPhase1 getData() {
    return getAny(1);
  }

  public UnsignedLong getInclusion_delay() {
    return ((UInt64View) get(2)).get();
  }

  public UnsignedLong getProposer_index() {
    return ((UInt64View) get(3)).get();
  }

  public Boolean getCrosslink_success() {
    return ((BitView) get(4)).get();
  }

  @Override
  public Bytes32 hash_tree_root() {
    return hashTreeRoot();
  }
}