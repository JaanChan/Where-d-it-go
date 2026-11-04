# Where'd It Go? — AI Personal Memory Assistant

Where'd It Go? is a modern, AI-powered external memory assistant for Android that lets users effortlessly record where they placed everyday belongings—via natural language text, live speech-to-text, or photos—and retrieve exact locations instantly using conversational AI search, while also tracking borrowed items and shared household spaces.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and technical decisions incorporate your confirmed preferences from our planning interview alongside platform best practices:

- **Confirmed Decision — Shared Memory Spaces Invitation**: Users can invite family members or roommates into Shared Spaces using **both a shareable 6-character invite code and email lookup**, with owner/member permission controls and attribution showing who recorded each memory.
- **Confirmed Decision — Voice Memory Capture**: Recording a voice memory uses **live speech-to-text with instant AI extraction**, automatically parsing the spoken phrase (e.g., *"I put my black headphones inside my blue school bag"*) into structured item name, location, category, and contextual notes.
- **Confirmed Decision — Multimodal Photo Memory**: When capturing or selecting a photo of a drawer, desk, bag, or shelf, the app **automatically analyzes the image with Gemini multimodal AI** to detect visible items, storage containers, and descriptive visual context, attaching a compressed thumbnail to the memory record.
- **Authentication & Cloud Storage Strategy**: Authentication is powered by **Google Sign-In** (via Android Credential Manager) and backed by **Firebase Firestore** for real-time cross-device sync, private per-user isolation, and collaborative Shared Spaces.

---

## 1. Overview & Core Concept

- **What It Does**: Eliminates the everyday frustration of misplacing keys, school IDs, chargers, documents, earphones, and household items. Users record where something is once ("Save where something is once. Find it whenever you need it.") and ask natural questions later (*"Where did I put my school ID?"* or *"Who borrowed my calculator?"*).
- **Target Audience / Persona**: Students, busy professionals, roommates, and families who want a fast, conversational personal memory assistant rather than a rigid spreadsheet-style inventory system.
- **Key Value**: Grounded AI memory extraction and retrieval that **never fabricates locations**—every answer cites the user's actual saved memories, complete with timestamps, photo thumbnails, and previous location history.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Onboarding**:
   - Welcomes the user with the tagline *"Your things. Your memory. Never lose track again."* and a one-tap **Sign in with Google** flow. Automatically syncs the user's profile and notification settings.
2. **Main Dashboard (Home)**:
   - Displays the personalized greeting *"Hey! What are we looking for?"* with a prominent **AI Search Bar** (supporting follow-up conversational context and quick query chips).
   - Features a glowing **Remember Something** hero action button for immediate capture.
   - Surfaces **Recently Remembered Items**, **Frequently Searched Items**, **Borrowed Items Awaiting Return** (with overdue badges), and a preview of the **Recent Memory Timeline**.
3. **Quick Memory Capture ("Remember Something")**:
   - Opens a streamlined capture sheet where users can type a natural sentence, tap the microphone for **Live Speech-to-Text**, or snap/pick a **Photo** of a drawer, shelf, or bag.
   - **AI Extraction Engine** immediately extracts the **Item Name**, **Current Location**, **Category**, and **Additional Details/Notes**, allowing the user to save to their Private Vault or a selected Shared Space in one tap.
   - If an item with the same normalized name already exists, its previous location is automatically archived into its **Location History** so users can see both where it is now and where it used to be.
4. **Grounded AI-Powered Search**:
   - Users ask natural questions (*"Where is my charger?"*, *"Did I leave my headphones in my school bag?"*, *"Where did I put the document I photographed yesterday?"*, *"Who borrowed my calculator?"*).
   - Maintains multi-turn conversation context for follow-up questions while strictly grounding responses in the user's private memories, shared space memories, and borrow records. Displays matched memory cards directly beneath the AI answer.
5. **Chronological Memory Timeline (Memories)**:
   - Displays a filterable timeline of all recorded items showing item name, current location badge, photo thumbnail, relative/exact timestamp, notes, and expandable **Previous Recorded Locations** chips. Supports inline editing, moving an item to a new location, and deletion.
