package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object TestString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestString")
  val hints : Hints = Hints(
    smithy4s.example.TestTrait(Some(smithy4s.example.OrderType.InStoreOrder(smithy4s.example.OrderNumber(100), Some("someLocation")))),
  )
  val underlyingSchema : Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : Schema[TestString] = bijection(underlyingSchema, asBijection)
}