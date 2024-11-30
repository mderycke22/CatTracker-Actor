package be.unamur.cattracker.http

import akka.actor.typed.*
import be.unamur.cattracker.actors.DispenserScheduleDbActor.DispenserScheduleDbCommand
import be.unamur.cattracker.model.{DispenserSchedule, DispenserScheduleUpdateDTO}
import akka.actor.typed.scaladsl.AskPattern.*

import scala.concurrent.{ExecutionContext, Future}
import akka.util.Timeout
import be.unamur.cattracker.actors.DispenserScheduleDbActor

import scala.concurrent.duration.DurationInt
import scala.util.Failure

class DispenserScheduleService(dsDbActor: ActorRef[DispenserScheduleDbCommand])
                              (implicit val system: ActorSystem[Nothing], val ec: ExecutionContext) {

  implicit val timeout: Timeout = 5.seconds

  def getDispenserSchedules(label: Option[String]): Future[Seq[DispenserSchedule]]  = {
    dsDbActor.ask(ref => DispenserScheduleDbActor.Select(label, ref)).map {
      case DispenserScheduleDbActor.Retrieved(values) =>
        system.log.info("Dispenser schedules retrieved successfully")
        values
      case DispenserScheduleDbActor.NotRetrieved(message) =>
        system.log.error(s"Error getting the dispenser schedules: $message")
        Seq.empty[DispenserSchedule]
    }
  }

  def addDispenserSchedule(dto: DispenserScheduleUpdateDTO): Future[Int] = {
    val entity = DispenserSchedule(0, dto.distributionTime, dto.kibblesAmountValue, dto.label)
    dsDbActor.ask(ref => DispenserScheduleDbActor.Insert(entity, ref)).map {
      case DispenserScheduleDbActor.Inserted(values) =>
        system.log.info(s"Dispenser schedule added successfully ($values value added)")
        values
      case DispenserScheduleDbActor.NotRetrieved(message) =>
        system.log.error(s"Couldn't insert the dispenser schedule: $message")
        throw new IllegalArgumentException("Couldn't add the dispenser schedule due to: " + message)
    }
  }

  def updateDispenserSchedule(id: Long, dto: DispenserScheduleUpdateDTO): Future[Int] = {
    dsDbActor.ask(ref => DispenserScheduleDbActor.Update(id, dto, ref)).map {
      case DispenserScheduleDbActor.Updated(values) =>
        system.log.info(s"Dispenser schedule updated successfully ($values value updated)")
        values
      case DispenserScheduleDbActor.NotUpdated(message) =>
        system.log.error(s"Couldn't update the dispenser schedule: $message")
        throw new IllegalArgumentException("Couldn't update the dispenser schedule due to: " + message)
    }
  }

  def deleteDispenserSchedule(id: Long): Future[Int] = {
    dsDbActor.ask(ref => DispenserScheduleDbActor.Delete(id, ref)).map {
      case DispenserScheduleDbActor.Deleted(values) =>
        system.log.info(s"Dispenser schedule deleted successfully ($values value deleted)")
        values
      case DispenserScheduleDbActor.NotDeleted(message) =>
        system.log.error(s"Couldn't delete the dispenser schedule: $message")
        throw new IllegalArgumentException("Couldn't delete the dispenser schedule due to: " + message)
    }
  }
}
