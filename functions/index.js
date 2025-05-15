const functions = require("firebase-functions");
const admin = require("firebase-admin");
const fetch = require("node-fetch");
admin.initializeApp();

exports.getNutrients = functions.https.onCall(async (data, context) => {
  const items = data.items;   // [{foodCode, grams}]
  const db = admin.firestore();
  const sum = { kcal: 0, carbs: 0, protein: 0, fat: 0 };

  for (const it of items) {
    const ref = db.doc(`foodNutrients/${it.foodCode}`);
    let snap = await ref.get();

    if (!snap.exists) {
      const url = `https://openapi.foodsafetykorea.go.kr/api/${process.env.MFDS_API_KEY}/I2790/json/1/1/FOOD_CD=${it.foodCode}`;
      const res = await fetch(url).then(r => r.json());
      const row = res.I2790?.row?.[0];
      if (!row) throw new functions.https.HttpsError("not-found", "Invalid FOOD_CD");

      await ref.set({
        kcal:   parseFloat(row.NUTR_CONT1),
        carbs:  parseFloat(row.NUTR_CONT2),
        protein:parseFloat(row.NUTR_CONT3),
        fat:    parseFloat(row.NUTR_CONT4)
      });
      snap = await ref.get();
    }

    const b = snap.data();
    const r = it.grams / 100;
    sum.kcal    += b.kcal   * r;
    sum.carbs   += b.carbs  * r;
    sum.protein += b.protein* r;
    sum.fat     += b.fat    * r;
  }
  return sum;   // {kcal, carbs, protein, fat}
});
