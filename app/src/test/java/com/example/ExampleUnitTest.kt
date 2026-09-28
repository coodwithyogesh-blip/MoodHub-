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
    fun taskEntity_multiTaskTypes_isCorrect() {
        val appTask = TaskEntity(
            id = "task_app_1",
            projectId = "proj_app_1",
            title = "Build App",
            taskType = "APP_BUILD",
            status = "BUILDING",
            currentStep = "Compiling APK"
        )
        val thumbTask = TaskEntity(
            id = "task_thumb_2",
            projectId = "proj_thumb_2",
            title = "Thumbnail Gen",
            taskType = "THUMBNAIL_GEN",
            status = "GENERATING_MEDIA",
            currentStep = "Rendering 16:9 graphic"
        )
        val docTask = TaskEntity(
            id = "task_doc_3",
            projectId = "proj_doc_3",
            title = "Document Summary",
            taskType = "DOC_SUMMARIZE",
            status = "COMPLETED",
            currentStep = "Summary ready"
        )

        assertEquals("APP_BUILD", appTask.taskType)
        assertEquals("THUMBNAIL_GEN", thumbTask.taskType)
        assertEquals("DOC_SUMMARIZE", docTask.taskType)
        assertEquals("BUILDING", appTask.status)
        assertEquals("COMPLETED", docTask.status)
    }
}
