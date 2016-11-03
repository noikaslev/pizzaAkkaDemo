package PizzaShopRouter


/*
  * This is a demo of Router/Routees pattern in Akka toolkit
  * The input for this actor system is an pizza order
  *
  * Participant actors:
   * PizzaShop - Like front end of the pizza
   * PizzaShopOrder - Taking the orders of clients
   * PizzaShopChef - preparing the all pizzas of the order
   * PizzaShopStove - baking a pizza
  */

import akka.actor.ActorSystem

object PizzaShopApp extends App {
  val as = ActorSystem("pizza-shop")
  val shop = as.actorOf(PizzaShop.props, name=PizzaShop.name)

  val orderFirstReq = PizzaOrderRouter.OrderRequest(5, "First")
  val orderSecondReq = PizzaOrderRouter.OrderRequest(3, "Second")

  shop ! orderFirstReq
  shop ! orderSecondReq
}