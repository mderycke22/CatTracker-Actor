package be.unamur.cattracker.model

import slick.jdbc.PostgresProfile.api.*

import java.time.LocalTime

class DispenserScheduleTable(tag: Tag) extends Table[DispenserSchedule](tag, "dispenser_schedule") {
  override def * = (distributionTime, kibblesAmountValue, label, isActive).mapTo[DispenserSchedule]
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def distributionTime: Rep[LocalTime] = column[LocalTime]("distribution_time")
  def kibblesAmountValue: Rep[Int] = column[Int]("kibbles_amount_value")
  def label: Rep[String] = column[String]("label")
  def isActive: Rep[Boolean] = column[Boolean]("is_active")
}

case class DispenserSchedule(distributionTime: LocalTime, kibblesAmountValue: Int, label: String, isActive: Boolean)