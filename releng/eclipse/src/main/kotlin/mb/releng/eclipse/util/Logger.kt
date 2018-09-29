package mb.releng.eclipse.util

import java.io.PrintWriter

interface Logger {
  fun warning(message: String)
  fun progress(message: String)
}

internal class StreamLogger(private val writer: PrintWriter = PrintWriter(System.out, true)) : Logger {
  override fun warning(message: String) {
    writer.println(message)
  }

  override fun progress(message: String) {
    writer.println(message)
  }
}