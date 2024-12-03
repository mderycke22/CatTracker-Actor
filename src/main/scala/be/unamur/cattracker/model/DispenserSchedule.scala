package be.unamur.cattracker.model

import slick.jdbc.PostgresProfile.api.*

import java.time.LocalTime

class DispenserScheduleTable(tag: Tag) extends Table[DispenserSchedule](tag, "dispenser_schedule") {
  override def * = (id, distributionTime, kibblesAmountValue, label).mapTo[DispenserSchedule]
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def distributionTime: Rep[LocalTime] = column[LocalTime]("distribution_time")
  def kibblesAmountValue: Rep[Int] = column[Int]("kibbles_amount_value")
  def label: Rep[String] = column[String]("label")
}

case class DispenserSchedule(id: Long, distributionTime: LocalTime, kibblesAmountValue: Int, label: String)

case class DispenserScheduleUpdateDTO(distributionTime: LocalTime, kibblesAmountValue: Int, label: String)

case class DistributionDTO(kibblesAmountValue: Int)