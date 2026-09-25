const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

// Инициализируем приложение Firebase Admin
admin.initializeApp();

// Триггер на создание нового сообщения в коллекции "notes"
exports.sendChatNotification = onDocumentCreated("notes/{noteId}", async (event) => {
    // Получаем данные созданного документа
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

    // Имя топика, на который подписаны приложения в MainActivity.kt
    const topic = "chat_messages_topic";

    // Формируем payload-пакет уведомления
    const message = {
        topic: topic,
        data: {
            title: `Чат ПСГиИ: ${authorName}`,
            body: text,
            type: "chat"
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
