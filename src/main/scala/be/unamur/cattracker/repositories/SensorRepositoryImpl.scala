package be.unamur.cattracker.repositories

import be.unamur.cattracker.model.{SensorValue, SensorValueTable}
import slick.jdbc.PostgresProfile.api.*

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class SensorRepositoryImpl(db: Database)(implicit ec: ExecutionContext) extends SensorBaseRepository[SensorValue, Long] {
  private val sensorValuesTable = TableQuery[SensorValueTable]

  override def findById(id: Long): Future[Option[SensorValue]] =
    db.run(sensorValuesTable.filter(_.valueId === id).result.headOption)

  override def findAll(): Future[Seq[SensorValue]] =
    db.run(sensorValuesTable.result)

  override def save(entity: SensorValue): Future[Int] =
    db.run(sensorValuesTable += entity)

  override def delete(id: Long): Future[Int] =
    db.run(sensorValuesTable.filter(_.valueId === id).delete)

  override def findForSensorBetween(sensor: String, start: LocalDateTime, end: LocalDateTime): Future[Seq[SensorValue]] = {
    val query = for (
      record <- sensorValuesTable
      if record.sensorType === sensor
      if record.recordedAt >= start && record.recordedAt <= end
    ) yield record
    db.run(query.result)
  }

  override def findForSensor(sensor: String): Future[Seq[SensorValue]] = {
    val query = sensorValuesTable.filter(_.sensorType === sensor)
    db.run(query.result)
  }
}