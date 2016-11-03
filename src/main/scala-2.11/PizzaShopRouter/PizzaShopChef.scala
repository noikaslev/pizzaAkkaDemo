package PizzaShopRouter

/*
  PizzaChefRouter is creating workers. Number of workers are given in constructor.
  The work distributed by SmallestMailboxRoutingLogic. Choosing the least busy chef and pushing the order.
  Worker preparing all pizzas and telling to StoveRouter to bake
 */

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing._

object PizzaChefRouter {
  val name = "ChefRouter"
  val workerProps = Props[PizzaChefWorker]
  def props(param: RouterParam) = Props(new PizzaChefRouter(param, workerProps))

  case class PizzaPrepareRequest(orderRequest: PizzaOrderRouter.OrderRequest)
}

class PizzaChefRouter(param: RouterParam, workerProps: Props) extends AbstractRouter(param) {
  import PizzaChefRouter._

  log.info("Creating workers")
  var noWorker = 1
  val routees = Vector.fill(param.noOfWorkers) {
    val r = context.actorOf(workerProps, name = s"ChefWorker_$noWorker")
    noWorker += 1
    context watch r
    ActorRefRoutee(r)
  }

  //SmallestMailboxRoutingLogic
  var chefRouter = Router(param.routingLogic, routees)

  def receive = {
    case pr: PizzaPrepareRequest =>
      chefRouter.route(pr, sender)
  }
}

class PizzaChefWorker extends Actor with ActorLogging {
  import PizzaChefRouter._

  def receive = {
    case pr: PizzaPrepareRequest =>
      log.info("Preparing {} order", pr.orderRequest.name)
      val stoveRouter = context.system.actorSelection(s"user/${PizzaShop.name}/${StoveRouter.name}")
      stoveRouter ! StoveRouter.FetchStove(pr.orderRequest)
  }
}