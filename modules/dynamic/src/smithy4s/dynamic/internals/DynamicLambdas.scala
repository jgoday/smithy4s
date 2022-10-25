/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.dynamic.internals

import smithy4s.schema.Alt
import smithy4s.schema.Schema

/**
  * A bunch of hash-code stable lambdas that are less likely to break memoization
  * that may be able to SchemaVisitors.
  */
private[internals] object DynamicLambdas {
  final case class Injector(index: Int) extends (DynData => DynAlt) {
    def apply(data: DynData): DynAlt = (index, data)
  }

  final case class Accessor(index: Int) extends (DynStruct => DynData) {
    def apply(data: DynStruct): DynData = data(index)
  }

  final case class OptionalAccessor(index: Int)
      extends (DynStruct => Option[DynData]) {
    def apply(data: DynStruct): Option[DynData] = Option(data(index))
  }

  case object Constructor extends (IndexedSeq[DynData] => DynStruct) {
    def apply(fields: IndexedSeq[Any]): DynStruct = {
      val array = Array.ofDim[Any](fields.size)
      var i = 0
      fields.foreach {
        case None        => i += 1 // leaving value to null
        case Some(value) => (array(i) = value); i += 1
        case other       => (array(i) = other); i += 1
      }
      array
    }
  }

  case class Dispatcher(alts: IndexedSeq[Alt[Schema, DynAlt, DynData]])
      extends (DynAlt => Alt.SchemaAndValue[DynAlt, DynData]) {
    def apply(dynAlt: DynAlt): Alt.SchemaAndValue[DynAlt, DynData] = {
      val index = dynAlt._1
      val data = dynAlt._2
      alts(index).apply(data)
    }
  }

}
