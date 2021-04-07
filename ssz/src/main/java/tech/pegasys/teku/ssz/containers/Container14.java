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

package tech.pegasys.teku.ssz.containers;

import tech.pegasys.teku.ssz.SszData;
import tech.pegasys.teku.ssz.impl.AbstractSszImmutableContainer;
import tech.pegasys.teku.ssz.tree.TreeNode;

/** Autogenerated by tech.pegasys.teku.ssz.backing.ContainersGenerator */
public class Container14<
        C extends Container14<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13>,
        V0 extends SszData,
        V1 extends SszData,
        V2 extends SszData,
        V3 extends SszData,
        V4 extends SszData,
        V5 extends SszData,
        V6 extends SszData,
        V7 extends SszData,
        V8 extends SszData,
        V9 extends SszData,
        V10 extends SszData,
        V11 extends SszData,
        V12 extends SszData,
        V13 extends SszData>
    extends AbstractSszImmutableContainer {

  protected Container14(
      ContainerSchema14<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> schema) {
    super(schema);
  }

  protected Container14(
      ContainerSchema14<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> schema,
      TreeNode backingNode) {
    super(schema, backingNode);
  }

  protected Container14(
      ContainerSchema14<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13> schema,
      V0 arg0,
      V1 arg1,
      V2 arg2,
      V3 arg3,
      V4 arg4,
      V5 arg5,
      V6 arg6,
      V7 arg7,
      V8 arg8,
      V9 arg9,
      V10 arg10,
      V11 arg11,
      V12 arg12,
      V13 arg13) {
    super(
        schema, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
        arg13);
  }

  protected V0 getField0() {
    return getAny(0);
  }

  protected V1 getField1() {
    return getAny(1);
  }

  protected V2 getField2() {
    return getAny(2);
  }

  protected V3 getField3() {
    return getAny(3);
  }

  protected V4 getField4() {
    return getAny(4);
  }

  protected V5 getField5() {
    return getAny(5);
  }

  protected V6 getField6() {
    return getAny(6);
  }

  protected V7 getField7() {
    return getAny(7);
  }

  protected V8 getField8() {
    return getAny(8);
  }

  protected V9 getField9() {
    return getAny(9);
  }

  protected V10 getField10() {
    return getAny(10);
  }

  protected V11 getField11() {
    return getAny(11);
  }

  protected V12 getField12() {
    return getAny(12);
  }

  protected V13 getField13() {
    return getAny(13);
  }
}