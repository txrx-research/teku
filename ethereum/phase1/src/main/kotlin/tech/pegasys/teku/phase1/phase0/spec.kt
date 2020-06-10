package tech.pegasys.teku.phase1.phase0

import tech.pegasys.teku.phase1.deps.bls
import tech.pegasys.teku.phase1.deps.hash
import tech.pegasys.teku.phase1.deps.hash_tree_root
import tech.pegasys.teku.phase1.pylib.Tuple2
import tech.pegasys.teku.phase1.pylib.all
import tech.pegasys.teku.phase1.pylib.any
import tech.pegasys.teku.phase1.pylib.append
import tech.pegasys.teku.phase1.pylib.bit_length
import tech.pegasys.teku.phase1.pylib.contains
import tech.pegasys.teku.phase1.pylib.count
import tech.pegasys.teku.phase1.pylib.enumerate
import tech.pegasys.teku.phase1.pylib.filter
import tech.pegasys.teku.phase1.pylib.from_bytes
import tech.pegasys.teku.phase1.pylib.get
import tech.pegasys.teku.phase1.pylib.index
import tech.pegasys.teku.phase1.pylib.intersection
import tech.pegasys.teku.phase1.pylib.keys
import tech.pegasys.teku.phase1.pylib.len
import tech.pegasys.teku.phase1.pylib.list
import tech.pegasys.teku.phase1.pylib.map
import tech.pegasys.teku.phase1.pylib.max
import tech.pegasys.teku.phase1.pylib.min
import tech.pegasys.teku.phase1.pylib.plus
import tech.pegasys.teku.phase1.pylib.pow
import tech.pegasys.teku.phase1.pylib.pybool
import tech.pegasys.teku.phase1.pylib.pybytes
import tech.pegasys.teku.phase1.pylib.pyint
import tech.pegasys.teku.phase1.pylib.range
import tech.pegasys.teku.phase1.pylib.set
import tech.pegasys.teku.phase1.pylib.shr
import tech.pegasys.teku.phase1.pylib.slice
import tech.pegasys.teku.phase1.pylib.sorted
import tech.pegasys.teku.phase1.pylib.sum
import tech.pegasys.teku.phase1.pylib.to_bytes
import tech.pegasys.teku.phase1.pylib.unaryMinus
import tech.pegasys.teku.phase1.pylib.updateSlice
import tech.pegasys.teku.phase1.pylib.zip
import tech.pegasys.teku.phase1.ssz.Bytes32
import tech.pegasys.teku.phase1.ssz.CBitlist
import tech.pegasys.teku.phase1.ssz.CDict
import tech.pegasys.teku.phase1.ssz.SSZObject
import tech.pegasys.teku.phase1.ssz.Sequence
import tech.pegasys.teku.phase1.ssz.uint64
import kotlin.experimental.xor

fun ceillog2(x: uint64): pyint {
  return (x - 1uL).bit_length()
}

/*
    Return the largest integer ``x`` such that ``x**2 <= n``.
    */
fun integer_squareroot(n: uint64): uint64 {
  var x = n
  var y = ((x + 1uL) / 2uL)
  while ((y < x)) {
    x = y
    y = ((x + (n / x)) / 2uL)
  }
  return x
}

/*
    Return the exclusive-or of two 32-byte strings.
    */
fun xor(bytes_1: Bytes32, bytes_2: Bytes32): Bytes32 {
  return Bytes32(zip(bytes_1, bytes_2).map({ (a, b) -> a xor b }))
}

/*
    Return the ``length``-byte serialization of ``n`` in ``ENDIANNESS``-endian.
    */
fun int_to_bytes(n: uint64, length: uint64): pybytes {
  return n.to_bytes(length, ENDIANNESS)
}

/*
    Return the integer deserialization of ``data`` interpreted as ``ENDIANNESS``-endian.
    */
fun bytes_to_int(data: pybytes): uint64 {
  return from_bytes(data, ENDIANNESS)
}

/*
    Check if ``validator`` is active.
    */
fun is_active_validator(validator: Validator, epoch: Epoch): pybool {
  return (validator.activation_epoch <= epoch) && (epoch < validator.exit_epoch)
}

/*
    Check if ``validator`` is eligible to be placed into the activation queue.
    */
fun is_eligible_for_activation_queue(validator: Validator): pybool {
  return (validator.activation_eligibility_epoch == FAR_FUTURE_EPOCH) && (validator.effective_balance == MAX_EFFECTIVE_BALANCE)
}

/*
    Check if ``validator`` is eligible for activation.
    */
fun is_eligible_for_activation(state: BeaconState, validator: Validator): pybool {
  return (validator.activation_eligibility_epoch <= state.finalized_checkpoint.epoch) && (validator.activation_epoch == FAR_FUTURE_EPOCH)
}

/*
    Check if ``validator`` is slashable.
    */
fun is_slashable_validator(validator: Validator, epoch: Epoch): pybool {
  return !(validator.slashed) && (validator.activation_epoch <= epoch) && (epoch < validator.withdrawable_epoch)
}

/*
    Check if ``data_1`` and ``data_2`` are slashable according to Casper FFG rules.
    */
fun is_slashable_attestation_data(data_1: AttestationData, data_2: AttestationData): pybool {
  return (data_1 != data_2) && (data_1.target.epoch == data_2.target.epoch) || (data_1.source.epoch < data_2.source.epoch) && (data_2.target.epoch < data_1.target.epoch)
}

/*
    Check if ``indexed_attestation`` has sorted and unique indices and a valid aggregate signature.
    */
fun is_valid_indexed_attestation(state: BeaconState, indexed_attestation: IndexedAttestation): pybool {
  var indices = indexed_attestation.attesting_indices
  if (!((indices == sorted(set(indices))))) {
    return false
  } else {
  }
  var pubkeys = indices.map { i -> state.validators[i].pubkey }.toMutableList()
  var domain = get_domain(state, DOMAIN_BEACON_ATTESTER, indexed_attestation.data.target.epoch)
  var signing_root = compute_signing_root(indexed_attestation.data, domain)
  return bls.FastAggregateVerify(pubkeys, signing_root, indexed_attestation.signature)
}

/*
    Check if ``leaf`` at ``index`` verifies against the Merkle ``root`` and ``branch``.
    */
fun is_valid_merkle_branch(leaf: Bytes32, branch: Sequence<Bytes32>, depth: uint64, index: uint64, root: Root): pybool {
  var value = leaf
  for (i in range(depth)) {
    if (pybool(((index / (2uL.pow(i))) % 2uL))) {
      value = hash((branch[i] + value))
    } else {
      value = hash((value + branch[i]))
    }
  }
  return (value == root)
}

/*
    Return the shuffled validator index corresponding to ``seed`` (and ``index_count``).
    */
fun compute_shuffled_index(index: ValidatorIndex, index_count: uint64, seed: Bytes32): ValidatorIndex {
  assert((index < index_count))
  var index_ = index
  for (current_round in range(SHUFFLE_ROUND_COUNT)) {
    var pivot = (bytes_to_int(hash((seed + int_to_bytes(current_round, length = 1uL))).slice(0uL, 8uL)) % index_count)
    var flip = ValidatorIndex((((pivot + index_count) - index_) % index_count))
    var position = max(index_, flip)
    var source = hash(((seed + int_to_bytes(current_round, length = 1uL)) + int_to_bytes((position / 256uL), length = 4uL)))
    var byte = source[((position % 256uL) / 8uL)]
    var bit = ((byte shr (position % 8uL)) % 2uL)
    index_ = if (pybool(bit)) flip else index_
  }
  return ValidatorIndex(index_)
}

