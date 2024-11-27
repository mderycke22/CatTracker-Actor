package be.unamur.cattracker.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import be.unamur.cattracker.model.SensorValue
import be.unamur.cattracker.repositories.SensorBaseRepository
import scala.util.{Success, Failure}

import java.time.LocalDateTime
import scala.concurrent.Future

object SensorValueDbActor {

  sealed trait SensorValueDbCommand
  final case class Insert(ds: SensorValue, replyTo: ActorRef[InsertResult]) extends SensorValueDbCommand
  final case class Select(sensorType: String, startDate: LocalDateTime, endDate: LocalDateTime, replyTo: ActorRef[RetrievedResult]) extends SensorValueDbCommand
  final case class SelectAll(sensorType: String, replyTo: ActorRef[RetrievedResult]) extends SensorValueDbCommand

  sealed trait InsertResult
  sealed trait RetrievedResult

  final case class Inserted(value: Int) extends InsertResult
  final case class NotInserted(message: String) extends InsertResult
  final case class Retrieved(values: Seq[SensorValue]) extends RetrievedResult
  final case class NotRetrieved(message: String) extends RetrievedResult

  private final case class WrapperInsertedResult(result: InsertResult, replyTo: ActorRef[InsertResult]) extends SensorValueDbCommand
  private final case class WrapperRetrievedResult(result: RetrievedResult, replyTo: ActorRef[RetrievedResult]) extends SensorValueDbCommand

  def apply(repository: SensorBaseRepository[SensorValue, Long]): Behavior[SensorValueDbCommand] =
    Behaviors.setup { context =>
      import context.executionContext

      Behaviors.receiveMessage {
        case Insert(sv, replyTo) =>
          val insertFuture: Future[Int] = repository.save(sv)
          context.pipeToSelf(insertFuture) {
            case Success(v) => WrapperInsertedResult(Inserted(v), replyTo)
            case Failure(e) => WrapperInsertedResult(NotInserted(e.getMessage), replyTo)
          }
          Behaviors.same

        case Select(sensorType, startDate, endDate, replyTo) =>
          val selectFuture: Future[Seq[SensorValue]] = repository.findForSensorBetween(sensorType, startDate, endDate)
          context.pipeToSelf(selectFuture) {
            case Success(data) => WrapperRetrievedResult(Retrieved(data), replyTo)
            case Failure(e) => WrapperRetrievedResult(NotRetrieved(e.getMessage), replyTo)
          }
          Behaviors.same

        case SelectAll(sensorType, replyTo) =>
          val selectAllFuture: Future[Seq[SensorValue]] = repository.findAll()
          context.pipeToSelf(selectAllFuture) {
            case Success(data) => WrapperRetrievedResult(Retrieved(data), replyTo)
            case Failure(e) => WrapperRetrievedResult(NotRetrieved(e.getMessage), replyTo)
          }
          Behaviors.same

        case WrapperInsertedResult(data, replyTo) =>
          replyTo ! data
          Behaviors.same

        case WrapperRetrievedResult(value, replyTo) =>
          replyTo ! value
          Behaviors.same
      }
    }
}