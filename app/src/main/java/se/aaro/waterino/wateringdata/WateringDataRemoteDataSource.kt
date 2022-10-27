package se.aaro.waterino.wateringdata

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import se.aaro.waterino.data.dto.SettingsDto
import se.aaro.waterino.data.dto.WateringDataDto

class WateringDataRemoteDataSource(firebaseDatabase: FirebaseDatabase) {

    private val settingsRef = firebaseDatabase.getReference("settings")
    private val wateringDataRef = firebaseDatabase.getReference("wateringdata")

    private val _wateringSettings = MutableStateFlow(Result.success(SettingsDto()))
    val wateringSettings: StateFlow<Result<SettingsDto>> = _wateringSettings

    private val _wateringData = MutableStateFlow(Result.success(emptyList<WateringDataDto>()))
    val wateringData: StateFlow<Result<List<WateringDataDto>>> = _wateringData

    init {
        settingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _wateringSettings.value =
                    Result.success(snapshot.getValue<SettingsDto>() as SettingsDto)
                dispatchFilteredWateringData()
            }

            override fun onCancelled(error: DatabaseError) {
                _wateringSettings.tryEmit(Result.failure(error.toException()))
            }
        })

        wateringDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                _wateringData.tryEmit(
                    Result.success(
                        dataSnapshot
                            .children
                            .mapNotNull { it.getValue<WateringDataDto>() as WateringDataDto }
                            .filter { it.time > wateringSettings.value.getOrThrow().lastReset }
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                _wateringData.tryEmit(Result.failure(error.toException()))
            }
        })
    }

    private fun dispatchFilteredWateringData() {
        wateringDataRef.get().addOnCompleteListener { task ->
            _wateringData.tryEmit(
                Result.success(
                    task.result.children
                        .mapNotNull { it.getValue<WateringDataDto>() as WateringDataDto }
                        .filter { it.time > wateringSettings.value.getOrThrow().lastReset }
                )
            )
        }
    }


    suspend fun setWateringSettings(settingsDto: SettingsDto): Result<Unit> {
        val result = CompletableDeferred<Result<Unit>>()
        settingsRef.setValue(settingsDto)
            .addOnSuccessListener {
                result.complete(Result.success(Unit))
            }
            .addOnCanceledListener {
                result.complete(Result.failure(RuntimeException("Operation was cancelled")))
            }
            .addOnFailureListener {
                result.complete(Result.failure(it))
            }
        return result.await()
    }

    suspend fun resetData(): Result<Unit> {
        val result = CompletableDeferred<Result<Unit>>()
        val resetTime = System.currentTimeMillis()
        settingsRef.child("lastReset").setValue(resetTime)
            .addOnSuccessListener {
                result.complete(Result.success(Unit))
            }
            .addOnCanceledListener {
                result.complete(Result.failure(RuntimeException("Operation was cancelled")))
            }
            .addOnFailureListener {
                result.complete(Result.failure(it))
            }
        return result.await()
    }
}