/*
    Return from ``indices`` a random index sampled by effective balance.
    */
fun compute_proposer_index(state: BeaconState, indices: Sequence<ValidatorIndex>, seed: Bytes32): ValidatorIndex {
  assert((len(indices) > 0uL))
  var MAX_RANDOM_BYTE = ((2uL.pow(8uL)) - 1uL)
  var i = 0uL
  while (true) {
    var candidate_index = indices[compute_shuffled_index(ValidatorIndex((i % len(indices))), len(indices), seed)]
    var random_byte = hash((seed + int_to_bytes((i / 32uL), length = 8uL)))[(i % 32uL)]
    var effective_balance = state.validators[candidate_index].effective_balance
    if (((effective_balance * MAX_RANDOM_BYTE) >= (MAX_EFFECTIVE_BALANCE * random_byte.toUInt()))) {
      return ValidatorIndex(candidate_index)
    } else {
    }
    i += 1uL
  }
}

/*
    Return the committee corresponding to ``indices``, ``seed``, ``index``, and committee ``count``.
    */
fun compute_committee(indices: Sequence<ValidatorIndex>, seed: Bytes32, index: uint64, count: uint64): Sequence<ValidatorIndex> {
  var start = ((len(indices) * index) / count)
  var end = ((len(indices) * (index + 1uL)) / count)
  return range(start, end).map { i -> indices[compute_shuffled_index(ValidatorIndex(i), len(indices), seed)] }.toMutableList()
}

/*
    Return the epoch number at ``slot``.
    */
fun compute_epoch_at_slot(slot: Slot): Epoch {
  return Epoch((slot / SLOTS_PER_EPOCH))
}

/*
    Return the start slot of ``epoch``.
    */
fun compute_start_slot_at_epoch(epoch: Epoch): Slot {
  return Slot((epoch * SLOTS_PER_EPOCH))
}

/*
    Return the epoch during which validator activations and exits initiated in ``epoch`` take effect.
    */
fun compute_activation_exit_epoch(epoch: Epoch): Epoch {
  return Epoch(((epoch + 1uL) + MAX_SEED_LOOKAHEAD))
}

/*
    Return the 32-byte fork data root for the ``current_version`` and ``genesis_validators_root``.
    This is used primarily in signature domains to avoid collisions across forks/chains.
    */
fun compute_fork_data_root(current_version: Version, genesis_validators_root: Root): Root {
  return hash_tree_root(ForkData(current_version = current_version, genesis_validators_root = genesis_validators_root))
}

/*
    Return the 4-byte fork digest for the ``current_version`` and ``genesis_validators_root``.
    This is a digest primarily used for domain separation on the p2p layer.
    4-bytes suffices for practical separation of forks/chains.
    */
fun compute_fork_digest(current_version: Version, genesis_validators_root: Root): ForkDigest {
  return ForkDigest(compute_fork_data_root(current_version, genesis_validators_root).slice(0uL, 4uL))
}

/*
    Return the domain for the ``domain_type`` and ``fork_version``.
    */
fun compute_domain(domain_type: DomainType, fork_version: Version? = null, genesis_validators_root: Root? = null): Domain {
  val fork_version_ = fork_version ?: GENESIS_FORK_VERSION
  val genesis_validators_root_ = genesis_validators_root ?: Root()
  var fork_data_root = compute_fork_data_root(fork_version_, genesis_validators_root_)
  return Domain((domain_type + fork_data_root.slice(0uL, 28uL)))
}

/*
    Return the signing root of an object by calculating the root of the object-domain tree.
    */
fun compute_signing_root(ssz_object: SSZObject, domain: Domain): Root {
  var domain_wrapped_object = SigningRoot(object_root = hash_tree_root(ssz_object), domain = domain)
  return hash_tree_root(domain_wrapped_object)
}

/*
    Return the current epoch.
    */
fun get_current_epoch(state: BeaconState): Epoch {
  return compute_epoch_at_slot(state.slot)
}

/*`
    Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
    */
fun get_previous_epoch(state: BeaconState): Epoch {
  var current_epoch = get_current_epoch(state)
  return if ((current_epoch == GENESIS_EPOCH)) GENESIS_EPOCH else Epoch((current_epoch - 1uL))
}

/*
    Return the block root at the start of a recent ``epoch``.
    */
fun get_block_root(state: BeaconState, epoch: Epoch): Root {
  return get_block_root_at_slot(state, compute_start_slot_at_epoch(epoch))
}

/*
    Return the block root at a recent ``slot``.
    */
fun get_block_root_at_slot(state: BeaconState, slot: Slot): Root {
  assert((slot < state.slot) && (state.slot <= (slot + SLOTS_PER_HISTORICAL_ROOT)))
  return state.block_roots[(slot % SLOTS_PER_HISTORICAL_ROOT)]
}

/*
    Return the randao mix at a recent ``epoch``.
    */
fun get_randao_mix(state: BeaconState, epoch: Epoch): Bytes32 {
  return state.randao_mixes[(epoch % EPOCHS_PER_HISTORICAL_VECTOR)]
}

/*
    Return the sequence of active validator indices at ``epoch``.
    */
fun get_active_validator_indices(state: BeaconState, epoch: Epoch): Sequence<ValidatorIndex> {
  return enumerate(state.validators).filter { (_, v) -> is_active_validator(v, epoch) }.map { (i, _) -> ValidatorIndex(i) }.toMutableList()
}

/*
    Return the validator churn limit for the current epoch.
    */
fun get_validator_churn_limit(state: BeaconState): uint64 {
  var active_validator_indices = get_active_validator_indices(state, get_current_epoch(state))
  return max(MIN_PER_EPOCH_CHURN_LIMIT, (len(active_validator_indices) / CHURN_LIMIT_QUOTIENT))
}

/*
    Return the seed at ``epoch``.
    */
fun get_seed(state: BeaconState, epoch: Epoch, domain_type: DomainType): Bytes32 {
  var mix = get_randao_mix(state, Epoch((((epoch + EPOCHS_PER_HISTORICAL_VECTOR) - MIN_SEED_LOOKAHEAD) - 1uL)))
  return hash(((domain_type + int_to_bytes(epoch, length = 8uL)) + mix))
}

/*
    Return the number of committees at ``slot``.
    */
fun get_committee_count_at_slot(state: BeaconState, slot: Slot): uint64 {
  var epoch = compute_epoch_at_slot(slot)
  return max(1uL, min(MAX_COMMITTEES_PER_SLOT, ((len(get_active_validator_indices(state, epoch)) / SLOTS_PER_EPOCH) / TARGET_COMMITTEE_SIZE)))
}

/*
    Return the beacon committee at ``slot`` for ``index``.
    */
fun get_beacon_committee(state: BeaconState, slot: Slot, index: CommitteeIndex): Sequence<ValidatorIndex> {
  var epoch = compute_epoch_at_slot(slot)
  var committees_per_slot = get_committee_count_at_slot(state, slot)
  return compute_committee(indices = get_active_validator_indices(state, epoch), seed = get_seed(state, epoch, DOMAIN_BEACON_ATTESTER), index = (((slot % SLOTS_PER_EPOCH) * committees_per_slot) + index), count = (committees_per_slot * SLOTS_PER_EPOCH))
}

/*
    Return the beacon proposer index at the current slot.
    */
fun get_beacon_proposer_index(state: BeaconState): ValidatorIndex {
  var epoch = get_current_epoch(state)
  var seed = hash((get_seed(state, epoch, DOMAIN_BEACON_PROPOSER) + int_to_bytes(state.slot, length = 8uL)))
  var indices = get_active_validator_indices(state, epoch)
  return compute_proposer_index(state, indices, seed)
}

