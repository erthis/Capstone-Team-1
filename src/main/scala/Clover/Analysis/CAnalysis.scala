package Clover.Analysis

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
class CAnalysis(spark:SparkSession) {
  import spark.implicits._
  val tempdf: DataFrame = spark.read.format("parquet")
    .option("delimiter", ",")
    .option("header", "true")
    .option("infer.schema","true")
    .load("src/main/scala/Clover/data/files/ConsumedData").toDF("order_id","customer_id","customer_name"
    ,"product_id","product_name","product_category","datetime","price","qty","country","city","ecommerce_website_name"
    ,"payment_txn_id","payment_txn_success","nothing","nothing2")

  //tempdf.createOrReplaceTempView("temp2")
  //spark.sql("SELECT * FROM temp2 ORDER BY order_id").show()

  val df = tempdf.withColumn("newValue",regexp_replace(col("price"),"\\$",""))
    .select(
      col("order_id").cast("int"),
      col("customer_id"),
      col("customer_name"),
      col("product_id").cast("int"),
      col("product_name"),
      col("product_category"),
      col("qty").cast("int"),
      col("newValue").cast("double"),
      col("datetime"),col("country"),
      col("city"),
      col("ecommerce_website_name"),
      col("payment_txn_id"),
      col("payment_txn_success"))
    .withColumnRenamed("newValue","price")
    .filter(col("datetime")==="2022-03-03 12:47:25")
    .distinct()
  df.createOrReplaceTempView("temp")
  //spark.sql("SELECT product_category,SUM(price) sum FROM temp GROUP BY product_category ORDER BY sum").show()
  val customers: Dataset[team2Customer] = df.select(col("customer_id"),col("customer_name"),col("country"),col("city"))
    .distinct()
    .filter(x=>{x.getString(2) != "Mozambique"})
    .as[team2Customer]
  val products: Dataset[team2Product] = df.select(col("product_id"),col("product_category"),col("product_name"),col("price"))
    .distinct()
    .as[team2Product]
  val transactions: Dataset[team2Transaction] = df.select(col("order_id"),col("payment_txn_id"),col("payment_txn_success"),col("datetime"))
    .distinct()
    .as[team2Transaction]

  //val idTest = customers.map(x=>{(x.customer_id.substring(0,2),x.customer_id.substring(3))})
  /*val idTest = customers.withColumn("initial",regexp_extract(col("customer_id"),"\\w\\w",0))
    .withColumn("id",regexp_extract(col("customer_id"),"[^\\D]\\w+",0))
    .as[idTestC]*/



  //idTest.orderBy(asc("customer_name")).createOrReplaceTempView("idTest")

  customers.createOrReplaceTempView("customers")
  products.createOrReplaceTempView("products")
  transactions.createOrReplaceTempView("transactions")
  spark.sql("SELECT c.customer_id,c.customer_name,m.product_id,m.product_category,m.payment_txn_success FROM temp m JOIN customers c on c.customer_id = m.customer_id " +
    "ORDER BY c.customer_name ASC").show(1000, false)
  //spark.sql("SELECT customer_id,customer_name FROM customers WHERE country='Mozambique'").show()
  //spark.sql("SELECT country,COUNT(country),city,COUNT(city) FROM customers GROUP BY country,city").show()
  //spark.sql("SELECT country,city FROM customers WHERE country= 'Mozambique'").show(Int.MaxValue)
  //spark.sql("SELECT product_category,COUNT(product_category) FROM products GROUP BY product_category").show()
  //spark.sql("SELECT customer_name, product_name, product_category FROM temp ORDER BY customer_name").show(Int.MaxValue)
  //spark.sql("SELECT customer_id,customer_name FROM customers ORDER BY customer_id").show(Int.MaxValue)
}
case class team2Row(order_id: Int,customer_id: Option[String],customer_name: Option[String],product_id: Option[Int]
                   ,product_name: Option[String], product_category: Option[String], qty: Option[Int], price: Option[Double]
                   ,datetime: Option[String],country: Option[String],city: Option[String],ecommerce_website_name: Option[String]
                   ,payment_txn_id: Option[String],payment_txn_success: Option[String])
case class team2Transaction(order_id:Int,payment_txn_id:String,payment_txn_success:String,datetime:String)
case class team2Customer(customer_id:String,customer_name:String,country:String,city:String)
case class team2Product(product_id:Int,product_category:String,product_name:String,price:Double)
case class idTestC(customer_id:String,customer_name:String,country:String,city:String,initial:String,id:Int)