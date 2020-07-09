const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

exports.sendNewDataNotification = functions.database
  .ref("/wateringdata/{wateringDataId}")
  .onCreate((snapshot, context) => {
    const payload = {
      notification: {
        title: `New data from Waterino available!`,
        body: `Moisture: ${
          snapshot.val().moisture
        }%\nTemperature: ${snapshot
          .val()
          .temperature.toFixed(2)}°C \nHumididty: ${
          snapshot.val().humidity
        }%\nWatered: ${snapshot.val().wateredPlant ? "Yes" : "No"}`,
      },
    };
    console.log(payload);
    return admin
      .messaging()
      .sendToTopic("plantData", payload)
      .then((response) => {
        // Response is a message ID string.
        console.log("Successfully sent message:", response);
        return;
      })
      .catch((error) => {
        console.log("Error sending message:", error);
      });
  });
