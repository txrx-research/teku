package tech.pegasys.teku.phase1.eth1engine.stub

import org.apache.tuweni.rlp.RLP
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.uint64

fun encodeBlockDataWithRLP(
  number: uint64,
  parentHash: Bytes32,
  stateRoot: Bytes32,
  receiptsRoot: Bytes32,
  body: Bytes
): Bytes {
  return RLP.encodeList {
    it.writeLong(number.toLong())
    it.writeValue(parentHash)
    it.writeValue(stateRoot)
    it.writeValue(receiptsRoot)
    it.writeValue(body)
  }
}
