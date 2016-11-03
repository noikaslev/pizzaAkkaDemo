package PizzaShopRouter

/*
  PizzaShop responsible for forwarding the orders to Order actor anf when pizza is ready deliver to clients
  In preStart created actors that will be used latter in our system
  PizzaShop is the supervisor of Order/Chef and stove actors.
  In case pizza fried by stove(~20% that will happen), a new pizza added to the order.
 */

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.routing.{RandomRoutingLogic, RoundRobinRoutingLogic, RoutingLogic, SmallestMailboxRoutingLogic}

import scala.collection.mutable


abstract class AbstractRouter(param: RouterParam) extends Actor with ActorLogging
case class RouterParam(noOfWorkers: Int, routingLogic: RoutingLogic)

object PizzaShop {
  val name = "PizzaShop"
  def props = Props(new PizzaShop)

  case class Delivered(orderName: String)
  class PizzaFried(msg: String) extends RuntimeException(msg)
}

class PizzaShop extends Actor with ActorLogging{
  import PizzaShop._

  var orderReqOffice: ActorRef = _
  override def preStart(): Unit = {
    orderReqOffice = context.actorOf(PizzaOrderRouter.props(RouterParam(2, RandomRoutingLogic())), name = PizzaOrderRouter.name)
    context.actorOf(PizzaChefRouter.props(RouterParam(3, SmallestMailboxRoutingLogic())), name = PizzaChefRouter.name)
    context.actorOf(StoveRouter.props(RouterParam(4, RoundRobinRoutingLogic())), name = StoveRouter.name)
  }

  var pizzaLeftToBakeForOrders = mutable.Map[String, Int]()

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3) {
    case pf: PizzaFried =>
      log.error(s"Pizza fried for order {}", pf.getMessage)

      val chefRouter = context.system.actorSelection(s"user/${PizzaShop.name}/${PizzaChefRouter.name}")
      chefRouter ! PizzaChefRouter.PizzaPrepareRequest(PizzaOrderRouter.OrderRequest(1, pf.getMessage))
      Resume
  }

  def receive = {
    case or: PizzaOrderRouter.OrderRequest =>
      log.info("received OrderRequest {}", or.name)
      pizzaLeftToBakeForOrders += (or.name -> or.noOfPizzas)
      orderReqOffice ! or

    case pd: Delivered =>
      val noPizzaLeft = pizzaLeftToBakeForOrders.get(pd.orderName) match {
        case Some(number) => number - 1
        case _ => 0
      }

      if (noPizzaLeft > 0)
        pizzaLeftToBakeForOrders.update(pd.orderName, noPizzaLeft)
      else {
        log.info("Order {} ready for delivery", pd.orderName)
      }
  }
}

