package com.example

import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun projectEntity_creation_isCorrect() {
        val project = ProjectEntity(
            id = "proj_test_123",
            name = "Carpenter Service",
            projectType = "mobile_app",
            description = "Test carpenter app",
            status = "CREATED",
            rootDir = "/tmp/workspace/proj_test_123"
        )
        assertEquals("Carpenter Service", project.name)
        assertEquals("mobile_app", project.projectType)
        assertNotNull(project.id)
    }

    @Test
    fun taskEntity_creation_isCorrect() {
        val task = TaskEntity(
            id = "task_test_123",
            projectId = "proj_test_123",
            title = "Build App",
            taskType = "APP_BUILD",
            status = "PLANNING",
            currentStep = "Planning architecture"
        )
        assertEquals("APP_BUILD", task.taskType)
        assertEquals("PLANNING", task.status)
        assertEquals(0f, task.progress)
    }
}
