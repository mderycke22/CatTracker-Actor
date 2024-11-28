package be.unamur.cattracker.http

import akka.actor.ActorSystem
import be.unamur.cattracker.model.{DispenserSchedule, DispenserScheduleUpdateDTO}
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
            system.log.error(s"Couldn't insert the dispenser schedule: $t")
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
        system.log.error(s"Couldn't insert the dispenser schedule: $t")
        0
      }
  }

  def updateDispenserSchedule(id: Long, dto: DispenserScheduleUpdateDTO): Future[Int] = {
    
    dispenserScheduleRepository.update(id, DispenserSchedule(id, dto.distributionTime, dto.kibblesAmountValue, dto.label)).map { i =>
        system.log.info(s"Dispenser schedule updated successfully ($i value updated)")
        i
      }
      .recover { case t: Throwable =>
        system.log.error(s"Couldn't update the dispenser schedule: $t")
        0
      }
  }

  def deleteDispenserSchedule(id: Long): Future[Int] = {
    dispenserScheduleRepository.delete(id).map { i =>
        system.log.info(s"Dispenser schedule deleted successfully ($i value deleted)")
        i
      }
      .recover { case t: Throwable =>
        system.log.error(s"Couldn't delete the dispenser schedule: $t")
        0
      }
  }
}
