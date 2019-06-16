package com.github.tuleism.home.process

import scala.collection.mutable

class ProcessLogger extends scala.sys.process.ProcessLogger {
  private val _errs = mutable.ListBuffer.empty[String]

  def out(s: => String): Unit = {}
  def err(s: => String): Unit = _errs.append(s)
  def buffer[T](f: => T): T = f

  def errs: List[String] = _errs.toList
}
