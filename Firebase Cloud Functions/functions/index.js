const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

// Инициализируем приложение Firebase Admin
admin.initializeApp();

// 1. Триггер на создание нового сообщения в коллекции "notes"
exports.sendChatNotification = onDocumentCreated("notes/{noteId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
        console.log("Документ пуст, отмена.");
        return;
    }

    const messageData = snapshot.data();
    if (!messageData) {
        console.log("Данные сообщения отсутствуют.");
        return;
    }

    const text = messageData.text || "Новое сообщение";
    const authorName = messageData.authorName || "Сотрудник";

    // Топик мгновенных сообщений чата
    const topic = "chat_messages_topic";

    // Формируем payload-пакет уведомления для чата
    const message = {
        topic: topic,
        data: {
            title: `Чат ПСГиИ: ${authorName}`,
            body: text,
            type: "chat" // Вызовет открытие экрана чата в приложении
        }
    };

    try {
        console.log(`Отправка push-уведомления от ${authorName} в топик ${topic}...`);
        const response = await admin.messaging().send(message);
        console.log("Push-уведомление успешно отправлено:", response);
    } catch (error) {
        console.error("Ошибка при отправке push-уведомления:", error);
    }
});
// 2. HTTP-триггер для запуска OTA-рассылки обновлений
exports.sendOtaNotification = onRequest(async (req, res) => {
    // Получаем параметры из GET или POST запроса
    const versionName = req.query.version || req.body.version || "v0.0.5";
    const description = req.query.desc || req.body.desc || "Доступны новые исправления стабильности.";

    // Топик обновлений, на который подписано приложение в MainActivity.kt
    const topic = "app_updates";

    // Формируем payload-пакет для OTA-кампании
    const message = {
        topic: topic,
        data: {
            title: "Доступно обновление ПСГиИ!",
            body: `Выпущена версия ${versionName}. Нажмите для установки.`,
            type: "update" // В MyFirebaseMessagingService превратится в navigate_to = "update"
        }
    };

    try {
        console.log(`Запуск OTA рассылки для версии ${versionName} в топик ${topic}...`);
        const response = await admin.messaging().send(message);
        console.log("OTA Push-уведомление успешно отправлено:", response);

        // Возвращаем успешный статус в браузер
        res.status(200).send({
            success: true,
            message: `OTA рассылка успешно запущена для версии ${versionName}`,
            firebaseResponse: response
        });
    } catch (error) {
        console.error("Ошибка при отправке OTA push-уведомления:", error);
        res.status(500).send({
            success: false,
            error: error.message
        });
    }
});
