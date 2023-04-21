package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example.typeclass.EqInterpreter
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

case class MovieTheater(name: Option[String] = None)
object MovieTheater extends ShapeTag.Companion[MovieTheater] {
  val id: ShapeId = ShapeId("smithy4s.example", "MovieTheater")

  val hints: Hints = Hints(
    smithy4s.example.Eq(),
  )

  implicit val schema: Schema[MovieTheater] = struct(
    string.optional[MovieTheater]("name", _.name),
  ){
    MovieTheater.apply
  }.withId(id).addHints(hints)

  implicit val movieTheaterEq: cats.Eq[MovieTheater] = EqInterpreter.fromSchema(schema)
}