/*
    Return the combined effective balance of the ``indices``.
    ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    Math safe up to ~10B ETH, afterwhich this overflows uint64.
    */
fun get_total_balance(state: BeaconState, indices: Set<ValidatorIndex>): Gwei {
  return Gwei(max(EFFECTIVE_BALANCE_INCREMENT, sum(indices.map { index -> state.validators[index].effective_balance }.toMutableList())))
}

/*
    Return the combined effective balance of the active validators.
    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    */
fun get_total_active_balance(state: BeaconState): Gwei {
  return get_total_balance(state, set(get_active_validator_indices(state, get_current_epoch(state))))
}

/*
    Return the signature domain (fork version concatenated with domain type) of a message.
    */
fun get_domain(state: BeaconState, domain_type: DomainType, epoch: Epoch? = null): Domain {
  var epoch_ = if ((epoch == null)) get_current_epoch(state) else epoch
  var fork_version = if ((epoch_ < state.fork.epoch)) state.fork.previous_version else state.fork.current_version
  return compute_domain(domain_type, fork_version, state.genesis_validators_root)
}

/*
    Return the indexed attestation corresponding to ``attestation``.
    */
fun get_indexed_attestation(state: BeaconState, attestation: Attestation): IndexedAttestation {
  var attesting_indices = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)
  return IndexedAttestation(attesting_indices = sorted(attesting_indices), data = attestation.data, signature = attestation.signature)
}

/*
    Return the set of attesting indices corresponding to ``data`` and ``bits``.
    */
fun get_attesting_indices(state: BeaconState, data: AttestationData, bits: CBitlist): Set<ValidatorIndex> {
  var committee = get_beacon_committee(state, data.slot, data.index)
  return set(enumerate(committee).filter { (i, _) -> bits[i] }.map { (_, index) -> index })
}

/*
    Increase the validator balance at index ``index`` by ``delta``.
    */
fun increase_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei): Unit {
  state.balances[index] += delta
}

/*
    Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
    */
fun decrease_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei): Unit {
  state.balances[index] = if ((delta > state.balances[index])) 0uL else (state.balances[index] - delta)
}

/*
    Initiate the exit of the validator with index ``index``.
    */
fun initiate_validator_exit(state: BeaconState, index: ValidatorIndex): Unit {
  var validator = state.validators[index]
  if ((validator.exit_epoch != FAR_FUTURE_EPOCH)) {
    return
  } else {
  }
  var exit_epochs = state.validators.filter { v -> (v.exit_epoch != FAR_FUTURE_EPOCH) }.map { v -> v.exit_epoch }.toMutableList()
  var exit_queue_epoch = max((exit_epochs + mutableListOf(compute_activation_exit_epoch(get_current_epoch(state)))))
  var exit_queue_churn = len(state.validators.filter { v -> (v.exit_epoch == exit_queue_epoch) }.map { v -> v }.toMutableList())
  if ((exit_queue_churn >= get_validator_churn_limit(state))) {
    exit_queue_epoch += Epoch(1uL)
  } else {
  }
  validator.exit_epoch = exit_queue_epoch
  validator.withdrawable_epoch = Epoch((validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY))
}

/*
    Slash the validator with index ``slashed_index``.
    */
fun slash_validator(state: BeaconState, slashed_index: ValidatorIndex, whistleblower_index: ValidatorIndex? = null): Unit {
  var epoch = get_current_epoch(state)
  initiate_validator_exit(state, slashed_index)
  var validator = state.validators[slashed_index]
  validator.slashed = true
  validator.withdrawable_epoch = max(validator.withdrawable_epoch, Epoch((epoch + EPOCHS_PER_SLASHINGS_VECTOR)))
  state.slashings[(epoch % EPOCHS_PER_SLASHINGS_VECTOR)] += validator.effective_balance
  decrease_balance(state, slashed_index, (validator.effective_balance / MIN_SLASHING_PENALTY_QUOTIENT))
  var proposer_index = get_beacon_proposer_index(state)
  var whistleblower_index_ = whistleblower_index ?: proposer_index
  var whistleblower_reward = Gwei((validator.effective_balance / WHISTLEBLOWER_REWARD_QUOTIENT))
  var proposer_reward = Gwei((whistleblower_reward / PROPOSER_REWARD_QUOTIENT))
  increase_balance(state, proposer_index, proposer_reward)
  increase_balance(state, whistleblower_index_, (whistleblower_reward - proposer_reward))
}

fun initialize_beacon_state_from_eth1(eth1_block_hash: Bytes32, eth1_timestamp: uint64, deposits: Sequence<Deposit>): BeaconState {
  var fork = Fork(previous_version = GENESIS_FORK_VERSION, current_version = GENESIS_FORK_VERSION, epoch = GENESIS_EPOCH)
  var state = BeaconState(
      genesis_time = ((eth1_timestamp - (eth1_timestamp % MIN_GENESIS_DELAY)) + (2uL * MIN_GENESIS_DELAY)),
      fork = fork,
      eth1_data = Eth1Data(block_hash = eth1_block_hash, deposit_count = len(deposits)),
      latest_block_header = BeaconBlockHeader(body_root = hash_tree_root(BeaconBlockBody())),
      randao_mixes = MutableList(EPOCHS_PER_HISTORICAL_VECTOR.toInt()) { eth1_block_hash })
  var leaves = list(map({ deposit -> deposit.data }, deposits))
  for ((index, deposit) in enumerate(deposits)) {
    var deposit_data_list = leaves.slice(0uL, (index.toULong() + 1uL))
    state.eth1_data.deposit_root = hash_tree_root(deposit_data_list)
    process_deposit(state, deposit)
  }
  for ((index, validator) in enumerate(state.validators)) {
    var balance = state.balances[index]
    validator.effective_balance = min((balance - (balance % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)
    if ((validator.effective_balance == MAX_EFFECTIVE_BALANCE)) {
      validator.activation_eligibility_epoch = GENESIS_EPOCH
      validator.activation_epoch = GENESIS_EPOCH
    } else {
    }
  }
  state.genesis_validators_root = hash_tree_root(state.validators)
  return state
}

fun is_valid_genesis_state(state: BeaconState): pybool {
  if ((state.genesis_time < MIN_GENESIS_TIME)) {
    return false
  } else {
  }
  if ((len(get_active_validator_indices(state, GENESIS_EPOCH)) < MIN_GENESIS_ACTIVE_VALIDATOR_COUNT)) {
    return false
  } else {
  }
  return true
}

fun state_transition(state: BeaconState, signed_block: SignedBeaconBlock, validate_result: pybool = true): BeaconState {
  var block = signed_block.message
  process_slots(state, block.slot)
  if (validate_result) {
    assert(verify_block_signature(state, signed_block))
  } else {
  }
  process_block(state, block)
  if (validate_result) {
    assert((block.state_root == hash_tree_root(state)))
  } else {
  }
  return state
}

fun verify_block_signature(state: BeaconState, signed_block: SignedBeaconBlock): pybool {
  var proposer = state.validators[signed_block.message.proposer_index]
  var signing_root = compute_signing_root(signed_block.message, get_domain(state, DOMAIN_BEACON_PROPOSER))
  return bls.Verify(proposer.pubkey, signing_root, signed_block.signature)
}

fun process_slots(state: BeaconState, slot: Slot): Unit {
  assert((state.slot <= slot))
  while ((state.slot < slot)) {
    process_slot(state)
    if ((((state.slot + 1uL) % SLOTS_PER_EPOCH) == 0uL)) {
      process_epoch(state)
    } else {
    }
    state.slot += Slot(1uL)
  }
}

fun process_slot(state: BeaconState): Unit {
  var previous_state_root = hash_tree_root(state)
  state.state_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_state_root
  if ((state.latest_block_header.state_root == Bytes32())) {
    state.latest_block_header.state_root = previous_state_root
  } else {
  }
  var previous_block_root = hash_tree_root(state.latest_block_header)
  state.block_roots[(state.slot % SLOTS_PER_HISTORICAL_ROOT)] = previous_block_root
}

fun process_epoch(state: BeaconState): Unit {
  process_justification_and_finalization(state)
  process_rewards_and_penalties(state)
  process_registry_updates(state)
  process_slashings(state)
  process_final_updates(state)
}

fun get_matching_source_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  assert((epoch in Pair(get_previous_epoch(state), get_current_epoch(state))))
  return if ((epoch == get_current_epoch(state))) state.current_epoch_attestations else state.previous_epoch_attestations
}

fun get_matching_target_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  return get_matching_source_attestations(state, epoch).filter { a -> (a.data.target.root == get_block_root(state, epoch)) }.map { a -> a }.toMutableList()
}

