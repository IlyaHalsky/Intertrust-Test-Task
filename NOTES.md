# Notes

## Goals
- Implement solution similar to Elastic + Kibana or Prometheus + Grafana
- Make use of `akka-persistence`

## Simulator
Simulator runs from and to defined time, with provided tick rate (real world time) and time per tick (log time).

Due to it being simulation we can't really use build in timers for actors other than main simulator.

## Persistence
The only actor without persistence is Clock actor, since it drives simulation. Other actors will perform recovery from
journal and snapshots.

If you want to do a clean run remove folders `/target/journal` and `/target/snapshots`

## Serialization
Due to `jackson` serialization working strange with case object I've had to implement custom `Serializer` and `Desirializer`.
Otherwise `jackson` creates new instance of `case object` and it doesn't match against instances from code.

I've changed quite a bit `protocol.Events` for serialization purposes, hopefully it's not against rules.
On the same subject: created multiple traits just for actors, since Scala 2 doesn't support union types.