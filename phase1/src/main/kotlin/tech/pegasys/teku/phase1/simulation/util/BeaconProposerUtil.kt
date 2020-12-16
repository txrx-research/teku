package tech.pegasys.teku.phase1.simulation.util

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.phase1.integration.datastructures.Attestation
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlock
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlockBody
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.ShardTransition
import tech.pegasys.teku.phase1.integration.datastructures.SignedBeaconBlock
import tech.pegasys.teku.phase1.onotole.deps.hash
import tech.pegasys.teku.phase1.onotole.phase1.MAX_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.simulation.ExecutableDataProducer
import tech.pegasys.teku.phase1.util.logDebug

fun produceBeaconBlock(
  state: BeaconState,
  slot: Slot,
  parentRoot: Root,
  attestations: List<Attestation>,
  shardTransitions: List<ShardTransition>,
  secretKeys: SecretKeyRegistry,
  spec: Phase1Spec,
  executableDataProducer: ExecutableDataProducer,
  eth1_parent_hash: Bytes32
): SignedBeaconBlock {
  val stateWithAdvancedSlot = state.copy()
  spec.process_slots(stateWithAdvancedSlot, slot)

  logDebug { "ProposerUtil: state slot is advanced to ${stateWithAdvancedSlot.slot}" }

  val proposerIndex = spec.get_beacon_proposer_index(stateWithAdvancedSlot)
  val proposerSecretKey = secretKeys[proposerIndex]
  val blockHeader = BeaconBlock(
    slot,
    proposerIndex,
    parentRoot,
    Bytes32.ZERO,
    BeaconBlockBody(),
    eth1_parent_hash
  )
  val randaoReveal = spec.get_epoch_signature(stateWithAdvancedSlot, blockHeader, proposerSecretKey)
  val (shards, winningRoots) = spec.get_shard_winning_roots(stateWithAdvancedSlot, attestations)
  val shardTransitionDict = shardTransitions.map { it.hashTreeRoot() to it }.toMap()
  val shardTransitionVector = List(MAX_SHARDS.toInt()) {
    val indexOfWinningRoot = shards.indexOf(it.toULong())
    if (indexOfWinningRoot >= 0) {
      val winningRoot = winningRoots[indexOfWinningRoot]
      shardTransitionDict[winningRoot] ?: ShardTransition()
    } else {
      ShardTransition()
    }
  }

  val epoch = spec.get_current_epoch(stateWithAdvancedSlot)
  val timestamp = spec.compute_time_at_slot(stateWithAdvancedSlot, slot)
  val randaoMix = spec.xor(spec.get_randao_mix(stateWithAdvancedSlot, epoch), hash(randaoReveal))
  val executableData = executableDataProducer.produce(eth1_parent_hash, slot, timestamp, randaoMix)

  val block = BeaconBlock(
    slot,
    proposerIndex,
    parentRoot,
    Bytes32.ZERO,
    BeaconBlockBody(
      randaoReveal,
      state.eth1_data,
      Bytes32.rightPad(Bytes.ofUnsignedLong(proposerIndex.toLong())),
      attestations,
      shardTransitionVector,
      executableData
    ),
    eth1_parent_hash
  )
  logDebug { "ProposerUtil: new block created ${block.toStringFull()}" }

  val endState = spec.state_transition(state.copy(), SignedBeaconBlock(block), false)
  val blockWithStateRoot = block.copy(state_root = endState.applyChanges().hashTreeRoot())
  val signature = spec.get_block_signature(endState, blockWithStateRoot, proposerSecretKey)

  return SignedBeaconBlock(blockWithStateRoot, signature)
}
