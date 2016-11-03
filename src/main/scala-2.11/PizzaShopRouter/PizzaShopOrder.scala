package PizzaShopRouter

/*
  PizzaOrderRouter is creating workers. Number of workers are given in constructor.
  The work distributed by RandomRoutingLogic. Picking randomly on of the workers and pushing the order.
  Worker telling to ChecfRouter to prepare the order
 */

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing._

object PizzaOrderRouter {
  val name = "OrderRouter"
  val workerProps = Props[PizzaOrderHandlingWorker]
  def props(param: RouterParam) = Props(new PizzaOrderRouter(param, workerProps))

  case class OrderRequest(noOfPizzas: Int, name: String)
}

class PizzaOrderRouter(param: RouterParam, workerProps: Props) extends AbstractRouter(param) {
  import PizzaOrderRouter._

  log.info("Creating workers")
  var noWorker = 1
  val routees = Vector.fill(param.noOfWorkers) {
    val r = context.actorOf(workerProps, name = s"OrderHandlingWorker_$noWorker")

    noWorker += 1
    context watch r
    ActorRefRoutee(r)
  }

  //RandomRoutingLogic
  var orderRouter = Router(param.routingLogic, routees)

  def receive = {
    case or: OrderRequest =>
      orderRouter.route(or, sender)
  }
}

class PizzaOrderHandlingWorker extends Actor with ActorLogging {
  import PizzaOrderRouter._

  def receive = {
    case or: OrderRequest =>
      log.info("Processing {} order, should bake {} pizzas", or.name, or.noOfPizzas)

      val chefRouter = context.system.actorSelection(s"user/${PizzaShop.name}/${PizzaChefRouter.name}")
      chefRouter ! PizzaChefRouter.PizzaPrepareRequest(or)
  }
}