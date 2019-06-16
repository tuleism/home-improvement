package com.github.tuleism.home.process

import scala.collection.mutable

// TODO fix when it becomes too inefficient
class ProcessLogger extends scala.sys.process.ProcessLogger {
  private val _outs = mutable.ListBuffer.empty[String]
  private val _errs = mutable.ListBuffer.empty[String]

  def out(s: => String): Unit = _outs.append(s)
  def err(s: => String): Unit = _errs.append(s)
  def buffer[T](f: => T): T = f

  def outs: List[String] = _outs.toList
  def errs: List[String] = _errs.toList
}
