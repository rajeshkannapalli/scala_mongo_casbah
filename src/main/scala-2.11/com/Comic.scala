/**
  * Created by Rajeshkumar on 6/28/2016.
  */

import com.mongodb.casbah.Imports._
/**
  * Comic class
  * @param Name : comic name
  * @param Character : comic character
  * @param Author : comic Author
  * @param Date : comic date
  */
class Comic(val Name: String,val Character:String,val Author:String,val Date:String) {
  private var Status : String = "Available"
  def getStatus() = Status
  def setStatus(status:String) = Status = status

}

object Comic {
  /**
    * Convert a Comic object into a BSON format that MongoDb can store.
    */
  def buildMongoDbComicObject(comic: Comic): MongoDBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "Name" -> comic.Name
    builder += "Character" -> comic.Character
    builder += "Author" -> comic.Author
    builder += "Date" -> comic.Date
    builder += "Status" -> comic.Status
    builder.result
  }

  /**
    * Convert a Comic object into an iterable map
    */
  def provideIterableComicObject(comic:Comic): Map[String,String] = {
    var comicMap : Map[String,String] = Map("Name"->comic.Name)
    comicMap = comicMap ++ Map("Character" -> comic.Character)
    comicMap = comicMap ++ Map( "Author" -> comic.Author)
    comicMap = comicMap ++ Map("Date" -> comic.Date)
    return comicMap
  }
}