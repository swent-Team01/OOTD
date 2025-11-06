import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export async function setup() {
  const projectId = process.env.FIREBASE_PROJECT_ID || 'ootd-rules-test';
  // Resolve the firestore.rules at the repo root from firestore/rules-tests
  const rulesPath = path.resolve(__dirname, '../firestore.rules');
  const rules = fs.readFileSync(rulesPath, 'utf8');

  // Read emulator host/port from env or default to localhost:8080
  const hostPort = (process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080').split(':');
  const host = hostPort[0];
  const port = Number(hostPort[1] || 8080);

  const env = await initializeTestEnvironment({
    projectId,
    firestore: { rules, host, port }
  });
  return env;
}

export async function setupFirestore(env, seed = {}) {
  // Seed baseline accounts, posts, and notifications with rules disabled
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();

    // Accounts: use the new structure with ownerId and friendUids
    const accounts = seed.accounts ?? defaultAccounts();
    for (const account of accounts) {
      await db.collection('accounts').doc(account.ownerId).set(account);
    // Users: derive friendUids from friendList so rules can perform membership checks
    const users = seed.users ?? defaultUsers();
    for (const u of users) {
      const friendUids = (u.friendList ?? [])
        .filter(f => f && typeof f.uid === 'string' && f.uid.length > 0)
        .map(f => f.uid);
      // Ensure ownerId exists so security rules that check ownerId behave correctly
      await db.collection('users').doc(u.uid).set({ ...u, friendUids, ownerId: u.uid });
    }

    // Posts
    const posts = seed.posts ?? defaultPosts();
    for (const post of posts) {
      await db.collection('posts').doc(post.postUID).set(post);
    }

    // Notifications (optional seed)
    const notifications = seed.notifications ?? [];
    for (const notif of notifications) {
      const notifId = notif.id || `${notif.senderId}_${notif.type}_${notif.receiverId}`;
      await db.collection('notifications').doc(notifId).set(notif);
    }

    // Items (optional seed)
    const items = seed.items ?? [];
    for (const item of items) {
      await db.collection('items').doc(item.itemId).set(item);
    }
  });
}

export function defaultAccounts() {
  return [
    {
      ownerId: 'me',
      name: 'Me',
      email: 'me@example.com',
      friendUids: ['u1', 'u2'] // mutual friends with u1 and u2
    },
    {
      ownerId: 'u1',
      name: 'Alice',
      email: 'alice@example.com',
      friendUids: ['me'] // mutual with me
    },
    {
      ownerId: 'u2',
      name: 'Bob',
      email: 'bob@example.com',
      friendUids: ['me'] // mutual with me
    },
    {
      ownerId: 'u3',
      name: 'Carol',
      email: 'carol@example.com',
      friendUids: []
    },
    {
      ownerId: 'u9',
      name: 'Mallory',
      email: 'mallory@example.com',
      friendUids: [] // no friends
    }
  ];
}

export function defaultPosts() {
  return [
    {
      postUID: 'p1',
      ownerId: 'u1',
      name: 'Alice',
      outfitURL: 'url_p1',
      timestamp: 1
    },
    {
      postUID: 'p2',
      ownerId: 'u2',
      name: 'Bob',
      outfitURL: 'url_p2',
      timestamp: 2
    },
    {
      postUID: 'p3',
      ownerId: 'u3',
      name: 'Carol',
      outfitURL: 'url_p3',
      timestamp: 3
    },
    {
      postUID: 'p9',
      ownerId: 'u9',
      name: 'Mallory',
      outfitURL: 'url_p9',
      timestamp: 9
    }
  ];
}

export function defaultNotifications() {
  return [
    {
      id: 'u1_follow_me',
      senderId: 'u1',
      receiverId: 'me',
      type: 'follow',
      timestamp: Date.now(),
      read: false
    }
  ];
}

export function defaultItems() {
  return [
    {
      itemId: 'item1',
      ownerId: 'me',
      name: 'Denim Jacket',
      imageURL: 'url_item1',
      category: 'outerwear'
    },
    {
      itemId: 'item2',
      ownerId: 'u1',
      name: 'White Sneakers',
      imageURL: 'url_item2',
      category: 'shoes'
    }
  ];
}