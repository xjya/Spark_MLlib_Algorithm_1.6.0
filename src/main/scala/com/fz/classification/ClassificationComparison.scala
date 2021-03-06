package com.fz.classification

import com.fz.util.Utils
import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.optimization.L1Updater

/**
 *
 * 分类算法对比，包括SVM，逻辑回归，随机森林，GradientBoostedTree，
 * NaiveBayes
 *
 * 模型路径： /path/to/model/output, 下面的模型路径指的就是这一级
 *              --data
 *                  -- _SUCCESS
 *                  -- _common_metadata
 *                  -- _metadata
 *                  -- part-r-....
 *              --metadata
 *                  -- _SUCCESS
 *                  -- part-00000
 *
 * 输入参数：
 * testOrNot : 是否是测试，正常情况设置为false
 * input：输入测试数据（包含label的数据）；
 * minPartitions: 输入数据最小partition个数
 * output：输出路径
 * targetIndex：目标列所在下标，从1开始
 * splitter : 输入数据分隔符
 * model1 : 模型1的路径
 * model2: 模型2的路径
 *
 * Created by fanzhe on 2017/1/25.
 */
//TODO 模型对比待完成
object ClassificationComparison {

   def main (args: Array[String]) {
    if(args.length != 8){
      println("Usage: com.fz.classification.ClassificationComparison testOrNot input minPartitions output targetIndex " +
        "splitter model1 model2")

      System.exit(-1)
    }
     val testOrNot = args(0).toBoolean // 是否是测试，sparkContext获取方式不一样, true 为test
     val input = args(1)
     val minPartitions = args(2).toInt
     val output = args(3)
     val targetIndex = args(4).toInt // 从1开始，不是从0开始要注意
     val splitter = args(5)
     val model1Path = args(6)
     val model2Path = args(7)

     // 删除输出，不在Scala算法里面删除，而在Java代码里面删除
     //     Utils.deleteOutput(output)

     val sc =  Utils.getSparkContext(testOrNot,"Classification Algorithm Comparison")

     // construct data
     // Load and parse the data
     val training = Utils.getLabeledPointData(sc,input,minPartitions,splitter,targetIndex).cache()
     val numCLasses = Utils.modelParam(sc,model1Path,"numClasses").toInt
     if( numCLasses != Utils.modelParam(sc,model2Path,"numClasses").toInt){
       System.err.println("模型1："+model1Path+"和 模型2："+model2Path+" 类别不匹配，请检查metadata!")
       System.exit(2)
     }

     // use model to predict
     val preAndReal1 = Utils.useModel2Predict(sc,model1Path,training)

     val preAndReal2 = Utils.useModel2Predict(sc,model2Path,training)

     // model1 evaluation
     val evaluate1 = Utils.evaluate(preAndReal1,numCLasses)
     val evaluate2 = Utils.evaluate(preAndReal2,numCLasses)


     // save result
     sc.parallelize(Array(evaluate1)).saveAsTextFile(output+"/model1")
     sc.parallelize(Array(evaluate2)).saveAsTextFile(output+"/model2")

     sc.stop()
  }
}
