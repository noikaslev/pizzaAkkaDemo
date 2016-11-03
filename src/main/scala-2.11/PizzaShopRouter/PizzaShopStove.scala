package PizzaShopRouter

/*
  StoveRouter is creating workers. Number of workers are given in constructor.
  The work distributed by RoundRobinRoutingLogic. Each pizza enters to the next stove.
  Worker baking the pizza and deliver it to pizzaShop
 */

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import akka.routing._

object StoveRouter {
  val name = "StoveRouter"
  val workerProps = Props[StoveWorker]
  def props(param: RouterParam) = Props(new StoveRouter(param, workerProps))

  case class FetchStove(orderRequest: PizzaOrderRouter.OrderRequest)
}

class StoveRouter(param: RouterParam, workerProps: Props) extends AbstractRouter(param) {
  import StoveRouter._

  log.info("Creating workers")
  var noWorker = 1
  val routees = Vector.fill(param.noOfWorkers) {
    val r = context.actorOf(workerProps, name = s"StoveWorker_$noWorker")
    noWorker += 1
    context watch r
    ActorRefRoutee(r)
  }

  //Override the default behaviour of supervisorStrategy(restart the actor)
  override val supervisorStrategy = OneForOneStrategy() {
    case _: PizzaShop.PizzaFried =>
      Escalate //Let the pizzaShop deal with exception
  }

  //RoundRobinRoutingLogic
  val stoveRouter = Router(param.routingLogic, routees)

  def receive = {
    case fs: FetchStove =>
      stoveRouter.route(fs, sender)

      if (fs.orderRequest.noOfPizzas > 1) {
        self.tell(fs.copy(orderRequest = PizzaOrderRouter.OrderRequest(fs.orderRequest.noOfPizzas-1, fs.orderRequest.name)), sender)
      }
  }
}

class StoveWorker extends Actor with ActorLogging {
  import StoveRouter._
  import util.Random

  def receive = {
    case fs: FetchStove =>
      val orderName = fs.orderRequest.name
      log.info("Baking pizza for order: {}", orderName)
      Thread.sleep(2000)

      //~20% of pizzas fried
      if (Random.nextInt(10)>7) {
        log.info("Worker Fried pizza for order: {}", orderName)
        throw new PizzaShop.PizzaFried(orderName)
      }

      val pizzaShop = context.system.actorSelection(s"user/${PizzaShop.name}")
      pizzaShop ! PizzaShop.Delivered(orderName)
  }
}