fun get_matching_head_attestations(state: BeaconState, epoch: Epoch): Sequence<PendingAttestation> {
  return get_matching_target_attestations(state, epoch).filter { a -> (a.data.beacon_block_root == get_block_root_at_slot(state, a.data.slot)) }.map { a -> a }.toMutableList()
}

fun get_unslashed_attesting_indices(state: BeaconState, attestations: Sequence<PendingAttestation>): Set<ValidatorIndex> {
  var output = set<ValidatorIndex>()
  for (a in attestations) {
    output = output.union(get_attesting_indices(state, a.data, a.aggregation_bits))
  }
  return set(filter({ index -> !(state.validators[index].slashed) }, output))
}

/*
    Return the combined effective balance of the set of unslashed validators participating in ``attestations``.
    Note: ``get_total_balance`` returns ``EFFECTIVE_BALANCE_INCREMENT`` Gwei minimum to avoid divisions by zero.
    */
fun get_attesting_balance(state: BeaconState, attestations: Sequence<PendingAttestation>): Gwei {
  return get_total_balance(state, get_unslashed_attesting_indices(state, attestations))
}

fun process_justification_and_finalization(state: BeaconState): Unit {
  if ((get_current_epoch(state) <= (GENESIS_EPOCH + 1uL))) {
    return
  } else {
  }
  var previous_epoch = get_previous_epoch(state)
  var current_epoch = get_current_epoch(state)
  var old_previous_justified_checkpoint = state.previous_justified_checkpoint
  var old_current_justified_checkpoint = state.current_justified_checkpoint
  state.previous_justified_checkpoint = state.current_justified_checkpoint
  state.justification_bits.updateSlice(
      1uL, len(state.justification_bits), state.justification_bits.slice(0uL, len(state.justification_bits) - 1uL))
  state.justification_bits[0uL] = 0uL
  var matching_target_attestations = get_matching_target_attestations(state, previous_epoch)
  if (((get_attesting_balance(state, matching_target_attestations) * 3uL) >= (get_total_active_balance(state) * 2uL))) {
    state.current_justified_checkpoint = Checkpoint(epoch = previous_epoch, root = get_block_root(state, previous_epoch))
    state.justification_bits[1uL] = 1uL
  } else {
  }
  matching_target_attestations = get_matching_target_attestations(state, current_epoch)
  if (((get_attesting_balance(state, matching_target_attestations) * 3uL) >= (get_total_active_balance(state) * 2uL))) {
    state.current_justified_checkpoint = Checkpoint(epoch = current_epoch, root = get_block_root(state, current_epoch))
    state.justification_bits[0uL] = 1uL
  } else {
  }
  var bits = state.justification_bits
  if (all(bits.slice(1uL, 4uL)) && ((old_previous_justified_checkpoint.epoch + 3uL) == current_epoch)) {
    state.finalized_checkpoint = old_previous_justified_checkpoint
  } else {
  }
  if (all(bits.slice(1uL, 3uL)) && ((old_previous_justified_checkpoint.epoch + 2uL) == current_epoch)) {
    state.finalized_checkpoint = old_previous_justified_checkpoint
  } else {
  }
  if (all(bits.slice(0uL, 3uL)) && ((old_current_justified_checkpoint.epoch + 2uL) == current_epoch)) {
    state.finalized_checkpoint = old_current_justified_checkpoint
  } else {
  }
  if (all(bits.slice(0uL, 2uL)) && ((old_current_justified_checkpoint.epoch + 1uL) == current_epoch)) {
    state.finalized_checkpoint = old_current_justified_checkpoint
  } else {
  }
}

fun get_base_reward(state: BeaconState, index: ValidatorIndex): Gwei {
  var total_balance = get_total_active_balance(state)
  var effective_balance = state.validators[index].effective_balance
  return Gwei((((effective_balance * BASE_REWARD_FACTOR) / integer_squareroot(total_balance)) / BASE_REWARDS_PER_EPOCH))
}

fun get_eligible_validator_indices(state: BeaconState): Sequence<ValidatorIndex> {
  var previous_epoch = get_previous_epoch(state)
  return enumerate(state.validators)
      .filter { (_, v) -> is_active_validator(v, previous_epoch) || v.slashed && ((previous_epoch + 1uL) < v.withdrawable_epoch) }
      .map { (index, _) -> ValidatorIndex(index) }.toMutableList()
}

/*
    Helper with shared logic for use by get source, target, and head deltas functions
    */
fun get_attestation_component_deltas(state: BeaconState, attestations: Sequence<PendingAttestation>): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var rewards = MutableList(len(state.validators).toInt()) { Gwei(0uL) }
  var penalties = MutableList(len(state.validators).toInt()) { Gwei(0uL) }
  var total_balance = get_total_active_balance(state)
  var unslashed_attesting_indices = get_unslashed_attesting_indices(state, attestations)
  var attesting_balance = get_total_balance(state, unslashed_attesting_indices)
  for (index in get_eligible_validator_indices(state)) {
    if ((index in unslashed_attesting_indices)) {
      var increment = EFFECTIVE_BALANCE_INCREMENT
      var reward_numerator = (get_base_reward(state, index) * (attesting_balance / increment))
      rewards[index] += (reward_numerator / (total_balance / increment))
    } else {
      penalties[index] += get_base_reward(state, index)
    }
  }
  return Pair(rewards, penalties)
}

/*
    Return attester micro-rewards/penalties for source-vote for each validator.
    */
fun get_source_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_source_attestations)
}

/*
    Return attester micro-rewards/penalties for target-vote for each validator.
    */
fun get_target_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_target_attestations)
}

/*
    Return attester micro-rewards/penalties for head-vote for each validator.
    */
fun get_head_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var matching_head_attestations = get_matching_head_attestations(state, get_previous_epoch(state))
  return get_attestation_component_deltas(state, matching_head_attestations)
}

/*
    Return proposer and inclusion delay micro-rewards/penalties for each validator.
    */