6. **Borrow Tracker (Borrowed)**:
   - Tracks lent items with item name, borrower name/contact, date borrowed, expected return date, notes, and return status. Highlights overdue items with alert banners and allows one-tap **Mark Returned** or reminder scheduling.
7. **Shared Spaces & Profile**:
   - Allows creating household/roommate spaces, inviting members via **email lookup** or **6-character invite code**, viewing who recorded each shared item, and customizing notification preferences (overdue borrow reminders, daily memory summary, and shared space updates).

### Visual Identity & Theme

- **Aesthetic Direction**: Premium Obsidian & Electric Violet Memory Vault—sleek, minimal, high-contrast dark mode interface that feels like an intelligent personal assistant.
- **Color Palette & Mood**:
  - `--background`: Deep Charcoal (`#0B0D13`)
  - `--surface`: Elevated Slate (`#141824`) and Card Surface (`#1C2232`)
  - `--primary`: Electric Violet (`#8B5CF6`) with soft glow accents
  - `--secondary` / `--accent`: Electric Cyan-Blue (`#38BDF8`) for AI indicators and search highlights
  - `--warning` / `--error`: Amber (`#F59E0B`) for due-soon items and Coral Red (`#EF4444`) for overdue alerts
- **Typography & Hierarchy**:
  - **Display & Headlines**: *Space Grotesk* (bold, geometric, modern tech-editorial feel for greetings, headers, and item titles).
  - **Body & Conversational UI**: *Plus Jakarta Sans* (clean, highly legible for natural language transcripts, notes, and timestamps).
- **Component Styling & Adaptive Layout**:
  - Rounded 20dp surface cards with subtle 1dp borders, pill badges for locations, and smooth spring animations.
  - **Adaptive Navigation**: Bottom Navigation Bar on compact mobile screens (**Home**, **Memories**, **Add**, **Borrowed**, **Profile & Spaces**) and a persistent **Navigation Rail** with multi-pane layout on wide/tablet screens.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Strict AI Grounding with Structured JSON Output**
  - *Chosen Approach*: Use **Firebase AI Logic (`gemini-3.8-flash`)** configured with `Schema.obj` structured JSON schemas (`responseMimeType = "application/json"`). When answering search queries, pass the user's real Firestore memories and borrow records as grounded context, instructing the model to return the exact conversational answer, matched memory IDs, and confidence level (`CONFIRMED`, `HISTORICAL`, or `NOT_FOUND`).
  - *Why*: Guarantees zero hallucinated locations and enables the UI to render clickable cards for the exact matched item and its historical locations.
  - *Alternatives Considered*: Free-form ungrounded LLM chat was rejected because it risks inventing locations or failing to link back to the user's actual saved records.

- **Decision 2: Cloud Persistence & Photo Thumbnail Architecture**
  - *Chosen Approach*: Store structured memories, location history arrays, borrow records, and shared spaces in **Firebase Firestore** with zero-trust security rules. Compress and resize captured photos on-device into optimized WebP/JPEG data thumbnails stored seamlessly alongside memory documents so photos load instantaneously across devices and inside Shared Spaces without requiring external storage bucket configuration.
  - *Why*: Ensures instant cross-device synchronization, reliable offline caching, and zero-permission media selection via the Android Photo Picker and Camera capture.

- **Decision 3: Local Reminder & Notification Engine**
  - *Chosen Approach*: Implement a notification and reminder coordinator using Android Notification Channels and scheduled checks for overdue borrowed items, daily memory summaries, and shared space updates, controlled by user preferences in the Profile tab.
  - *Why*: Gives users actionable, customizable alerts when borrowed items pass their expected return date without spamming them.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        JETPACK COMPOSE ADAPTIVE UI                          │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐  │
