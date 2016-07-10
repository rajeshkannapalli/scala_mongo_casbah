
import com.mongodb.casbah.Imports._
/**
  * Created by Rajeshkumar on 7/5/2016.
  */

  class OTopic(val topicName:String, val topicValue:String) { }

object OTopic {
  /**
    * Convert a Comic object into a BSON format that MongoDb can store.
    */
  def buildMongoDbTopicObject(topic: OTopic): MongoDBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "topicName" -> topic.topicName
    builder += "topicValue" -> topic.topicValue
    builder.result
  }
}