fun get_inclusion_delay_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var rewards = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
  var matching_source_attestations = get_matching_source_attestations(state, get_previous_epoch(state))
  for (index in get_unslashed_attesting_indices(state, matching_source_attestations)) {
    var attestation = min(matching_source_attestations.filter { a -> (index in get_attesting_indices(state, a.data, a.aggregation_bits)) }.map { a -> a }.toMutableList(), key = { a -> a.inclusion_delay })
    var proposer_reward = Gwei((get_base_reward(state, index) / PROPOSER_REWARD_QUOTIENT))
    rewards[attestation.proposer_index] += proposer_reward
    var max_attester_reward = (get_base_reward(state, index) - proposer_reward)
    rewards[index] += Gwei((max_attester_reward / attestation.inclusion_delay))
  }
  var penalties = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
  return Pair(rewards, penalties)
}

/*
    Return inactivity reward/penalty deltas for each validator.
    */
fun get_inactivity_penalty_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  var penalties = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
  var finality_delay = (get_previous_epoch(state) - state.finalized_checkpoint.epoch)
  if ((finality_delay > MIN_EPOCHS_TO_INACTIVITY_PENALTY)) {
    var matching_target_attestations = get_matching_target_attestations(state, get_previous_epoch(state))
    var matching_target_attesting_indices = get_unslashed_attesting_indices(state, matching_target_attestations)
    for (index in get_eligible_validator_indices(state)) {
      penalties[index] += Gwei((BASE_REWARDS_PER_EPOCH * get_base_reward(state, index)))
      if ((index !in matching_target_attesting_indices)) {
        var effective_balance = state.validators[index].effective_balance
        penalties[index] += Gwei(((effective_balance * finality_delay) / INACTIVITY_PENALTY_QUOTIENT))
      } else {
      }
    }
  } else {
  }
  var rewards = range(len(state.validators)).map { _ -> Gwei(0uL) }.toMutableList()
  return Pair(rewards, penalties)
}

/*
    Return attestation reward/penalty deltas for each validator.
    */
fun get_attestation_deltas(state: BeaconState): Pair<Sequence<Gwei>, Sequence<Gwei>> {
  val (source_rewards, source_penalties) = get_source_deltas(state)
  val (target_rewards, target_penalties) = get_target_deltas(state)
  val (head_rewards, head_penalties) = get_head_deltas(state)
  val (inclusion_delay_rewards, _) = get_inclusion_delay_deltas(state)
  val (_, inactivity_penalties) = get_inactivity_penalty_deltas(state)
  var rewards = range(len(state.validators)).map { i -> (((source_rewards[i] + target_rewards[i]) + head_rewards[i]) + inclusion_delay_rewards[i]) }.toMutableList()
  var penalties = range(len(state.validators)).map { i -> (((source_penalties[i] + target_penalties[i]) + head_penalties[i]) + inactivity_penalties[i]) }.toMutableList()
  return Pair(rewards, penalties)
}

fun process_rewards_and_penalties(state: BeaconState): Unit {
  if ((get_current_epoch(state) == GENESIS_EPOCH)) {
    return
  } else {
  }
  val (rewards, penalties) = get_attestation_deltas(state)
  for (index in range(len(state.validators))) {
    increase_balance(state, ValidatorIndex(index), rewards[index])
    decrease_balance(state, ValidatorIndex(index), penalties[index])
  }
}

fun process_registry_updates(state: BeaconState): Unit {
  for ((index, validator) in enumerate(state.validators)) {
    if (is_eligible_for_activation_queue(validator)) {
      validator.activation_eligibility_epoch = (get_current_epoch(state) + 1uL)
    } else {
    }
    if (is_active_validator(validator, get_current_epoch(state)) && (validator.effective_balance <= EJECTION_BALANCE)) {
      initiate_validator_exit(state, ValidatorIndex(index))
    } else {
    }
  }
  var activation_queue = sorted(
      enumerate(state.validators)
          .filter { (_, validator) -> is_eligible_for_activation(state, validator) }
          .map { (index, _) -> index }.toMutableList(),
      key = { index -> Tuple2(state.validators[index].activation_eligibility_epoch, index) })
  for (index in activation_queue.slice(0uL, get_validator_churn_limit(state))) {
    var validator = state.validators[index]
    validator.activation_epoch = compute_activation_exit_epoch(get_current_epoch(state))
  }
}

fun process_slashings(state: BeaconState): Unit {
  var epoch = get_current_epoch(state)
  var total_balance = get_total_active_balance(state)
  for ((index, validator) in enumerate(state.validators)) {
    if (validator.slashed && ((epoch + (EPOCHS_PER_SLASHINGS_VECTOR / 2uL)) == validator.withdrawable_epoch)) {
      var increment = EFFECTIVE_BALANCE_INCREMENT
      var penalty_numerator = ((validator.effective_balance / increment) * min((sum(state.slashings) * 3uL), total_balance))
      var penalty = ((penalty_numerator / total_balance) * increment)
      decrease_balance(state, ValidatorIndex(index), penalty)
    } else {
    }
  }
}

