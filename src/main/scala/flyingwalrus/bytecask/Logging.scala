package flyingwalrus.bytecask

import org.slf4j.LoggerFactory

trait Logging {

  final def trace_? = logger.isTraceEnabled

  final def debug_? = logger.isDebugEnabled

  final def info_? = logger.isInfoEnabled

  final def warning_? = logger.isWarnEnabled

  final def error_? = logger.isErrorEnabled

  private lazy val logger = LoggerFactory.getLogger(this.getClass.getName)

  final def debug(fmt: => String, arg: Any, argN: Any*): Unit = {
    debug(message(fmt, arg, argN: _*))
  }

  final def debug(msg: => String): Unit = {
    if (debug_?) logger debug msg
  }

  final def info(msg: => String) = {
    if (info_?) logger info msg
  }

  final def warn(msg: => String) = {
    if (warning_?) logger warn msg
  }

  final def error(msg: => String) = {
    if (error_?) logger error msg
  }

  protected final def message(fmt: String, arg: Any, argN: Any*): String = {
    if ((argN eq null) || argN.isEmpty) fmt.format(arg)
    else fmt.format((arg +: argN): _*)
  }
}
