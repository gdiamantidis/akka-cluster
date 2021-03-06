package com.gdiama.example.cluster.util.metrics

import akka.actor.Address
import akka.cluster.metrics.{ CapacityMetricsSelector, NodeMetrics }
import CustomMetric.KnsInUse

@SerialVersionUID(1L)
case object KmsCapacityMetricsSelector extends CapacityMetricsSelector {

  override def capacity(nodeMetrics: Set[NodeMetrics]): Map[Address, Double] = {
    nodeMetrics.collect {
      case KnsInUse(address, kmsInUse) =>
        val capacity = if (kmsInUse == 32) 0.0 else 1.0
        (address, capacity)
    }.toMap
  }
}
