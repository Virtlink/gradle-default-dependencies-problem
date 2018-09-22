package mb.pie.runtime

import mb.log.api.Logger
import mb.pie.api.PieAPI

class PieRuntime : PieAPI {
  override fun doPieStuff(logger: Logger) {
    logger.info("The PIE runtime does some stuff!")
  }
}
