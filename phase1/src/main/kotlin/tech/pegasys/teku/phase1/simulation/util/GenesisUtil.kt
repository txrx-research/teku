package tech.pegasys.teku.phase1.simulation.util

import tech.pegasys.teku.bls.BLSKeyPair
import tech.pegasys.teku.datastructures.util.BeaconStateUtil
import tech.pegasys.teku.datastructures.util.MockStartBeaconStateGenerator
import tech.pegasys.teku.datastructures.util.MockStartDepositGenerator
import tech.pegasys.teku.phase1.integration.datastructures.BeaconState
import tech.pegasys.teku.phase1.integration.toUInt64
import tech.pegasys.teku.phase1.onotole.phase1.MIN_GENESIS_TIME
import tech.pegasys.teku.phase1.onotole.phase1.Phase1Spec
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32

fun getGenesisState(blsKeyPairs: List<BLSKeyPair>, spec: Phase1Spec): BeaconState {
  BeaconStateUtil.BLS_VERIFY_DEPOSIT = false
  val deposits = MockStartDepositGenerator().createDeposits(blsKeyPairs)
  val state = spec.upgrade_to_phase1(
    MockStartBeaconStateGenerator().createInitialBeaconState(
      MIN_GENESIS_TIME.toUInt64(),
      deposits
    )
  )
  return state.applyChanges()
}

fun getGenesisStore(state: BeaconState, spec: Phase1Spec, eth1_genesis_hash: Bytes32 = Bytes32()) =
  spec.get_forkchoice_store(state, eth1_genesis_hash)
