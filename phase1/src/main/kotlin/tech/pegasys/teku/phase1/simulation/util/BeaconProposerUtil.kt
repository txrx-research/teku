package tech.pegasys.teku.phase1.simulation.util

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import tech.pegasys.teku.phase1.integration.datastructures.Attestation
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlock
import tech.pegasys.teku.phase1.integration.datastructures.BeaconBlockBody
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.datastructures.ShardTransition
import tech.pegasys.teku.phase1.integration.datastructures.SignedBeaconBlock
import tech.pegasys.teku.phase1.onotole.phase1.MAX_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.phase1.Slot
import tech.pegasys.teku.phase1.onotole.phase1.get_beacon_proposer_index
import tech.pegasys.teku.phase1.onotole.phase1.get_block_signature
import tech.pegasys.teku.phase1.onotole.phase1.get_epoch_signature
import tech.pegasys.teku.phase1.onotole.phase1.get_shard_winning_roots
import tech.pegasys.teku.phase1.onotole.phase1.process_slots
import tech.pegasys.teku.phase1.onotole.phase1.state_transition
import tech.pegasys.teku.phase1.util.logDebug

fun produceBeaconBlock(
  state: BeaconState,
  slot: Slot,
  parentRoot: Root,
  attestations: List<Attestation>,
  shardTransitions: List<ShardTransition>,
  secretKeys: SecretKeyRegistry
): SignedBeaconBlock {
  val stateWithAdvancedSlot = state.copy()
  process_slots(stateWithAdvancedSlot, slot)

  logDebug("ProposerUtil: state slot is advanced to ${stateWithAdvancedSlot.slot}")

  val proposerIndex = get_beacon_proposer_index(stateWithAdvancedSlot)
  val proposerSecretKey = secretKeys[proposerIndex]
  val blockHeader = BeaconBlock(
    slot,
    proposerIndex,
    parentRoot,
    Bytes32.ZERO,
    BeaconBlockBody()
  )
  val randaoReveal = get_epoch_signature(stateWithAdvancedSlot, blockHeader, proposerSecretKey)
  val (shards, winningRoots) = get_shard_winning_roots(stateWithAdvancedSlot, attestations)
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
      shardTransitionVector
    )
  )
  logDebug("ProposerUtil: new block created $block")

  val endState = state_transition(state.copy(), SignedBeaconBlock(block), false)
  val blockWithStateRoot = block.copy(state_root = endState.applyChanges().hashTreeRoot())
  val signature = get_block_signature(state, blockWithStateRoot, proposerSecretKey)

  return SignedBeaconBlock(blockWithStateRoot, signature)
}