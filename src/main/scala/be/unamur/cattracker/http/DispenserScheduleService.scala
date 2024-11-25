package be.unamur.cattracker.http

import akka.actor.ActorSystem
import be.unamur.cattracker.model.DispenserSchedule
import be.unamur.cattracker.repositories.DispenserScheduleBaseRepository

import scala.concurrent.{ExecutionContext, Future}

class DispenserScheduleService(dispenserScheduleRepository: DispenserScheduleBaseRepository[DispenserSchedule, Long])
                              (implicit val system: ActorSystem, val ec: ExecutionContext) {

  def getDispenserSchedules(label: String): Future[Seq[DispenserSchedule]]  = {
    label match
      case null => {
        dispenserScheduleRepository.findAll()
          .map { ds =>
            system.log.info("Dispenser schedules retrieved successfully")
            ds
          }
          .recover { case t: Throwable =>
            system.log.error(s"Error getting the sensor values: $t")
            Seq.empty[DispenserSchedule]
          }
      }
      case _ => {
        dispenserScheduleRepository.findByLabelContains(label)
          .map { ds =>
            system.log.info(s"Dispenser schedules containing '${label}' retrieved successfully'")
            ds
          }
          .recover { case t: Throwable =>
            system.log.error(s"Couldn't insert the sensor value: $t")
            Seq.empty[DispenserSchedule]
          }
      }
  }

  def addDispenserSchedule(dispenserSchedule: DispenserSchedule): Future[Int] = {
    dispenserScheduleRepository.save(dispenserSchedule).map { i =>
        system.log.info(s"Dispenser schedule added successfully ($i value added)")
        i
      }
      .recover { case t: Throwable =>
        system.log.error(s"Couldn't insert the sensor value: $t")
        0
      }
  }
  
  def updateDispenserSchedule(id: Long, dispenserSchedule: DispenserSchedule): Future[Int] = {
    dispenserScheduleRepository.update(id, dispenserSchedule).map { i =>
        system.log.info(s"Dispenser schedule updated successfully ($i value updated)")
        i
      }
      .recover { case t: Throwable =>
        system.log.error(s"Couldn't update the sensor value: $t")
        0
      }
  }
}
