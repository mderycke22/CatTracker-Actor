package be.unamur.cattracker

object CatTrackerConstants {
  val publishTopics: Map[String, String] = Map(
    "weight" -> "cattracker/weight/reset",
    "kibbles" -> "cattracker/kibbles/distribution"
  )
  
  val subscribeTopics: List[String] = List(
    "cattracker/weight/sensor_outputs",
    "cattracker/temp_hum/sensor_outputs",
    "cattracker/kibbles/sensor_outputs"
  )
}
