package lendloop.core.locker

data class LockerStation(
    val id: String,
    val stationName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val lockerCount: Int,
    val availableCount: Int,
    val workingTimesJson: String,
    val lockers: List<LockerBox>? = null   // filled only on detail fetch
) {
    val status: LockerStatus
        get() = when {
            lockerCount == 0           -> LockerStatus.EMPTY
            availableCount == 0        -> LockerStatus.FULL
            availableCount < lockerCount -> LockerStatus.PARTIALLY_AVAILABLE
            else                       -> LockerStatus.AVAILABLE
        }
}

data class LockerBox(
    val id: String,
    val boxNumber: Short,
    val maxWeight: Double,
    val available: Boolean
)
