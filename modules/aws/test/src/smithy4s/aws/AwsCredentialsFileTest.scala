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

package smithy4s.aws

import weaver._
import fs2.io.file.Files
import cats.effect.IO
import smithy4s.aws.kernel.AwsCredentials

object AwsCredentialsFileTest extends FunSuite {
  val creds = AwsCredentials.Default("key", "sec", Some("token"))

  private def expectRight[A](
      e: Either[Throwable, A]
  )(f: A => Expectations): Expectations =
    e.fold(
      err => failure(s"Got Left but expected a Right. ${err.getMessage}"),
      f
    )

  test("find default profile") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[default]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(Some(creds), res.default) &&
      expect.same(true, res.profiles.isEmpty)
    }
  }

  test("parse comments") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|# A comment
             |[default] # another comment
             |aws_secret_access_key = sec
             |aws_access_key_id     = key #yet another comment
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(Some(creds), res.default)
    }
  }

  test("find some other profile") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[profile other]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(None, res.default) &&
      expect.same(
        "other" -> creds,
        res.profiles.head
      )
    }
  }

  test("find some other profile w/o prefix") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[other]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(None, res.default) &&
      expect.same(
        "other" -> creds,
        res.profiles.head
      )
    }
  }

  test("find default and some other profiles") {
    expectRight(
      AwsCredentialsFile.processFileLines(
        asLines(
          """|[default]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token
             |[profile p1]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token
             |
             |[p2]
             |aws_secret_access_key = sec
             |aws_access_key_id     = key
             |aws_session_token     = token""".stripMargin
        )
      )
    ) { res =>
      expect.same(Some(creds), res.default) &&
      expect.same(creds, res.profiles("p1")) &&
      expect.same(creds, res.profiles("p2"))
    }
  }

  private def asLines(s: String) = s.split("\\n").toList
}

object AwsCredentialsFileReadTest extends SimpleIOSuite {
  test("read credentials from a file") {
    Files[IO].tempFile.use { path =>
      val content = """|[default]
                       |aws_secret_access_key = def_sec
                       |aws_access_key_id     = def_key
                       |aws_session_token     = def_token
                       |[profile p1]
                       |aws_secret_access_key = sec
                       |aws_access_key_id     = key
                       |aws_session_token     = token
                  """.stripMargin

      val writeFile = fs2.Stream
        .emit(content)
        .through(fs2.text.utf8.encode)
        .through(Files[IO].writeAll(path))
        .compile
        .drain
      val readDefault = AwsCredentialsFile.fromDisk(path, None)
      val readP1 = AwsCredentialsFile.fromDisk(path, Some("p1"))

      for {
        _ <- writeFile

        default <- readDefault
        p1 <- readP1
      } yield {
        val expectedP1 =
          AwsCredentials.Default("key", "sec", Some("token"))
        val expectedDefault =
          AwsCredentials.Default("def_key", "def_sec", Some("def_token"))
        expect.same(expectedDefault, default) &&
        expect.same(expectedP1, p1)
      }
    }
  }
}