fun process_final_updates(state: BeaconState): Unit {
  var current_epoch = get_current_epoch(state)
  var next_epoch = Epoch((current_epoch + 1uL))
  if (((next_epoch % EPOCHS_PER_ETH1_VOTING_PERIOD) == 0uL)) {
    state.eth1_data_votes = mutableListOf()
  } else {
  }
  for ((index, validator) in enumerate(state.validators)) {
    var balance = state.balances[index]
    var HYSTERESIS_INCREMENT = (EFFECTIVE_BALANCE_INCREMENT / HYSTERESIS_QUOTIENT)
    var DOWNWARD_THRESHOLD = (HYSTERESIS_INCREMENT * HYSTERESIS_DOWNWARD_MULTIPLIER)
    var UPWARD_THRESHOLD = (HYSTERESIS_INCREMENT * HYSTERESIS_UPWARD_MULTIPLIER)
    if (((balance + DOWNWARD_THRESHOLD) < validator.effective_balance) || ((validator.effective_balance + UPWARD_THRESHOLD) < balance)) {
      validator.effective_balance = min((balance - (balance % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)
    } else {
    }
  }
  state.slashings[(next_epoch % EPOCHS_PER_SLASHINGS_VECTOR)] = Gwei(0uL)
  state.randao_mixes[(next_epoch % EPOCHS_PER_HISTORICAL_VECTOR)] = get_randao_mix(state, current_epoch)
  if (((next_epoch % (SLOTS_PER_HISTORICAL_ROOT / SLOTS_PER_EPOCH)) == 0uL)) {
    var historical_batch = HistoricalBatch(block_roots = state.block_roots, state_roots = state.state_roots)
    state.historical_roots.append(hash_tree_root(historical_batch))
  } else {
  }
  state.previous_epoch_attestations = state.current_epoch_attestations
  state.current_epoch_attestations = mutableListOf()
}

fun process_block(state: BeaconState, block: BeaconBlock): Unit {
  process_block_header(state, block)
  process_randao(state, block.body)
  process_eth1_data(state, block.body)
  process_operations(state, block.body)
}

fun process_block_header(state: BeaconState, block: BeaconBlock): Unit {
  assert((block.slot == state.slot))
  assert((block.proposer_index == get_beacon_proposer_index(state)))
  assert((block.parent_root == hash_tree_root(state.latest_block_header)))
  state.latest_block_header = BeaconBlockHeader(slot = block.slot, proposer_index = block.proposer_index, parent_root = block.parent_root, state_root = Bytes32(), body_root = hash_tree_root(block.body))
  var proposer = state.validators[block.proposer_index]
  assert(!(proposer.slashed))
}

fun process_randao(state: BeaconState, body: BeaconBlockBody): Unit {
  var epoch = get_current_epoch(state)
  var proposer = state.validators[get_beacon_proposer_index(state)]
  var signing_root = compute_signing_root(epoch, get_domain(state, DOMAIN_RANDAO))
  assert(bls.Verify(proposer.pubkey, signing_root, body.randao_reveal))
  var mix = xor(get_randao_mix(state, epoch), hash(body.randao_reveal))
  state.randao_mixes[(epoch % EPOCHS_PER_HISTORICAL_VECTOR)] = mix
}

fun process_eth1_data(state: BeaconState, body: BeaconBlockBody): Unit {
  state.eth1_data_votes.append(body.eth1_data)
  if (((state.eth1_data_votes.count(body.eth1_data) * 2uL) > (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH))) {
    state.eth1_data = body.eth1_data
  } else {
  }
}

fun process_operations(state: BeaconState, body: BeaconBlockBody): Unit {
  assert((len(body.deposits) == min(MAX_DEPOSITS, (state.eth1_data.deposit_count - state.eth1_deposit_index))))
  fun <T> for_ops(operations: Sequence<T>, fn: (BeaconState, T) -> Unit): Unit {
    for (operation in operations) {
      fn(state, operation)
    }
  }
  kotlin.Unit
  for_ops(body.proposer_slashings, ::process_proposer_slashing)
  for_ops(body.attester_slashings, ::process_attester_slashing)
  for_ops(body.attestations, ::process_attestation)
  for_ops(body.deposits, ::process_deposit)
  for_ops(body.voluntary_exits, ::process_voluntary_exit)
}

fun process_proposer_slashing(state: BeaconState, proposer_slashing: ProposerSlashing): Unit {
  var header_1 = proposer_slashing.signed_header_1.message
  var header_2 = proposer_slashing.signed_header_2.message
  assert((header_1.slot == header_2.slot))
  assert((header_1.proposer_index == header_2.proposer_index))
  assert((header_1 != header_2))
  var proposer = state.validators[header_1.proposer_index]
  assert(is_slashable_validator(proposer, get_current_epoch(state)))
  for (signed_header in listOf(proposer_slashing.signed_header_1, proposer_slashing.signed_header_2)) {
    var domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(signed_header.message.slot))
    var signing_root = compute_signing_root(signed_header.message, domain)
    assert(bls.Verify(proposer.pubkey, signing_root, signed_header.signature))
  }
  slash_validator(state, header_1.proposer_index)
}

fun process_attester_slashing(state: BeaconState, attester_slashing: AttesterSlashing): Unit {
  var attestation_1 = attester_slashing.attestation_1
  var attestation_2 = attester_slashing.attestation_2
  assert(is_slashable_attestation_data(attestation_1.data, attestation_2.data))
  assert(is_valid_indexed_attestation(state, attestation_1))
  assert(is_valid_indexed_attestation(state, attestation_2))
  var slashed_any = false
  var indices = set(attestation_1.attesting_indices).intersection(attestation_2.attesting_indices)
  for (index in sorted(indices)) {
    if (is_slashable_validator(state.validators[index], get_current_epoch(state))) {
      slash_validator(state, index)
      slashed_any = true
    } else {
    }
  }
  assert(slashed_any)
}

fun process_attestation(state: BeaconState, attestation: Attestation): Unit {
  var data = attestation.data
  assert((data.index < get_committee_count_at_slot(state, data.slot)))
  assert((data.target.epoch in Pair(get_previous_epoch(state), get_current_epoch(state))))
  assert((data.target.epoch == compute_epoch_at_slot(data.slot)))
  assert(((data.slot + MIN_ATTESTATION_INCLUSION_DELAY) <= state.slot) && (state.slot <= (data.slot + SLOTS_PER_EPOCH)))
  var committee = get_beacon_committee(state, data.slot, data.index)
  assert((len(attestation.aggregation_bits) == len(committee)))
  var pending_attestation = PendingAttestation(data = data, aggregation_bits = attestation.aggregation_bits, inclusion_delay = (state.slot - data.slot), proposer_index = get_beacon_proposer_index(state))
  if ((data.target.epoch == get_current_epoch(state))) {
    assert((data.source == state.current_justified_checkpoint))
    state.current_epoch_attestations.append(pending_attestation)
  } else {
    assert((data.source == state.previous_justified_checkpoint))
    state.previous_epoch_attestations.append(pending_attestation)
  }
  assert(is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation)))
}

fun process_deposit(state: BeaconState, deposit: Deposit): Unit {
  assert(is_valid_merkle_branch(leaf = hash_tree_root(deposit.data), branch = deposit.proof, depth = (DEPOSIT_CONTRACT_TREE_DEPTH + 1uL), index = state.eth1_deposit_index, root = state.eth1_data.deposit_root))
  state.eth1_deposit_index += 1uL
  var pubkey = deposit.data.pubkey
  var amount = deposit.data.amount
  var validator_pubkeys = state.validators.map { v -> v.pubkey }.toMutableList()
  if ((pubkey !in validator_pubkeys)) {
    var deposit_message = DepositMessage(pubkey = deposit.data.pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, amount = deposit.data.amount)
    var domain = compute_domain(DOMAIN_DEPOSIT)
    var signing_root = compute_signing_root(deposit_message, domain)
    if (!(bls.Verify(pubkey, signing_root, deposit.data.signature))) {
      return
    } else {
    }
    state.validators.append(Validator(pubkey = pubkey, withdrawal_credentials = deposit.data.withdrawal_credentials, activation_eligibility_epoch = FAR_FUTURE_EPOCH, activation_epoch = FAR_FUTURE_EPOCH, exit_epoch = FAR_FUTURE_EPOCH, withdrawable_epoch = FAR_FUTURE_EPOCH, effective_balance = min((amount - (amount % EFFECTIVE_BALANCE_INCREMENT)), MAX_EFFECTIVE_BALANCE)))
    state.balances.append(amount)
  } else {
    var index = ValidatorIndex(validator_pubkeys.index(pubkey))
    increase_balance(state, index, amount)
  }
}

fun process_voluntary_exit(state: BeaconState, signed_voluntary_exit: SignedVoluntaryExit): Unit {
  var voluntary_exit = signed_voluntary_exit.message
  var validator = state.validators[voluntary_exit.validator_index]
  assert(is_active_validator(validator, get_current_epoch(state)))
  assert((validator.exit_epoch == FAR_FUTURE_EPOCH))
  assert((get_current_epoch(state) >= voluntary_exit.epoch))
  assert((get_current_epoch(state) >= (validator.activation_epoch + PERSISTENT_COMMITTEE_PERIOD)))
  var domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, voluntary_exit.epoch)
  var signing_root = compute_signing_root(voluntary_exit, domain)
  assert(bls.Verify(validator.pubkey, signing_root, signed_voluntary_exit.signature))
  initiate_validator_exit(state, voluntary_exit.validator_index)
}

fun get_forkchoice_store(anchor_state: BeaconState): Store {
  var anchor_block_header = anchor_state.latest_block_header.copy()
  if ((anchor_block_header.state_root == Bytes32())) {
    anchor_block_header.state_root = hash_tree_root(anchor_state)
  } else {
  }
  var anchor_root = hash_tree_root(anchor_block_header)
  var anchor_epoch = get_current_epoch(anchor_state)
  var justified_checkpoint = Checkpoint(epoch = anchor_epoch, root = anchor_root)
  var finalized_checkpoint = Checkpoint(epoch = anchor_epoch, root = anchor_root)
  return Store(
      time = (anchor_state.genesis_time + (SECONDS_PER_SLOT * anchor_state.slot)),
      genesis_time = anchor_state.genesis_time,
      justified_checkpoint = justified_checkpoint,
      finalized_checkpoint = finalized_checkpoint,
      best_justified_checkpoint = justified_checkpoint,
      blocks = mutableMapOf(anchor_root to anchor_block_header),
      block_states = mutableMapOf(anchor_root to anchor_state.copy()),
      checkpoint_states = mutableMapOf(justified_checkpoint to anchor_state.copy()))
}

