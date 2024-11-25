package be.unamur.cattracker.repositories


import java.time.{LocalDateTime, LocalTime}
import scala.concurrent.Future

/**
 * Trait representing the method signatures a basic repository should have
 * @tparam Entity the entity type
 * @tparam Id the id type
 */
trait BaseRepository[Entity, Id] {
  def findAll(): Future[Seq[Entity]]
  def findById(id: Id): Future[Option[Entity]]
  def save(entity: Entity): Future[Int]
  def delete(id: Id): Future[Int]
}

/**
 * Traits that is an abstraction of a sensor repository.
 * This could be useful since it makes the code more flexible about the database implementation (we are not
 * depending on a specific database implementation).
 *
 * @tparam Entity the entity type (could change depending on the database implementation)
 * @tparam Id the id type
 */
trait SensorBaseRepository[Entity, Id] extends BaseRepository[Entity, Id] {
  def findForSensorBetween(sensor: String, start: LocalDateTime, end: LocalDateTime): Future[Seq[Entity]]
}

trait DispenserScheduleBaseRepository[Entity, Id] extends BaseRepository[Entity, Id] {
  def findByLabelContains(label: String): Future[Seq[Entity]]
  def update(id: Long, entity: Entity): Future[Int]
}