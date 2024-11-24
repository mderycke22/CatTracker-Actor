package be.unamur.cattracker.model

import slick.jdbc.PostgresProfile.api.*

import java.time.LocalTime

class DispenserScheduleTable(tag: Tag) extends Table[DispenserSchedule](tag, "sensor_values") {
  override def * = (distributionTime, label).mapTo[DispenserSchedule]
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def distributionTime: Rep[LocalTime] = column[LocalTime]("distribution_time")
  def label: Rep[String] = column[String]("label")
}

case class DispenserSchedule(distributionTime: LocalTime, label: String)