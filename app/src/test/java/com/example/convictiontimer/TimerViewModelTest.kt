
package com.example.convicttimer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TimerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var viewModel: TimerViewModel
    private lateinit var repository: TimerRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        viewModel = TimerViewModel(mock(), repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `loadInitialData should load exercises and update state`() = testDispatcher.runBlockingTest {
        // Given
        val categories = listOf("Pushups", "Squats")
        val steps = listOf("Step 1", "Step 2")
        val exercises = listOf(Exercise("Pushups", 1, "Wall Pushups", "Beginner", 10, 3))
        `when`(repository.getCategories()).thenReturn(categories)
        `when`(repository.getStepsForCategory("Pushups")).thenReturn(steps)
        `when`(repository.getExercisesForStep("Pushups", "Step 1")).thenReturn(exercises)

        // When
        viewModel.loadInitialData()

        // Then
        assertEquals(categories, viewModel.categories.value)
        assertEquals("Pushups", viewModel.selectedCategory.value)
        assertEquals(steps, viewModel.steps.value)
    }
}
