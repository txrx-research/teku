/*
 * Copyright 2020 ConsenSys AG.
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

package tech.pegasys.teku.cli.options;

import java.util.Optional;
import picocli.CommandLine.Option;
import tech.pegasys.teku.config.TekuConfiguration;

public class DepositOptions {

  @Option(
      names = {"--eth1-endpoint"},
      paramLabel = "<NETWORK>",
      description = "URL for Eth1 node.",
      arity = "1")
  private String eth1Endpoint = null;

  @Option(
      names = {"--eth1-deposit-contract-max-request-size"},
      paramLabel = "<INTEGER>",
      description =
          "Maximum number of blocks to request deposit contract event logs for in a single request.",
      arity = "1")
  private int eth1LogsMaxBlockRange = 10_000;

  public int getEth1LogsMaxBlockRange() {
    return eth1LogsMaxBlockRange;
  }

  public void configure(final TekuConfiguration.Builder builder) {
    builder.powchain(
        b ->
            b.eth1Endpoint(Optional.ofNullable(eth1Endpoint))
                .eth1LogsMaxBlockRange(eth1LogsMaxBlockRange));
  }
}
