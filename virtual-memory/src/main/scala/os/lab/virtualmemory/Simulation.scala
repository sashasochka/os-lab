package os.lab.virtualmemory

import scala.util.Random

object Simulation extends App {
  type Time = Int

  // Settings
  val nPhysicalPagesTotal = 30
  val nProcesses = 7
  val simulationDurationTicks = 4000
  val ticksPerQuantum = 100
  val ticksPerWorkingSet = 50
  val ticksBetweenAccesses = 5
  val ticksBetweenClearingAccessedBits = 1000

  val deltaT = 100 // FIXME

  val physicalPages = for (_ <- 1 to nPhysicalPagesTotal) yield new PhysicalPage()
  val processes = for (pid <- 1 to nProcesses) yield new Process(pid)

  var nPhysicalPagesUsed = 0
  var nPageFaults = 0
  var nAccesses = 0
  var currentProcessIndex = 0
  var currentPhysicalPageIndex = 0

  def currentProcess = processes(currentProcessIndex)

  def currentPhysicalPage = physicalPages(currentPhysicalPageIndex)

  logSettings()
  for (tick <- 1 to simulationDurationTicks) {
    implicit val ticks: Time = tick // for implicit time passing

    val quantumFinished = ticks % ticksPerQuantum == 0
    val shouldChangeWorkingSet = ticks % ticksPerWorkingSet == 0
    val shouldAccess = ticks % ticksBetweenAccesses == 0
    val shouldClearAccessBits = ticks % ticksBetweenClearingAccessedBits == 0

    if (shouldClearAccessBits) {
      println("-" * 20)
      println("kernel thread for updating accessed flags")
      for (page <- physicalPages) {
        if (page.accessed) {
          page.lastAccessTime = tick
        }
        page.resetAccessed()
        page.resetWriteAccessed()
      }
    }
    if (quantumFinished) {
      currentProcessIndex = (currentProcessIndex + 1) % nProcesses
    }
    if (shouldChangeWorkingSet) {
      currentProcess.changeWorkingSet()
    }
    if (shouldAccess) {
      nAccesses += 1
      val page = currentProcess.accessRandomPage
      log(currentProcess.pid, page.index)
    }
  }

  /**
   *
   * @param virtualPage Virtual page for which physical page is searched
   * @param time Current time
   * @return The index of a physical page `virtualPage` can now be mapped to
   */
  def WorkingSetAlgorithm(virtualPage: VirtualPage)(implicit time: Time) = {
    println("Paging algorithm called")
    nPageFaults += 1
    if (nPhysicalPagesUsed < nPhysicalPagesTotal) {
      link(nPhysicalPagesUsed, virtualPage)
      nPhysicalPagesUsed += 1
    } else {
      // On every page fault, the page table is scanned to look for a suitable page to evict.
      // As each entry is processed, the R bit is examined.
      var oldestPageIndex = Random.nextInt(physicalPages.size)
      var pageFound = false
      for ((page, pageIndex) <- physicalPages.zipWithIndex) {
        if (page.accessed) {
          // If it is 1, the current virtual time is written into the Time of last use field in the page table,
          // indicating that the page was in use at the time the fault occurred. Since the page has been referenced during the current clock tick,
          // it is clearly in the working set and is not a candidate for removal (t is assumed to span multiple clock
          // ticks).
          page.lastAccessTime = time
        } else if (time - page.lastAccessTime > deltaT && !pageFound) {
          // If R is 0, the page has not been referenced during the current clock tick and may be a candidate for removal.
          // To see whether or not it should be removed, its age, that is,
          // the current virtual time minus its Time of last use is computed and compared to t.
          // If the age is greater than t, the page is no longer in the working set. It is reclaimed and the new page loaded here.
          // The scan continues updating the remaining entries, however.
          link(pageIndex, virtualPage)
          pageFound = true
        } else {
          // However, if R is 0 but the age is less than or equal to t, the page is still in the working set.
          // The page is temporarily spared, but the page with the greatest age (smallest value of Time of last use) is noted.
          oldestPageIndex =
              if (page.lastAccessTime > physicalPages(oldestPageIndex).lastAccessTime) oldestPageIndex
              else pageIndex
        }
      }
      if (!pageFound) {
        // If the entire table is scanned without finding a candidate to evict,
        // that means that all pages are in the working set.
        // In that case, if one or more pages with R = 0 were found, the one with the greatest age is evicted.
        link(oldestPageIndex, virtualPage)

        // In the worst case, all pages have been referenced during the current clock tick (and thus all have R = 1),
        // so one is chosen at random for removal, preferably a clean page, if one exists.
      }
    }
  }

  def link(physicalPageIndex: Int, virtualPage: VirtualPage) = {
    for (page <- physicalPages(physicalPageIndex).virtualPage)
      page.resetPhysicalPageIndex()
    physicalPages(physicalPageIndex).virtualPage = Some(virtualPage)
    virtualPage.physicalPageIndex = physicalPageIndex
    println(s"Link physical page #$physicalPageIndex <-> virtual page #${virtualPage.index}")
  }

  def logSettings() = {
    println(s"Number of physical pages in the system: $nPhysicalPagesTotal")
    println(s"Number of processes in the system: $nProcesses")
    println(s"Simulation duration in ticks: $simulationDurationTicks")
    println(s"Ticks per quantum: $ticksPerQuantum")
    println(s"Delta t: $deltaT")
    println()
  }

  def log(pid: Int, virtualPageIndex: Int)(implicit tick: Time) {
    println("=" * 20)
    println(s"Working time: $tick")
    println(s"Total number of page faults: $nPageFaults")
    println(s"Total number of accesses: $nAccesses")
    println(s"Accessed virtual page #$virtualPageIndex, from process #$pid")
    println("-" * 20)
    for ((physicalPage, pageNumber) <- physicalPages.zipWithIndex) {
      println(s"Physical page number: $pageNumber")
      physicalPage.virtualPage match {
        case Some(page) =>
          println(s"Mapped to virtual page #${page.index} from process #${page.pid}\t" +
            s"Last access time: ${physicalPage.lastAccessTime} Accessed: ${physicalPage.accessed}\t" +
            s"Loaded from swap: ${page.isLoadedFromSwap}")
        case None =>
          println("Not mapped to virtual page")
      }
    }
    println()
  }
}
