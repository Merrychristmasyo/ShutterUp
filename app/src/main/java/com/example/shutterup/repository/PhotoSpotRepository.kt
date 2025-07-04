package com.example.shutterup.repository
import com.example.shutterup.model.PhotoSpot
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoSpotRepository @Inject constructor() {
    private val mockPhotoSpots = listOf(
        PhotoSpot("spot_001", "남산 타워", 37.55123, 126.98800, 10),
        PhotoSpot("spot_002", "경복궁", 37.5796, 126.9770, 25),
        PhotoSpot("spot_003", "잠실 롯데월드타워", 37.5126, 127.1026, 50),
        PhotoSpot("spot_004", "서울숲", 37.5447, 127.0366, 42),
        PhotoSpot("spot_005", "북촌 한옥마을", 37.5826, 126.9830, 18),
        PhotoSpot("spot_006", "한강공원 여의도지구", 37.5284, 126.9332, 30),
        PhotoSpot("spot_007", "덕수궁", 37.5658, 126.9751, 15),
        PhotoSpot("spot_008", "명동", 37.5610, 126.9860, 22),
        PhotoSpot("spot_009", "광화문 광장", 37.5759, 126.9768, 8),
        PhotoSpot("spot_010", "이태원", 37.5342, 126.9930, 35)
    )

    suspend fun getPhotoSpotList(): List<PhotoSpot> {
        delay(1000)
        return mockPhotoSpots
    }

    suspend fun getPhotoSpotById(id: String): PhotoSpot? {
        delay(1000)
        return mockPhotoSpots.find { it.id == id }
    }
}