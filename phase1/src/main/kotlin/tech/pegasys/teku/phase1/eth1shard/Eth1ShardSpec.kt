package tech.pegasys.teku.phase1.eth1shard

import tech.pegasys.teku.phase1.eth1client.Eth1BlockData
import tech.pegasys.teku.phase1.eth1client.Eth1EngineClient
import tech.pegasys.teku.phase1.integration.datastructures.ShardStore
import tech.pegasys.teku.phase1.integration.datastructures.SignedShardBlock
import tech.pegasys.teku.phase1.integration.datastructures.Store
import tech.pegasys.teku.phase1.onotole.phase1.Shard
import tech.pegasys.teku.phase1.onotole.phase1.on_shard_block

val ETH1_SHARD_NUMBER = Shard(0uL)

fun on_eth1_shard_block(store: Store, shard_store: ShardStore, signed_shard_block: SignedShardBlock, eth1_engine: Eth1EngineClient): Unit {
  val eth1BlockData = Eth1BlockData(signed_shard_block.message.body.toBytes())
  val ret = eth1_engine.eth2_insertBlock(eth1BlockData.blockRLP)
  if (ret.result != true) {
    throw IllegalStateException("Failed to import $eth1BlockData, reason: ${ret.reason}")
  }
  on_shard_block(store, shard_store, signed_shard_block)
}