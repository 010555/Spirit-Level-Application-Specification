package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AlignmentState
import com.example.ui.LevelMode
import com.example.ui.LevelViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `test app name is Spirit Level`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Spirit Level", appName)
    }

    @Test
    fun `test level mode transitions and initial state`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LevelViewModel(application)

        // Initial default state should be Horizontal
        assertEquals(LevelMode.HORIZONTAL, viewModel.currentMode.value)

        // Select vertical level
        viewModel.selectMode(LevelMode.VERTICAL)
        assertEquals(LevelMode.VERTICAL, viewModel.currentMode.value)

        // Select angle meter
        viewModel.selectMode(LevelMode.ANGLE_METER)
        assertEquals(LevelMode.ANGLE_METER, viewModel.currentMode.value)
    }

    @Test
    fun `test alignment state calculation boundaries`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LevelViewModel(application)

        // Assert that default status is PERFECT when there are no sensor readings yet (since default is 0.0)
        assertEquals(AlignmentState.PERFECT, viewModel.alignmentState.value)

        // Assert setup configurations are loaded successfully
        assertNotNull(viewModel.appSettings.value)
        assertTrue(viewModel.appSettings.value.hapticEnabled)
        assertTrue(viewModel.appSettings.value.audioEnabled)
    }
}
