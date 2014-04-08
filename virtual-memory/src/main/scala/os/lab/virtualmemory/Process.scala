package os.lab.virtualmemory

import scala.util.Random
import Simulation._

class Process(val pid: Int) {
  private[this] val nVirtualPages = Random.nextInt(nPhysicalPagesTotal) * 3 / 2 + 1

  private[this] val virtualPages =
    for (pageIndex <- 0 until nVirtualPages) yield new VirtualPage(pid, pageIndex)

  private[this] var workingSet = randomWorkingSet

  /**
   * Generates new random working set of pages
   * @return Random working set
   */
  def randomWorkingSet = {
    val nWorkingSetPages = 2 + Random.nextInt(nVirtualPages / 20 + 1)
    Random shuffle virtualPages take nWorkingSetPages
  }

  /**
   * Changes working set of the current process to a new random set
   */
  def changeWorkingSet() =
    workingSet = randomWorkingSet

  /**
   * Access random page with 90% probability of locality of reference principle applied
   * @return Accessed page
   */
  def accessRandomPage(implicit time: Time) = {
    // 90/10 probability locality of reference applied
    val localityOfReferenceApplied = Random.nextInt(10) < 9
    val setOfPagesToAccess = if (localityOfReferenceApplied) workingSet else virtualPages
    val page = setOfPagesToAccess(Random.nextInt(setOfPagesToAccess.size))
    accessPage(page)
    page
  }

  /**
   * Access `virtualPage` page with 50/50 probability of read/write operation
   * @param virtualPage Page to be accessed
   */
  def accessPage(virtualPage: VirtualPage)(implicit time: Time) = {
    val page = physicalPages(virtualPage.physicalPageIndex)
    // 50/50 read/write operations
    val accessType = if (Random.nextBoolean()) PhysicalPage.Read else PhysicalPage.Write
    page.access(accessType)
  }
}
