const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const cloudinary = require("cloudinary").v2;
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();
const CLOUDINARY_CLOUD_NAME = "x0bgvsgy"; // Existing cloud name found in Android source.
const CLOUDINARY_API_KEY = defineSecret("CLOUDINARY_API_KEY");
const CLOUDINARY_API_SECRET = defineSecret("CLOUDINARY_API_SECRET");

function requireOwner(request) {
  if (!request.auth || !(request.auth.token.owner === true || request.auth.token.admin === true)) {
    throw new HttpsError("permission-denied", "Owner permission required.");
  }
  return request.auth.uid;
}
function validString(v, max = 200) {
  return typeof v === "string" && v.trim().length > 0 && v.length <= max;
}

exports.adjustCoins = onCall(async (request) => {
  const ownerUid = requireOwner(request);
  const { uid, amount, direction, reason } = request.data || {};
  if (!validString(uid, 128) || !Number.isSafeInteger(amount) || amount <= 0 ||
      !["add", "remove"].includes(direction) || !validString(reason, 160)) {
    throw new HttpsError("invalid-argument", "Valid user, positive amount, action and reason are required.");
  }
  const userRef = db.collection("users").doc(uid);
  const txRef = db.collection("coinTransactions").doc();
  let newBalance = 0;
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(userRef);
    if (!snap.exists) throw new HttpsError("not-found", "User not found.");
    const oldBalance = Number(snap.get("coins") || 0);
    const delta = direction === "add" ? amount : -amount;
    newBalance = oldBalance + delta;
    if (!Number.isSafeInteger(newBalance) || newBalance < 0) {
      throw new HttpsError("failed-precondition", "Insufficient coin balance.");
    }
    tx.set(userRef, { coins: newBalance }, { merge: true });
    tx.set(txRef, {
      targetUid: uid, ownerUid, direction, amount, oldBalance, newBalance,
      reason: reason.trim(), createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
  return { transactionId: txRef.id, newBalance };
});

exports.createFrameUploadSignature = onCall(
  { secrets: [CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET] },
  async (request) => {
    requireOwner(request);
    const { fileName, resourceType } = request.data || {};
    if (!validString(fileName, 180) || !["image", "video"].includes(resourceType)) {
      throw new HttpsError("invalid-argument", "Invalid file details.");
    }
    const apiKey = CLOUDINARY_API_KEY.value();
    const apiSecret = CLOUDINARY_API_SECRET.value();
    const timestamp = Math.floor(Date.now() / 1000);
    const folder = "disco-voice-chat/frames";
    const publicId = "frame_" + crypto.randomUUID();
    const params = { folder, public_id: publicId, timestamp };
    const signature = cloudinary.utils.api_sign_request(params, apiSecret);
    return {
      cloudName: CLOUDINARY_CLOUD_NAME, apiKey, timestamp, folder, publicId,
      signature, resourceType
    };
  }
);

exports.saveFrame = onCall(async (request) => {
  const ownerUid = requireOwner(request);
  const d = request.data || {};
  if (!validString(d.name, 60) || !validString(d.assetUrl, 1200) ||
      !validString(d.publicId, 200) || !["image", "video"].includes(d.assetType) ||
      !Number.isSafeInteger(d.priceCoins) || d.priceCoins < 0 ||
      !validString(d.thumbnailUrl, 1200)) {
    throw new HttpsError("invalid-argument", "Invalid frame metadata.");
  }
  if (!d.assetUrl.startsWith("https://res.cloudinary.com/" + CLOUDINARY_CLOUD_NAME + "/")) {
    throw new HttpsError("invalid-argument", "Asset must be hosted in the configured Cloudinary account.");
  }
  const ref = db.collection("storeItems").doc();
  await ref.set({
    name: d.name.trim(), description: typeof d.description === "string" ? d.description.slice(0, 240) : "",
    category: "Frames", assetUrl: d.assetUrl, publicId: d.publicId,
    assetType: d.assetType, thumbnailUrl: d.thumbnailUrl,
    priceCoins: d.priceCoins, isPublished: false, createdBy: ownerUid,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { frameId: ref.id, isPublished: false };
});

exports.setFramePublished = onCall(async (request) => {
  requireOwner(request);
  const { frameId, isPublished } = request.data || {};
  if (!validString(frameId, 128) || typeof isPublished !== "boolean") {
    throw new HttpsError("invalid-argument", "Invalid frame status.");
  }
  await db.collection("storeItems").doc(frameId).update({
    isPublished, updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { frameId, isPublished };
});

exports.deleteFrame = onCall(
  { secrets: [CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET] },
  async (request) => {
    requireOwner(request);
    const { frameId } = request.data || {};
    if (!validString(frameId, 128)) throw new HttpsError("invalid-argument", "Frame ID required.");
    const ref = db.collection("storeItems").doc(frameId);
    const snap = await ref.get();
    if (!snap.exists) throw new HttpsError("not-found", "Frame not found.");
    const d = snap.data();
    cloudinary.config({
      cloud_name: CLOUDINARY_CLOUD_NAME,
      api_key: CLOUDINARY_API_KEY.value(),
      api_secret: CLOUDINARY_API_SECRET.value(),
      secure: true
    });
    const resourceType = d.assetType === "video" ? "video" : "image";
    await cloudinary.uploader.destroy(d.publicId, { resource_type: resourceType });
    await ref.delete();
    return { deleted: true };
  }
);

// Future Android integration: call this from the app instead of changing coins/owned locally.
exports.purchaseStoreItem = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Sign in required.");
  const uid = request.auth.uid;
  const { itemId } = request.data || {};
  if (!validString(itemId, 128)) throw new HttpsError("invalid-argument", "Item ID required.");
  const userRef = db.collection("users").doc(uid);
  const itemRef = db.collection("storeItems").doc(itemId);
  const txRef = db.collection("coinTransactions").doc();
  let balance = 0;
  await db.runTransaction(async (tx) => {
    const [userSnap, itemSnap] = await Promise.all([tx.get(userRef), tx.get(itemRef)]);
    if (!userSnap.exists || !itemSnap.exists || itemSnap.get("isPublished") !== true) {
      throw new HttpsError("not-found", "User or published store item not found.");
    }
    const user = userSnap.data(), item = itemSnap.data();
    const owned = Array.isArray(user.owned) ? user.owned : [];
    if (owned.includes(itemId)) throw new HttpsError("already-exists", "Item already owned.");
    const price = Number(item.priceCoins || 0);
    const oldBalance = Number(user.coins || 0);
    balance = oldBalance - price;
    if (!Number.isSafeInteger(price) || price < 0 || balance < 0) {
      throw new HttpsError("failed-precondition", "Not enough coins.");
    }
    tx.set(userRef, { coins: balance, owned: admin.firestore.FieldValue.arrayUnion(itemId) }, { merge: true });
    tx.set(txRef, {
      targetUid: uid, ownerUid: "system", direction: "purchase", amount: price,
      oldBalance, newBalance: balance, itemId, reason: "Store purchase",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
  return { newBalance: balance, transactionId: txRef.id };
});
