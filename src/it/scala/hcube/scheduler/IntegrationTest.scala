package hcube.scheduler

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

/**
  * sbt it:test
  */
class IntegrationTest extends Specification with Mockito {

  "dummy integration test" >> {
    "foo" must_== "foo"
  }

}