fun get_slots_since_genesis(store: Store): pyint {
  return pyint((store.time - store.genesis_time) / SECONDS_PER_SLOT)
}

fun get_current_slot(store: Store): Slot {
  return Slot((GENESIS_SLOT + get_slots_since_genesis(store).value.toLong().toUInt()))
}

fun compute_slots_since_epoch_start(slot: Slot): pyint {
  return pyint(slot - compute_start_slot_at_epoch(compute_epoch_at_slot(slot)))
}

fun get_ancestor(store: Store, root: Root, slot: Slot): Root {
  var block = store.blocks[root]!!
  if ((block.slot > slot)) {
    return get_ancestor(store, block.parent_root, slot)
  } else {
    if ((block.slot == slot)) {
      return root
    } else {
      return root
    }
  }
}

fun get_latest_attesting_balance(store: Store, root: Root): Gwei {
  var state = store.checkpoint_states[store.justified_checkpoint]!!
  var active_indices = get_active_validator_indices(state, get_current_epoch(state))
  return Gwei(sum(active_indices.filter { i -> (i in store.latest_messages) && (get_ancestor(store, store.latest_messages[i]!!.root, store.blocks[root]!!.slot) == root) }.map { i -> state.validators[i].effective_balance }))
}

fun filter_block_tree(store: Store, block_root: Root, blocks: CDict<Root, BeaconBlockHeader>): pybool {
  var block = store.blocks[block_root]!!
  var children = store.blocks.keys().filter { root -> (store.blocks[root]!!.parent_root == block_root) }.map { root -> root }.toMutableList()
  if (any(children)) {
    var filter_block_tree_result = children.map { child -> filter_block_tree(store, child, blocks) }.toMutableList()
    if (any(filter_block_tree_result)) {
      blocks[block_root] = block
      return true
    } else {
    }
    return false
  } else {
  }
  var head_state = store.block_states[block_root]!!
  var correct_justified = (store.justified_checkpoint.epoch == GENESIS_EPOCH) || (head_state.current_justified_checkpoint == store.justified_checkpoint)
  var correct_finalized = (store.finalized_checkpoint.epoch == GENESIS_EPOCH) || (head_state.finalized_checkpoint == store.finalized_checkpoint)
  if (correct_justified && correct_finalized) {
    blocks[block_root] = block
    return true
  } else {
  }
  return false
}

/*
    Retrieve a filtered block tree from ``store``, only returning branches
    whose leaf state's justified/finalized info agrees with that in ``store``.
    */
fun get_filtered_block_tree(store: Store): CDict<Root, BeaconBlockHeader> {
  var base = store.justified_checkpoint.root
  val blocks: CDict<Root, BeaconBlockHeader> = CDict()
  filter_block_tree(store, base, blocks)
  return blocks
}

fun get_head(store: Store): Root {
  var blocks = get_filtered_block_tree(store)
  var head = store.justified_checkpoint.root
  var justified_slot = compute_start_slot_at_epoch(store.justified_checkpoint.epoch)
  while (true) {
    var children = blocks.keys().filter { root -> (blocks[root]!!.parent_root == head) && (blocks[root]!!.slot > justified_slot) }.map { root -> root }.toMutableList()
    if ((len(children) == 0uL)) {
      return head
    } else {
    }
    head = max(children, key = { root -> Tuple2(get_latest_attesting_balance(store, root), root) })
  }
}

/*
    To address the bouncing attack, only update conflicting justified
    checkpoints in the fork choice if in the early slots of the epoch.
    Otherwise, delay incorporation of new justified checkpoint until next epoch boundary.

    See https://ethresear.ch/t/prevention-of-bouncing-attack-on-ffg/6114 for more detailed analysis and discussion.
    */
fun should_update_justified_checkpoint(store: Store, new_justified_checkpoint: Checkpoint): pybool {
  if ((compute_slots_since_epoch_start(get_current_slot(store)) < SAFE_SLOTS_TO_UPDATE_JUSTIFIED)) {
    return true
  } else {
  }
  var justified_slot = compute_start_slot_at_epoch(store.justified_checkpoint.epoch)
  if (!((get_ancestor(store, new_justified_checkpoint.root, justified_slot) == store.justified_checkpoint.root))) {
    return false
  } else {
  }
  return true
}

fun validate_on_attestation(store: Store, attestation: Attestation): Unit {
  var target = attestation.data.target
  var current_epoch = compute_epoch_at_slot(get_current_slot(store))
  var previous_epoch = if ((current_epoch > GENESIS_EPOCH)) (current_epoch - 1uL) else GENESIS_EPOCH
  assert((target.epoch in mutableListOf(current_epoch, previous_epoch)))
  assert((target.epoch == compute_epoch_at_slot(attestation.data.slot)))
  assert((target.root in store.blocks))
  assert((attestation.data.beacon_block_root in store.blocks))
  assert((store.blocks[attestation.data.beacon_block_root]!!.slot <= attestation.data.slot))
  assert((get_current_slot(store) >= (attestation.data.slot + 1uL)))
}

fun store_target_checkpoint_state(store: Store, target: Checkpoint): Unit {
  if ((target !in store.checkpoint_states)) {
    var base_state = store.block_states[target.root]!!.copy()
    process_slots(base_state, compute_start_slot_at_epoch(target.epoch))
    store.checkpoint_states[target] = base_state
  } else {
  }
}

fun update_latest_messages(store: Store, attesting_indices: Sequence<ValidatorIndex>, attestation: Attestation): Unit {
  var target = attestation.data.target
  var beacon_block_root = attestation.data.beacon_block_root
  for (i in attesting_indices) {
    if ((i !in store.latest_messages) || (target.epoch > store.latest_messages[i]!!.epoch)) {
      store.latest_messages[i] = LatestMessage(epoch = target.epoch, root = beacon_block_root)
    } else {
    }
  }
}

fun on_tick(store: Store, time: uint64): Unit {
  var previous_slot = get_current_slot(store)
  store.time = time
  var current_slot = get_current_slot(store)
  if (!((current_slot > previous_slot) && (compute_slots_since_epoch_start(current_slot) == pyint(0uL)))) {
    return
  } else {
  }
  if ((store.best_justified_checkpoint.epoch > store.justified_checkpoint.epoch)) {
    store.justified_checkpoint = store.best_justified_checkpoint
  } else {
  }
}

fun on_block(store: Store, signed_block: SignedBeaconBlock): Unit {
  var block = signed_block.message
  assert((block.parent_root in store.block_states))
  var pre_state = store.block_states[block.parent_root]!!.copy()
  assert((get_current_slot(store) >= block.slot))
  store.blocks[hash_tree_root(block)] = BeaconBlockHeader(
      slot = block.slot,
      proposer_index = block.proposer_index,
      parent_root = block.parent_root,
      state_root = block.state_root,
      body_root = hash_tree_root(block.body)
  )
  var finalized_slot = compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
  assert((block.slot > finalized_slot))
  assert((get_ancestor(store, hash_tree_root(block), finalized_slot) == store.finalized_checkpoint.root))
  var state = state_transition(pre_state, signed_block, true)
  store.block_states[hash_tree_root(block)] = state
  if ((state.current_justified_checkpoint.epoch > store.justified_checkpoint.epoch)) {
    if ((state.current_justified_checkpoint.epoch > store.best_justified_checkpoint.epoch)) {
      store.best_justified_checkpoint = state.current_justified_checkpoint
    } else {
    }
    if (should_update_justified_checkpoint(store, state.current_justified_checkpoint)) {
      store.justified_checkpoint = state.current_justified_checkpoint
    } else {
    }
  } else {
  }
  if ((state.finalized_checkpoint.epoch > store.finalized_checkpoint.epoch)) {
    store.finalized_checkpoint = state.finalized_checkpoint
    finalized_slot = compute_start_slot_at_epoch(store.finalized_checkpoint.epoch)
    if ((state.current_justified_checkpoint.epoch > store.justified_checkpoint.epoch) || (get_ancestor(store, store.justified_checkpoint.root, finalized_slot) != store.finalized_checkpoint.root)) {
      store.justified_checkpoint = state.current_justified_checkpoint
    } else {
    }
  } else {
  }
}

