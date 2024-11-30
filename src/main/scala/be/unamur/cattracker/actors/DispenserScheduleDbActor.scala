package be.unamur.cattracker.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import be.unamur.cattracker.model.{DispenserSchedule, DispenserScheduleUpdateDTO}
import be.unamur.cattracker.repositories.DispenserScheduleBaseRepository
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.Future
import scala.util.{Failure, Success}

object DispenserScheduleDbActor {

  sealed trait DispenserScheduleDbCommand
  final case class Insert(ds: DispenserSchedule, replyTo: ActorRef[InsertResult]) extends DispenserScheduleDbCommand
  final case class Select(label: Option[String], replyTo: ActorRef[RetrieveResult]) extends DispenserScheduleDbCommand
  final case class Update(id: Long, dto: DispenserScheduleUpdateDTO, replyTo: ActorRef[UpdateResult]) extends DispenserScheduleDbCommand
  final case class Delete(id: Long, replyTo: ActorRef[DeleteResult]) extends DispenserScheduleDbCommand

  sealed trait InsertResult
  sealed trait RetrieveResult
  sealed trait UpdateResult
  sealed trait DeleteResult

  final case class Inserted(value: Int) extends InsertResult
  final case class NotInserted(message: String) extends InsertResult
  final case class Retrieved(values: Seq[DispenserSchedule]) extends RetrieveResult
  final case class NotRetrieved(message: String) extends RetrieveResult
  final case class Updated(value: Int) extends UpdateResult
  final case class NotUpdated(message: String) extends UpdateResult
  final case class Deleted(value: Int) extends DeleteResult
  final case class NotDeleted(message: String) extends DeleteResult

  private final case class WrappedInsertedResult(result: InsertResult, replyTo: ActorRef[InsertResult]) extends DispenserScheduleDbCommand
  private final case class WrappedSelectResult(result: RetrieveResult, replyTo: ActorRef[RetrieveResult]) extends DispenserScheduleDbCommand
  private final case class WrappedUpdateResult(result: UpdateResult, replyTo: ActorRef[UpdateResult]) extends DispenserScheduleDbCommand
  private final case class WrappedDeleteResult(result: DeleteResult, replyTo: ActorRef[DeleteResult]) extends DispenserScheduleDbCommand

  def apply(repository: DispenserScheduleBaseRepository[DispenserSchedule, Long]): Behavior[DispenserScheduleDbCommand] =
    Behaviors.setup { context =>
      import context.executionContext

      Behaviors.receiveMessage {
        case Insert(ds, replyTo) =>
          val insertFuture: Future[Int] = repository.save(ds)
          context.pipeToSelf(insertFuture) {
            case Success(v) => WrappedInsertedResult(Inserted(v), replyTo)
            case Failure(e) => WrappedInsertedResult(NotInserted(e.getMessage), replyTo)
          }
          Behaviors.same

        case Select(label, replyTo) =>
          val selectFuture: Future[Seq[DispenserSchedule]] = label match {
            case Some(l) => repository.findByLabelContains(l)
            case None => repository.findAll()
          }

          context.pipeToSelf(selectFuture) {
            case Success(data) => WrappedSelectResult(Retrieved(data), replyTo)
            case Failure(e) => WrappedSelectResult(NotRetrieved(e.getMessage), replyTo)
          }
          Behaviors.same

        case Update(id, dto, replyTo) =>
          val updateFuture: Future[Int] = repository.update(id, DispenserSchedule(id, dto.distributionTime, dto.kibblesAmountValue, dto.label))

          context.pipeToSelf(updateFuture) {
            case Success(v) => {
              v match {
                case 0 => WrappedUpdateResult(NotUpdated("No matching schedule"), replyTo)
                case _ => WrappedUpdateResult(Updated(v), replyTo)
              }
            }
            case Failure(e) =>
              WrappedUpdateResult(NotUpdated(e.getMessage), replyTo)
          }
          Behaviors.same

        case Delete(id, replyTo) =>
          val deleteFuture: Future[Int] = repository.delete(id)
          context.pipeToSelf(deleteFuture) {
            case Success(v) => WrappedDeleteResult(Deleted(v), replyTo)
            case Failure(e) => WrappedDeleteResult(NotDeleted(e.getMessage), replyTo)
          }
          Behaviors.same

        case WrappedInsertedResult(data, replyTo) =>
          replyTo ! data
          Behaviors.same

        case WrappedSelectResult(data, replyTo) =>
          replyTo ! data
          Behaviors.same

        case WrappedUpdateResult(data, replyTo) =>
          replyTo ! data
          Behaviors.same

        case WrappedDeleteResult(data, replyTo) =>
          replyTo ! data
          Behaviors.same
      }
    }
}