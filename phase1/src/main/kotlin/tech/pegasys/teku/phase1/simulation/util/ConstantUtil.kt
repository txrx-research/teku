package tech.pegasys.teku.phase1.simulation.util

import tech.pegasys.teku.phase1.eth1shard.ETH1_SHARD_NUMBER
import tech.pegasys.teku.phase1.onotole.phase1.EPOCHS_PER_HISTORICAL_VECTOR
import tech.pegasys.teku.phase1.onotole.phase1.EPOCHS_PER_SLASHINGS_VECTOR
import tech.pegasys.teku.phase1.onotole.phase1.GENESIS_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.INITIAL_ACTIVE_SHARDS
import tech.pegasys.teku.phase1.onotole.phase1.MIN_GENESIS_ACTIVE_VALIDATOR_COUNT
import tech.pegasys.teku.phase1.onotole.phase1.MIN_GENESIS_TIME
import tech.pegasys.teku.phase1.onotole.phase1.PHASE_1_FORK_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.SECONDS_PER_SLOT
import tech.pegasys.teku.phase1.onotole.phase1.SHARD_COMMITTEE_PERIOD
import tech.pegasys.teku.phase1.onotole.phase1.SLOTS_PER_EPOCH
import tech.pegasys.teku.phase1.onotole.phase1.SLOTS_PER_HISTORICAL_ROOT
import tech.pegasys.teku.util.config.Constants

fun setConstants(constantSetName: String, config: ConstantsConfig) {
  // from the base set
  Constants.setConstants(constantSetName)
  SLOTS_PER_EPOCH = Constants.SLOTS_PER_EPOCH.toULong()
  SLOTS_PER_HISTORICAL_ROOT = Constants.SLOTS_PER_HISTORICAL_ROOT.toULong()
  EPOCHS_PER_HISTORICAL_VECTOR = Constants.EPOCHS_PER_HISTORICAL_VECTOR.toULong()
  EPOCHS_PER_SLASHINGS_VECTOR = Constants.EPOCHS_PER_SLASHINGS_VECTOR.toULong()

  // configurable
  INITIAL_ACTIVE_SHARDS = config.initialActiveShards
  ETH1_SHARD_NUMBER = config.eth1ShardNumber
  MIN_GENESIS_ACTIVE_VALIDATOR_COUNT = config.minGenesisActiveValidatorCount

  // hardcoded set
  SHARD_COMMITTEE_PERIOD = 16uL
  MIN_GENESIS_TIME = 0uL
  PHASE_1_FORK_SLOT = GENESIS_SLOT

  SECONDS_PER_SLOT = 3uL
}

data class ConstantsConfig(
  val initialActiveShards: ULong,
  val eth1ShardNumber: ULong,
  val minGenesisActiveValidatorCount: ULong
)