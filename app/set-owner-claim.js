// Run from a trusted computer after installing functions dependencies.
// Example: GOOGLE_APPLICATION_CREDENTIALS=/secure/path/service-account.json node scripts/set-owner-claim.js FIREBASE_AUTH_UID
// Never commit the service-account JSON file.
const admin = require("firebase-admin");
const uid = process.argv[2];
if (!uid) {
  console.error("Usage: node scripts/set-owner-claim.js FIREBASE_AUTH_UID");
  process.exit(1);
}
admin.initializeApp({ credential: admin.credential.applicationDefault() });
admin.auth().setCustomUserClaims(uid, { owner: true })
  .then(() => {
    console.log("Owner claim set for UID:", uid);
    process.exit(0);
  })
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
