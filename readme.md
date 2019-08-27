Queuing Product/Order service with throttling and bulking features

How to run:
1. Open cmd line
2. cd in root project directory `queuing-service`
3. print command `sbt run`

* AQS-1: see `ProductApi.scala` + `OrderApi.scala`
* AQS-2, AQS-3: see ApiDispatcher.scala with messages `HttpRqs`, `AsyncApiCallR`, `Push`
* AQS-4: see ApiDispatcher.scala with message `Tick`