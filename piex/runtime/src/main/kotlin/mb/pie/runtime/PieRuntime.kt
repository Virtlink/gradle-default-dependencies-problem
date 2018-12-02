package mb.pie.runtime

import mb.log.api.Logger
import mb.pie.api.PieApi

class PieRuntime : PieApi {
  override fun doPieStuff(logger: Logger) {
    logger.error("The PIE runtime does some stuff!")
  }
}
