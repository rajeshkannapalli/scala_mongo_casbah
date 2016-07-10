

import com.mongodb.casbah.Imports._
import Comic._
import OTopic._
import faker._

/**
 * Created by Rajeshkumar on 6/29/2016.
 */
class ComicBazaar() {
  // Define Variables to connect to Database
  val mongoClient = MongoClient("localhost", 27017)
  val database = mongoClient("scalatest")
  var collectionUsers = database("users")
  var collectionComics = database("comics")
  var collectionTopics = database("topics")

  /**
    * Adds User object to the database.
    * @param user  User object
    */
  def addUser(user: User)  =  {
    val name: String = user.name
    val userObj = MongoDBObject("user" -> s"$name")
    try{
      val result = collectionUsers.insert(userObj,WriteConcern.Acknowledged)
      println("Hi " + name + "! Welcome to ComicBazaar")
    }
    catch{
      case ex: Exception =>{
        println("Failed to add user " + name)
        println(ex.printStackTrace())

      }

    }

  }


  /**
    * Removes User object from the database.
    * Also removes user from subscribed topics
    * @param user  User object
    */
  def removeUser(user: User)  = {
    var name: String = user.name
    var query = MongoDBObject("user"-> name)
    val result = collectionUsers.findAndRemove ( query )
    if (result != None) {
      // Remove user from subscribed topics
      query = MongoDBObject()
      val result = collectionTopics.update(query,$pull("users"->name),false,true)
      println("User " + name + " is removed from ComicBazaar")
    } else {
      println("Failed to remove user " + name)

    }

  }

  /**
    * Checks if user is present in the database.
    * @param user  User object
    * @return boolean True if present, else false
    */
  def checkUser(user:User): Boolean ={
    var query = MongoDBObject("user"-> user.name)
    val result = collectionUsers.findOne( query )
    if (result != None) {
      return true
    }
    return false
  }

  /**
    * Unsubscribes user from provided topic only if user present in database.
    * @param user  User object
    * @param topic  OTopic object
    */
 def unsubscribe(user:User,topic:OTopic) = {
   if(!checkUser(user)){
     //if user is not present
     println("Failed to unsubscribe user " + user.name +" from topic " + topic.topicName.toString() + "->" + topic.topicValue.toString()+". Please check the query.")

   } else{
     //if user is present fetch topic
     val query = MongoDBObject("topicName"->s"${topic.topicName}")++("topicValue"->s"${topic.topicValue}")
     val queryResult = collectionTopics.findOne ( query )
     if (queryResult != None) {
       // topic is found
       val result = collectionTopics.update(query,$pull("users"->user.name))
       println("NOTIFICATION:- "+user.name + " is unsubscribed from topic " + topic.topicName.toString() + "->" + topic.topicValue.toString()+".")
     } else {
       // topic not present
       println("Failed to unsubscribe user " + user.name +" from topic " + topic.topicName.toString() + "->" + topic.topicValue.toString()+". Please check the query.")

     }
   }

 }

  /**
    * Subscribes user from provided topic only if user present in database.
    * @param user  User object
    * @param topic  OTopic object
    */
  def subscribeUserToTopic(user: User, topic: OTopic) = {
    // If user is not present add user
    if(!checkUser(user)) addUser(user)
    var query = MongoDBObject("topicName"->s"${topic.topicName}","topicValue"->s"${topic.topicValue}")
    val queryResult = collectionTopics.findOne ( query )
    try{
      if(queryResult != None) {
        // Topic is present hence update the list of users subscribed to topic
          val result = collectionTopics.update(query,$addToSet("users"->user.name))
      } else {
        // Topic is not present so add new topic
        query = MongoDBObject("topicName"->s"${topic.topicName}","topicValue"->s"${topic.topicValue}","users"->MongoDBList(List(user.name):_*))
        val result = collectionTopics.insert(query,WriteConcern.Acknowledged)
      }
      println("NOTIFICATION:- "+user.name + " is subscribed under topic " + topic.topicName.toString() + "->" + topic.topicValue.toString() )
    }
    catch{
      case ex: Exception =>{
        println("Failed to subscribe user " + user.name+" to topic "+topic.topicName+"->"+topic.topicValue)
        println(ex.printStackTrace())
      }

    }


  }

