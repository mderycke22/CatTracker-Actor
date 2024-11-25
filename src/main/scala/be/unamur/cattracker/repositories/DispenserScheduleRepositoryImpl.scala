package be.unamur.cattracker.repositories

import be.unamur.cattracker.model.{DispenserSchedule, DispenserScheduleTable}
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.{ExecutionContext, Future}

class DispenserScheduleRepositoryImpl(db: Database)
                                     (implicit ec: ExecutionContext)
  extends DispenserScheduleBaseRepository[DispenserSchedule, Long] {

  private val dispenserScheduleTable = TableQuery[DispenserScheduleTable]

  override def findByLabelContains(label: String): Future[Seq[DispenserSchedule]] =
    val query = dispenserScheduleTable.filter(_.label.like(s"%$label%"))
    db.run(query.result)

  override def findAll(): Future[Seq[DispenserSchedule]] =
    db.run(dispenserScheduleTable.result)

  override def findById(id: Long): Future[Option[DispenserSchedule]] =
    db.run(dispenserScheduleTable.filter(_.id === id).result.headOption)

  override def save(entity: DispenserSchedule): Future[Int] =
    db.run(dispenserScheduleTable += entity)

  override def delete(id: Long): Future[Int] =
    db.run(dispenserScheduleTable.filter(_.id === id).delete)

  override def update(id: Long, entity: DispenserSchedule): Future[Int] = {
    val query = for {
      ds <- dispenserScheduleTable.filter(_.id === id).update(entity)
    } yield ds
    db.run(query)
  }
}
