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
  final case class Insert(ds: DispenserSchedule, replyTo: ActorRef[Future[Int]]) extends DispenserScheduleDbCommand
  final case class Select(label: Option[String], replyTo: ActorRef[Future[Seq[DispenserSchedule]]]) extends DispenserScheduleDbCommand
  final case class Update(id: Long, dto: DispenserScheduleUpdateDTO, replyTo: ActorRef[Future[Int]]) extends DispenserScheduleDbCommand
  final case class Delete(id: Long, replyTo: ActorRef[Future[Int]]) extends DispenserScheduleDbCommand

  def apply(repository: DispenserScheduleBaseRepository[DispenserSchedule, Long]): Behavior[DispenserScheduleDbCommand] =
    Behaviors.setup { context =>
      import context.executionContext

      Behaviors.receiveMessage {
        case Insert(ds, replyTo) =>
          val insertFuture: Future[Int] = repository.save(ds)
          replyTo ! insertFuture
          Behaviors.same

        case Select(label, replyTo) =>
          val selectFuture: Future[Seq[DispenserSchedule]] = label match {
            case Some(l) => repository.findByLabelContains(l)
            case None => repository.findAll()
          }
          replyTo ! selectFuture
          Behaviors.same

        case Update(id, dto, replyTo) =>
          val updateFuture: Future[Int] = repository.update(id, DispenserSchedule(id, dto.distributionTime, dto.kibblesAmountValue, dto.label))
          replyTo ! updateFuture
          Behaviors.same

        case Delete(id, replyTo) =>
          val deleteFuture: Future[Int] = repository.delete(id)
          replyTo ! deleteFuture
          Behaviors.same
      }
    }
}