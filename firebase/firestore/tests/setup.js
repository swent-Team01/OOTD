import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export async function setup() {
  const projectId = process.env.FIREBASE_PROJECT_ID || 'ootd-rules-test';
  // Resolve the firestore.rules at the repo root from firestore/rules-tests
  const rulesPath = path.resolve(__dirname, '../../firestore.rules');
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
  // Seed baseline users and posts with rules disabled
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();

    // Users: derive friendUids from friendList so rules can perform membership checks
    const users = seed.users ?? defaultUsers();
    for (const u of users) {
      const friendUids = (u.friendList ?? [])
        .filter(f => f && typeof f.uid === 'string' && f.uid.length > 0)
        .map(f => f.uid);
      await db.collection('users').doc(u.uid).set({ ...u, friendUids });
    }

    // Posts
    const posts = seed.posts ?? defaultPosts();
    for (const p of posts) {
      await db.collection('posts').doc(p.postUID).set(p);
    }
  });
}

export function defaultUsers() {
  return [
    {
      uid: 'me',
      name: 'Me',
      friendList: [ { uid: 'u1', name: 'Alice' }, { uid: 'u2', name: 'Bob' } ]
    },
    {
      uid: 'u1',
      name: 'Alice',
      friendList: [ { uid: 'me', name: 'Me' } ] // mutual with me
    },
    {
      uid: 'u2',
      name: 'Bob',
      friendList: [ { uid: 'me', name: 'Me' } ] // mutual with me
    },
    {
      uid: 'u3',
      name: 'Carol',
      friendList: [ { uid: 'me', name: 'Me' } ] // one-way: me does NOT list u3
    },
    {
      uid: 'u9',
      name: 'Mallory',
      friendList: []
    }
  ];
}

export function defaultPosts() {
  return [
    { postUID: 'p1', uid: 'u1', name: 'Alice', outfitURL: 'url_p1', timestamp: 1 },
    { postUID: 'p2', uid: 'u2', name: 'Bob',   outfitURL: 'url_p2', timestamp: 2 },
    { postUID: 'p3', uid: 'u3', name: 'Carol', outfitURL: 'url_p3', timestamp: 3 },
    { postUID: 'p9', uid: 'u9', name: 'Mallory', outfitURL: 'url_p9', timestamp: 9 }
  ];
}