/*
    Run ``on_attestation`` upon receiving a new ``attestation`` from either within a block or directly on the wire.

    An ``attestation`` that is asserted as invalid may be valid at a later time,
    consider scheduling it for later processing in such case.
    */
fun on_attestation(store: Store, attestation: Attestation): Unit {
  validate_on_attestation(store, attestation)
  store_target_checkpoint_state(store, attestation.data.target)
  var target_state = store.checkpoint_states[attestation.data.target]!!
  var indexed_attestation = get_indexed_attestation(target_state, attestation)
  assert(is_valid_indexed_attestation(target_state, indexed_attestation))
  update_latest_messages(store, indexed_attestation.attesting_indices, attestation)
}

fun check_if_validator_active(state: BeaconState, validator_index: ValidatorIndex): pybool {
  var validator = state.validators[validator_index]
  return is_active_validator(validator, get_current_epoch(state))
}

/*
    Return the committee assignment in the ``epoch`` for ``validator_index``.
    ``assignment`` returned is a tuple of the following form:
        * ``assignmentlistOf(0)`` is the list of validators in the committee
        * ``assignmentlistOf(1)`` is the index to which the committee is assigned
        * ``assignmentlistOf(2)`` is the slot at which the committee is assigned
    Return None if no assignment.
    */
fun get_committee_assignment(state: BeaconState, epoch: Epoch, validator_index: ValidatorIndex): Triple<Sequence<ValidatorIndex>, CommitteeIndex, Slot>? {
  var next_epoch = (get_current_epoch(state) + 1uL)
  assert((epoch <= next_epoch))
  var start_slot = compute_start_slot_at_epoch(epoch)
  for (slot in range(start_slot, (start_slot + SLOTS_PER_EPOCH))) {
    for (index in range(get_committee_count_at_slot(state, Slot(slot)))) {
      var committee = get_beacon_committee(state, Slot(slot), CommitteeIndex(index))
      if ((validator_index in committee)) {
        return Triple(committee, CommitteeIndex(index), Slot(slot))
      } else {
      }
    }
  }
  return null
}

fun is_proposer(state: BeaconState, validator_index: ValidatorIndex): pybool {
  return (get_beacon_proposer_index(state) == validator_index)
}

fun get_epoch_signature(state: BeaconState, block: BeaconBlock, privkey: pyint): BLSSignature {
  var domain = get_domain(state, DOMAIN_RANDAO, compute_epoch_at_slot(block.slot))
  var signing_root = compute_signing_root(compute_epoch_at_slot(block.slot), domain)
  return bls.Sign(privkey, signing_root)
}

fun compute_time_at_slot(state: BeaconState, slot: Slot): uint64 {
  return (state.genesis_time + (slot * SECONDS_PER_SLOT))
}

fun voting_period_start_time(state: BeaconState): uint64 {
  var eth1_voting_period_start_slot = Slot((state.slot - (state.slot % (EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH))))
  return compute_time_at_slot(state, eth1_voting_period_start_slot)
}

fun is_candidate_block(block: Eth1Block, period_start: uint64): pybool {
  return (block.timestamp <= (period_start - (SECONDS_PER_ETH1_BLOCK * ETH1_FOLLOW_DISTANCE))) && (block.timestamp >= (period_start - ((SECONDS_PER_ETH1_BLOCK * ETH1_FOLLOW_DISTANCE) * 2uL)))
}

fun get_eth1_vote(state: BeaconState, eth1_chain: Sequence<Eth1Block>): Eth1Data {
  var period_start = voting_period_start_time(state)
  var votes_to_consider = eth1_chain.filter { block -> is_candidate_block(block, period_start) }.map { block -> get_eth1_data(block) }.toMutableList()
  var valid_votes = state.eth1_data_votes.filter { vote -> vote in votes_to_consider }.map { vote -> vote }.toMutableList()
  var default_vote = if (any(votes_to_consider)) votes_to_consider[-(1uL)] else state.eth1_data
  return max(valid_votes, key = { v -> Tuple2(valid_votes.count(v), -(valid_votes.index(v))) }, default = default_vote)
}

fun compute_new_state_root(state: BeaconState, block: BeaconBlock): Root {
  process_slots(state, block.slot)
  process_block(state, block)
  return hash_tree_root(state)
}

fun get_block_signature(state: BeaconState, header: BeaconBlockHeader, privkey: pyint): BLSSignature {
  var domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_at_slot(header.slot))
  var signing_root = compute_signing_root(header, domain)
  return bls.Sign(privkey, signing_root)
}

fun get_attestation_signature(state: BeaconState, attestation_data: AttestationData, privkey: pyint): BLSSignature {
  var domain = get_domain(state, DOMAIN_BEACON_ATTESTER, attestation_data.target.epoch)
  var signing_root = compute_signing_root(attestation_data, domain)
  return bls.Sign(privkey, signing_root)
}

fun get_slot_signature(state: BeaconState, slot: Slot, privkey: pyint): BLSSignature {
  var domain = get_domain(state, DOMAIN_SELECTION_PROOF, compute_epoch_at_slot(slot))
  var signing_root = compute_signing_root(slot, domain)
  return bls.Sign(privkey, signing_root)
}

fun is_aggregator(state: BeaconState, slot: Slot, index: CommitteeIndex, slot_signature: BLSSignature): pybool {
  var committee = get_beacon_committee(state, slot, index)
  var modulo = max(1uL, (len(committee) / TARGET_AGGREGATORS_PER_COMMITTEE))
  return ((bytes_to_int(hash(slot_signature).slice(0uL, 8uL)) % modulo) == 0uL)
}

fun get_aggregate_signature(attestations: Sequence<Attestation>): BLSSignature {
  var signatures = attestations.map { attestation -> attestation.signature }.toMutableList()
  return bls.Aggregate(signatures)
}

fun get_aggregate_and_proof(state: BeaconState, aggregator_index: ValidatorIndex, aggregate: Attestation, privkey: pyint): AggregateAndProof {
  return AggregateAndProof(aggregator_index = aggregator_index, aggregate = aggregate, selection_proof = get_slot_signature(state, aggregate.data.slot, privkey))
}

fun get_aggregate_and_proof_signature(state: BeaconState, aggregate_and_proof: AggregateAndProof, privkey: pyint): BLSSignature {
  var aggregate = aggregate_and_proof.aggregate
  var domain = get_domain(state, DOMAIN_AGGREGATE_AND_PROOF, compute_epoch_at_slot(aggregate.data.slot))
  var signing_root = compute_signing_root(aggregate_and_proof, domain)
  return bls.Sign(privkey, signing_root)
}

fun get_eth1_data(block: Eth1Block): Eth1Data = TODO()