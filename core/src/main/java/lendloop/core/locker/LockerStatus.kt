package lendloop.core.locker

import com.badlogic.gdx.graphics.Color

enum class LockerStatus(val color: Color) {
    AVAILABLE           (Color(0.2f, 0.9f, 0.3f, 1f)),   // green
    PARTIALLY_AVAILABLE (Color(1.0f, 0.7f, 0.2f, 1f)),   // orange
    FULL                (Color(0.9f, 0.2f, 0.2f, 1f)),   // red
    EMPTY               (Color(0.5f, 0.5f, 0.5f, 1f));   // grey
}
