package os.lab.virtualmemory

import Simulation._
import PhysicalPage._

class PhysicalPage() {
  private[this] var _modified = false
  private[this] var _accessed = false
  var lastAccessTime: Time = 0
  var virtualPage: Option[VirtualPage] = None

  def accessed = _accessed
  def modified = _modified

  def resetAccessed() =
    _accessed = false

  def resetWriteAccessed() =
    _modified = false

  def access(accessType: AccessType) = {
    _accessed = true
    _modified ||= accessType == Write
  }
}

object PhysicalPage {
  abstract class AccessType
  object Read extends AccessType
  object Write extends  AccessType
}
