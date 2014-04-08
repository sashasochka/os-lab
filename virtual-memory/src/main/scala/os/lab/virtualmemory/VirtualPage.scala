package os.lab.virtualmemory

import Simulation._

class VirtualPage(val pid: Int, val index: Int) {
  private[this] var _physicalPageIndex: Option[Int] = None
  private[this] var _movedToSwap = false
  private[this] var _loadedFromSwap = false

  def isLoadedFromSwap = _loadedFromSwap

  def physicalPageIndex_=(pageIndex: Int) = {
    _loadedFromSwap ||= _movedToSwap
    _physicalPageIndex = Some(pageIndex)
  }

  def physicalPageIndex(implicit time: Int): Int = {
    if (_physicalPageIndex.isEmpty) WorkingSetAlgorithm(this)
    _physicalPageIndex match {
      case Some(x) => x
      case None => sys.error(s"no physical mapping for VirtualPage with PID = $pid and index = $index")
    }
  }

  def resetPhysicalPageIndex() =
    _physicalPageIndex = None

  def moveToSwap() =
    _movedToSwap = true
    _physicalPageIndex = None
}
