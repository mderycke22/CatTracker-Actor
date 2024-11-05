package be.unamur.cattracker.model

import slick.jdbc.PostgresProfile.api.*

import java.time.LocalDateTime

class SensorValueTable(tag: Tag) extends Table[SensorValue](tag, "sensor_values") {
  override def * = (sensorType, sensorValue, recordedAt).mapTo[SensorValue]
  def valueId: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def sensorType: Rep[String] = column[String]("sensor_type")
  def sensorValue: Rep[Float] = column[Float]("sensor_value")
  def recordedAt: Rep[LocalDateTime] = column[LocalDateTime]("recorded_at")
}

case class SensorValue(sensorType: String, sensorValue: Float, recordedAt: LocalDateTime);
