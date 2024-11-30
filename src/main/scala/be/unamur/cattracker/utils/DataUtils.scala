package be.unamur.cattracker.utils

object DataUtils {

  def splitSensorData(input: String): Map[String, String] = {
    val parts = input.split(";")

    if (parts.length == 3) {
      Map(
        "sensor" -> parts(0),
        "value" -> parts(1),
        "unit" -> parts(2)
      )
    } else {
      throw new IllegalArgumentException("Invalid string for the sensor")
    }
  }

  def castToFloat(value: String): Float = {
    valueToFloat(value) match {
      case Some(floatValue) => floatValue
      case None => Float.MinValue
    }
  }

  private def valueToFloat(value: String): Option[Float] = {
    try {
      Some(value.toFloat)
    } catch {
      case _: NumberFormatException => None
    }
  }

}