│  │ Home & AI    │ │ Memory       │ │ Borrow       │ │ Shared Spaces &    │  │
│  │ Search View  │ │ Timeline     │ │ Tracker View │ │ Profile / Settings │  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └─────────┬──────────┘  │
│         └────────────────┼────────────────┼───────────────────┘             │
│                          ▼                ▼                                 │
│              ┌────────────────────────────────────────┐                     │
│              │   Quick Capture Sheet ("Remember")     │                     │
│              │  Text │ Live Speech │ Camera & Picker  │                     │
│              └───────────────────┬────────────────────┘                     │
└──────────────────────────────────┼──────────────────────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    VIEWMODEL & DOMAIN STATE LAYER                           │
│  ┌─────────────────────────┐  ┌────────────────────┐  ┌──────────────────┐  │
│  │ Memory & AI ViewModel   │  │ Borrow ViewModel   │  │ Spaces & Profile │  │
│  │ (Search, Capture, Sync) │  │ (Lending, Alerts)  │  │ ViewModel        │  │
│  └────────────┬────────────┘  └─────────┬──────────┘  └────────┬─────────┘  │
└───────────────┼─────────────────────────┼──────────────────────┼────────────┘
                ▼                         ▼                      ▼
┌──────────────────────────────────────┐ ┌────────────────────────────────────┐
│     FIREBASE AI LOGIC (GEMINI)       │ │   FIRESTORE & AUTH REPOSITORIES    │
│  • Natural Language Extraction       │ │  • Google Sign-In (CredentialMgr)  │
│  • Multimodal Drawer/Shelf Vision    │ │  • Users & Private Memories        │
│  • Grounded Conversational Search    │ │  • Borrowed Items & Shared Spaces  │
└──────────────────────────────────────┘ └────────────────────────────────────┘
```

### Data Model & State

1. **User Profile (`/users/{userId}`)**:
   - Stores `uid`, `displayName`, `email`, `photoUrl`, `frequentSearches`, notification toggles (`borrowRemindersEnabled`, `dailySummaryEnabled`, `sharedUpdatesEnabled`), and `createdAt` / `updatedAt`.
2. **Memory Record (`/users/{userId}/memories/{memoryId}` & `/shared_spaces/{spaceId}/memories/{memoryId}`)**:
   - Stores `userId`, `addedByName`, `itemName`, `location`, `category`, `description`, `imageBase64` (compressed visual thumbnail), `photoAiDescription` (extracted visual contents of the drawer/bag/shelf), `previousLocations` (chronological list of prior locations with timestamps), `searchCount`, `spaceId`, `createdAt`, and `updatedAt`.
3. **Borrowed Item (`/users/{userId}/borrowed_items/{borrowId}`)**:
   - Stores `userId`, `itemName`, `borrowerName`, `borrowerContact`, `notes`, `borrowDateMillis`, `expectedReturnMillis`, `isReturned`, `returnedAtMillis`, `createdAt`, and `updatedAt`.
4. **Shared Space (`/shared_spaces/{spaceId}`)**:
   - Stores `name`, `ownerId`, `ownerName`, `inviteCode` (unique 6-character code), `memberUids` (array of authorized UIDs for security rule enforcement), `memberEmails` (array of invited/joined emails), `memberNames`, `createdAt`, and `updatedAt`.

### Interactive Component & State Mapping

- **Quick Memory Capture Flow**:
  - *Voice Input*: Launches Android `SpeechRecognizer` with live partial transcript feedback; upon completion, passes transcript to Gemini extraction (`extractMemoryFromInput`) to auto-populate item name, location, category, and notes.
  - *Photo Input*: Launches zero-permission `PickVisualMedia` or `TakePicturePreview`; compresses the bitmap and runs Gemini multimodal analysis (`analyzePhotoForMemory`) to auto-detect items and storage context.
  - *Deduplication & Location History*: When saving a memory, if an existing record in the target space matches the item name, the app offers to update the existing item and automatically appends the old location + timestamp to `previousLocations`.
- **Grounded AI Search & Follow-Up Flow**:
  - Maintains a `chatHistory` list of user questions and grounded answers so follow-up questions (*"What else is in that bag?"* or *"When did I put it there?"*) resolve naturally.
  - Increments `searchCount` on matched memories so the Dashboard accurately populates **Frequently Searched Items**.
- **Shared Space Collaboration Flow**:
  - Users can create a space (generating a 6-character invite code), invite a member by email, or join an existing space via invite code. Firestore security rules validate `request.auth.uid in resource.data.memberUids` (or matching email on join) to enforce strict access control.
