package tech.pegasys.teku.phase1.deps

import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.ssz.SSZ
import tech.pegasys.teku.bls.BLS
import tech.pegasys.teku.bls.BLSPublicKey
import tech.pegasys.teku.bls.BLSSecretKey
import tech.pegasys.teku.bls.BLSSignature
import tech.pegasys.teku.phase1.pylib.pyint
import tech.pegasys.teku.phase1.ssz.Bytes
import tech.pegasys.teku.phase1.ssz.Bytes32
import tech.pegasys.teku.phase1.ssz.Bytes48
import tech.pegasys.teku.phase1.ssz.Bytes96
import tech.pegasys.teku.phase1.ssz.SSZComposite
import tech.pegasys.teku.phase1.ssz.boolean
import tech.pegasys.teku.phase1.ssz.uint64
import tech.pegasys.teku.phase1.ssz.uint8
import tech.pegasys.teku.util.hashtree.HashTreeUtil

fun hash_tree_root(a: Any): Bytes32 {
  return when (a) {
    is SSZComposite -> a.hash_tree_root()
    is boolean -> HashTreeUtil.hash_tree_root(HashTreeUtil.SSZTypes.BASIC, SSZ.encodeBoolean(a))
    is uint8 -> HashTreeUtil.hash_tree_root(HashTreeUtil.SSZTypes.BASIC, SSZ.encodeUInt8(a.toInt()))
    is uint64 -> HashTreeUtil.hash_tree_root(HashTreeUtil.SSZTypes.BASIC, SSZ.encodeUInt64(a.toLong()))
    is Bytes -> HashTreeUtil.hash_tree_root(HashTreeUtil.SSZTypes.BASIC, a)
    else -> throw IllegalArgumentException("Unsupported SSZ type: " + a::class.qualifiedName)
  }
}

fun hash(a: Bytes): Bytes32 = Hash.sha2_256(a)

data class FQ2(val coeffs: Pair<pyint, pyint>)

object bls {
  fun Sign(privkey: pyint, message: Bytes): Bytes96 {
    return BLS.sign(BLSSecretKey.fromBytes(Bytes.wrap(privkey.value.toByteArray())), message).toBytes()
  }

  fun Verify(pubkey: Bytes48, message: Bytes, signature: Bytes96): Boolean {
    return BLS.verify(BLSPublicKey.fromBytes(pubkey), message, BLSSignature.fromBytes(signature))
  }

  fun Aggregate(signatures: Collection<Bytes96>): Bytes96 {
    return BLS.aggregate(signatures.map { s -> BLSSignature.fromBytes(s) }).toBytes()
  }

  fun FastAggregateVerify(pubkeys: Collection<Bytes48>, root: Bytes, signature: Bytes96): Boolean {
    return BLS.fastAggregateVerify(
        pubkeys.map { k -> BLSPublicKey.fromBytes(k) }.toList(),
        root,
        BLSSignature.fromBytes(signature)
    )
  }

  fun AggregateVerify(pairs: List<Pair<Bytes48, Bytes>>, signature: Bytes96): boolean {
    return BLS.aggregateVerify(
        pairs.map { p -> BLSPublicKey.fromBytes(p.first) }.toList(),
        pairs.map { p -> p.second }.toList(),
        BLSSignature.fromBytes(signature)
    )
  }

  fun signature_to_G2(signature: Bytes96): Triple<FQ2, FQ2, FQ2> {
    TODO("Not yet implemented")
  }
}