  /**
    * Put comic for sale in the market.
    * Notify interested users if and only if new comic else just add.
    * @param comic  Comic Object
    */
  def sellComic(comic: Comic) = {
    val comicObj = buildMongoDbComicObject(comic)
    try{
      val query = MongoDBObject("Name"->comic.Name)
      var numberOfComics = collectionComics.find(query).count()
      val result = collectionComics.insert(comicObj,WriteConcern.Acknowledged)
      println("Successfully added comic " + comic.Name)
      if(numberOfComics == 0){
        // if comic is new
        try notifyObservers(comic)
        catch{
          case ex: Exception =>{
            println("Failed to notify users for added comic " + comic.Name)
            println(ex.printStackTrace())

          }
        }
      }
    }
    catch{
      case ex: Exception =>{
        println("Failed to add comic " + comic.Name)
        println(ex.printStackTrace())
      }

    }

  }

  /**
    * Buy comic from market.
    * Notify interested users about the sold comic.
    * @param comic  Comic Object
    */
  def buyComic(comic:Comic)= {
    comic.setStatus("Sold")
    val query = MongoDBObject("Name"->comic.Name)
    val result = collectionComics.findAndRemove(query)
    if (result != None) {
      println("Comic " + comic.Name + " is sold.")
      notifyObservers(comic)
    } else {
      println("Failed to sell " + comic.Name)

    }

  }

  /**
    * Notify interested users based on topics related to comic.
    * @param comic  Comic Object
    */
  def notifyObservers(comic:Comic)={
    //fetch comic object iterable
    val comicMap : Map[String,String] = provideIterableComicObject(comic)
    var users :Set[String] = Set()
    // for each possible topic of comic
    for(tuple <- comicMap){
      var topicName:String = tuple._1
      var topicValue:String = tuple._2
      val query = MongoDBObject("topicName"->s"${topicName}")++("topicValue"->s"${topicValue}")
      var queryResult = collectionTopics.findOne( query )
      if (queryResult != None) {
        // topic is present
        var queryResult1 = collectionTopics.findOne( query).get.getAs[MongoDBList]("users").get
        var results = queryResult1
        for( result<-queryResult1) {
         users +=   result.toString
        }
      }
    }
//  Check if comic status is available or sold
    if(comic.getStatus() == "Available"){
      // if available notify users
      for( user<-users)  println("NOTIFICATION:- Hi "+user + ", a new comic '"+comic.Name+"' is available for sale.")
    }
    else{
      // if not available
      val query = MongoDBObject("Name"->comic.Name)
      var numberOfComics = collectionComics.find(query).count()
      if(numberOfComics == 0)
        // if comic is not available
        for( user<-users)  println("NOTIFICATION:- Hi "+user+ ", last copy of comic '"+comic.Name+"' has been sold.")
      else
      // if comic is still available after sale
        for( user<-users)  println("NOTIFICATION:- Hi "+user+ ", comic '"+comic.Name+"' has been sold. Hurry before stock lasts!")
    }

  }


  def close()= {
    mongoClient.close()
    println("Close")

  }
}

object ComicBazaar {
  def main(args: Array[String]) {

    var comicBazaar = new ComicBazaar
    println("Start")
    var user1 = new User(faker.Internet.user_name)
    var user2 = new User(faker.Internet.user_name)
    comicBazaar.addUser(user1)
    comicBazaar.addUser(user2)
    comicBazaar.removeUser(user1)
    comicBazaar.removeUser( new User(faker.Internet.user_name))
    val topic1 = new OTopic("Character","Batman")
    val topic2 = new OTopic("Author","Rajesh")
    comicBazaar.subscribeUserToTopic(user1,topic1)
    comicBazaar.subscribeUserToTopic(user2,topic2)
    comicBazaar.subscribeUserToTopic(user1,topic2)

//    comicBazaar.unsubscribe(user2,topic2)
//    comicBazaar.removeUser(user1)
    comicBazaar.unsubscribe( new User(faker.Internet.user_name),topic2)
    val comic = new Comic("Batman Begins","Batman","Rajesh","5/15/2009")
    val comic1 = new Comic("Batman Begins","Batman","Rajesh","5/15/2009")
    comicBazaar.sellComic(comic)
    comicBazaar.sellComic(comic1)
    comicBazaar.buyComic(comic)
    comicBazaar.buyComic(comic)
    comicBazaar.close()

  }
}
