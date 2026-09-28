package com.example.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log

sealed class ContactResult {
    data class Found(val name: String, val phoneNumber: String) : ContactResult()
    data class MultipleMatches(val candidates: List<Pair<String, String>>) : ContactResult()
    object NotFound : ContactResult()
    object PermissionRequired : ContactResult()
}

data class ActionResult(
    val success: Boolean,
    val actionName: String,
    val details: String
)

class DeviceActionBridge(private val context: Context) {

    private val tag = "DeviceActionBridge"

    /**
     * Opens WhatsApp application or deep link
     */
    fun openWhatsApp(phoneNumber: String? = null, message: String? = null): ActionResult {
        return try {
            val pm = context.packageManager
            val intent = if (!phoneNumber.isNullOrBlank()) {
                val cleanNum = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNum&text=${Uri.encode(message ?: "")}")
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                pm.getLaunchIntentForPackage("com.whatsapp")?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            if (intent != null) {
                context.startActivity(intent)
                ActionResult(true, "openWhatsApp", "WhatsApp opened successfully")
            } else {
                // Try fallback web link
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionResult(true, "openWhatsApp", "WhatsApp Web opened in browser")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to open WhatsApp: ${e.message}")
            ActionResult(false, "openWhatsApp", "Could not open WhatsApp: ${e.message}")
        }
    }

    /**
     * Opens allowed applications
     */
    fun openApp(appName: String): ActionResult {
        val target = appName.trim().lowercase()
        val pm = context.packageManager

        val pkg = when {
            target.contains("whatsapp") -> "com.whatsapp"
            target.contains("youtube") -> "com.google.android.youtube"
            target.contains("instagram") || target.contains("insta") -> "com.instagram.android"
            target.contains("chrome") || target.contains("browser") -> "com.android.chrome"
            else -> null
        }

        if (pkg != null) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    return ActionResult(true, "openApp", "Opened $appName successfully")
                }
            } catch (e: Exception) {
                Log.w(tag, "Package launch error for $pkg: ${e.message}")
            }
        }

        // Special system intents
        try {
            when {
                target.contains("camera") -> {
                    val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(cameraIntent)
                    return ActionResult(true, "openApp", "Camera opened")
                }
                target.contains("dialer") || target.contains("phone") -> {
                    val dialerIntent = Intent(Intent.ACTION_DIAL).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialerIntent)
                    return ActionResult(true, "openApp", "Dialer opened")
                }
                target.contains("setting") -> {
                    val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return ActionResult(true, "openApp", "Settings opened")
                }
                target.contains("map") -> {
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(mapIntent)
                    return ActionResult(true, "openApp", "Maps opened")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Intent launch error: ${e.message}")
        }

        return ActionResult(false, "openApp", "App '$appName' is not installed or not in allowed list")
    }

    /**
     * Safely opens URL in browser
     */
    fun openUrl(url: String): ActionResult {
        return try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "openUrl", "Opened $formatted")
        } catch (e: Exception) {
            ActionResult(false, "openUrl", "Failed to open URL: ${e.message}")
        }
    }

    /**
     * Launches dialer with phone number
     */
    fun makeCall(phoneNumber: String): ActionResult {
        return try {
            val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "makeCall", "Calling $cleanNumber")
        } catch (e: Exception) {
            ActionResult(false, "makeCall", "Could not initiate call: ${e.message}")
        }
    }

    /**
     * Queries device contacts by contact name
     */
    fun searchAndCallContact(contactName: String): ContactResult {
        val hasContactPermission = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasContactPermission) {
            return ContactResult.PermissionRequired
        }

        val matches = mutableListOf<Pair<String, String>>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: ""
                    val number = cursor.getString(numberIndex) ?: ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        matches.add(name to number)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error reading contacts: ${e.message}")
        }

        return when {
            matches.isEmpty() -> ContactResult.NotFound
            matches.size == 1 -> {
                val (name, number) = matches.first()
                makeCall(number)
                ContactResult.Found(name, number)
            }
            else -> {
                // Return top distinct candidates
                val distinctCandidates = matches.distinctBy { it.second }.take(4)
                ContactResult.MultipleMatches(distinctCandidates)
            }
        }
    }
}
