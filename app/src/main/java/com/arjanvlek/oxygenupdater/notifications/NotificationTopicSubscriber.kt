package com.arjanvlek.oxygenupdater.notifications

import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.DEVICE_TOPIC_PREFIX
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.UPDATE_METHOD_TOPIC_PREFIX
import com.arjanvlek.oxygenupdater.BuildConfig.NOTIFICATIONS_PREFIX
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_NOTIFICATION_TOPIC
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD_ID
import com.google.firebase.messaging.FirebaseMessaging

object NotificationTopicSubscriber {

    private val TAG = "NotificationTopicSubscriber"

    fun subscribe(data: ApplicationData) {
        val settingsManager = SettingsManager(data.applicationContext)
        val serverConnector = data.getServerConnector()

        val oldTopic = settingsManager.getPreference<String>(PROPERTY_NOTIFICATION_TOPIC, null)

        if (oldTopic == null) {
            serverConnector.getDevices { devices ->
                serverConnector.getAllUpdateMethods { updateMethods ->
                    // If the topic is not saved (App Version 1.0.0 did not do this), unsubscribe from all possible topics first to prevent duplicate / wrong notifications.
                    for (method in updateMethods) {
                        for (device in devices) {
                            FirebaseMessaging.getInstance()
                                    .unsubscribeFromTopic(NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX + device.id + UPDATE_METHOD_TOPIC_PREFIX + method.id)
                        }
                    }
                }
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic)
        }

        val newTopic = (NOTIFICATIONS_PREFIX + DEVICE_TOPIC_PREFIX
                + settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L)
                + UPDATE_METHOD_TOPIC_PREFIX
                + settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L))

        // Subscribe to the new topic to start receiving notifications.
        FirebaseMessaging.getInstance().subscribeToTopic(newTopic)
        logVerbose(TAG, "Subscribed to notifications on topic $newTopic ...")
        settingsManager.savePreference(PROPERTY_NOTIFICATION_TOPIC, newTopic)

    }
}
