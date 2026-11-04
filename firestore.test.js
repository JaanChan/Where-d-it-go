const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read or write private memories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("memories").get());
});

test("2. Authenticated user can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      photoUrl: "",
      frequentSearches: ["Where is my charger?"],
      borrowRemindersEnabled: true,
      dailySummaryEnabled: false,
      sharedUpdatesEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("3. Cross-user profile PII read is rejected", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      borrowRemindersEnabled: true,
      dailySummaryEnabled: false,
      sharedUpdatesEnabled: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("4. Authenticated user can create, query, and update their own memory", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const memRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1");

  await assertSucceeds(
    memRef.set({
      userId: ALICE_UID,
      addedByName: "Alice",
      itemName: "Black headphones",
      location: "Blue school bag",
      category: "Electronics",
      description: "Put inside front pocket",
      imageBase64: "",
      photoAiDescription: "",
      previousLocations: [],
      searchCount: 0,
      spaceId: "",
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    memRef.update({
      location: "Desk top drawer",
      previousLocations: ["Blue school bag (Yesterday)"],
      updatedAt: new Date(),
    })
  );
});

test("5. Cross-user private memory access and shadow field injection fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  // Shadow field injection must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_bad").set({
      userId: ALICE_UID,
      addedByName: "Alice",
      itemName: "Keys",
      location: "Kitchen counter",
      category: "Keys",
      description: "",
      searchCount: 0,
      spaceId: "",
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seed valid memory for Alice
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      addedByName: "Alice",
      itemName: "School ID",
      location: "Second drawer",
      category: "ID & Cards",
      description: "",
      searchCount: 0,
      spaceId: "",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").get());
});

test("6. Borrowed items CRUD and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const borrowRef = aliceDb.collection("users").doc(ALICE_UID).collection("borrowed_items").doc("borrow_1");

  await assertSucceeds(
    borrowRef.set({
      userId: ALICE_UID,
      itemName: "Scientific Calculator",
      borrowerName: "Sam",
      borrowerContact: "sam@school.edu",
      notes: "For calculus exam",
      borrowDateMillis: 1700000000000,
      expectedReturnMillis: 1700086400000,
      isReturned: false,
      returnedAtMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    borrowRef.update({
      isReturned: true,
      returnedAtMillis: 1700050000000,
      updatedAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("borrowed_items").doc("borrow_1").get());
});

test("7. Shared space creation, invite code lookup, joining, and shared memories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const spaceId = "space_home1";
  const inviteCode = "HOME99";

  // Atomic batch creating shared space + invite code lookup
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("shared_spaces").doc(spaceId), {
    name: "Apt 4B Roommates",
    ownerId: ALICE_UID,
    ownerName: "Alice",
    inviteCode: inviteCode,
    memberUids: [ALICE_UID],
    memberNames: ["Alice"],
    invitedEmails: ["bob@example.com"],
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("space_codes").doc(inviteCode), {
    inviteCode: inviteCode,
    spaceId: spaceId,
    spaceName: "Apt 4B Roommates",
    ownerId: ALICE_UID,
    ownerName: "Alice",
    createdAt: now,
  });
  await assertSucceeds(batch.commit());

  // Bob can look up the exact inviteCode via get, but cannot list all space_codes
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(bobDb.collection("space_codes").doc(inviteCode).get());
  await assertFails(bobDb.collection("space_codes").get());

  // Bob joins the shared space
  await assertSucceeds(
    bobDb.collection("shared_spaces").doc(spaceId).update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: ["Alice", "Bob"],
      updatedAt: new Date(),
    })
  );

  // Bob adds a shared memory
  await assertSucceeds(
    bobDb.collection("shared_spaces").doc(spaceId).collection("memories").doc("shared_mem_1").set({
      userId: BOB_UID,
      addedByName: "Bob",
      itemName: "Spare House Keys",
      location: "Ceramic bowl by the front door",
      category: "Keys",
      description: "Shared spare set",
      imageBase64: "",
      photoAiDescription: "",
      previousLocations: [],
      searchCount: 0,
      spaceId: spaceId,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Charlie (non-member) cannot read shared space or its memories
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("shared_spaces").doc(spaceId).get());
  await assertFails(
    charlieDb.collection("shared_spaces").doc(spaceId).collection("memories").doc("shared_mem_1").get()
  );
});
