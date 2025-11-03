import { describe, test, beforeAll, afterAll, expect } from 'vitest';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  where,
} from 'firebase/firestore';
import { setup, setupFirestore } from './setup.js';

describe('Firestore rules - OOTD friend-only feed', () => {
  let env;
  let meCtx, me;
  let aliceCtx, alice;
  let charlieCtx, charlie;

  beforeAll(async () => {
    env = await setup();

    meCtx = env.authenticatedContext('me');
    me = meCtx.firestore();

    aliceCtx = env.authenticatedContext('u1');
    alice = aliceCtx.firestore();

    charlieCtx = env.authenticatedContext('u3');
    charlie = charlieCtx.firestore();

    await setupFirestore(env);
  });

  afterAll(async () => {
    await env.clearFirestore();
    await env.cleanup();
  });

  // Accounts
  test('Friends can read accounts; non-friends cannot', async () => {
    // me and u1 are friends (seeded)
    await assertSucceeds(getDoc(doc(me, 'accounts/u1')));

    // u3 is not friends with me
    await assertFails(getDoc(doc(me, 'accounts/u3')));
  });

  test('User can remove themselves from friendUids', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'accounts/me'), {
        ownerId: 'me',
        name: 'Me',
        friendUids: ['u1'],
      })
    );

    // u1 removes themselves from me's friendUids
    await assertSucceeds(
      setDoc(doc(alice, 'accounts/me'), {
        ownerId: 'me',
        name: 'Me',
        friendUids: [],
      })
    );
  });

  test('User can add themselves to friendUids ONLY with follow notification', async () => {
    // Create follow notification
    await assertSucceeds(
      setDoc(doc(alice, 'notifications/u1_follow_me'), {
        senderId: 'u1',
        receiverId: 'me',
        type: 'follow',
      })
    );

    await assertSucceeds(
      setDoc(doc(alice, 'accounts/u1'), {
        ownerId: 'u1',
        name: 'Alice',
        friendUids: [],
      })
    );

    // me can add themselves because notification exists
    await assertSucceeds(
      setDoc(doc(me, 'accounts/u1'), {
        ownerId: 'u1',
        name: 'Alice',
        friendUids: ['me'],
      })
    );

    // Without notification, should fail
    await assertSucceeds(
      setDoc(doc(charlie, 'accounts/u3'), {
        ownerId: 'u3',
        name: 'Charlie',
        friendUids: [],
      })
    );

    await assertFails(
      setDoc(doc(me, 'accounts/u3'), {
        ownerId: 'u3',
        name: 'Charlie',
        friendUids: ['me'],
      })
    );
  });

  // Notifications
  test('User can read/write their own notifications as sender or receiver', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'notifications/notif1'), {
        senderId: 'me',
        receiverId: 'u1',
        type: 'follow',
      })
    );

    await assertSucceeds(getDoc(doc(me, 'notifications/notif1')));
    await assertSucceeds(getDoc(doc(alice, 'notifications/notif1')));

    // u3 is neither sender nor receiver
    await assertFails(getDoc(doc(charlie, 'notifications/notif1')));
  });

  // Posts
  test('Author and friends can read posts; non-friends cannot', async () => {
    // Ensure accounts are set up properly
    await assertSucceeds(
      setDoc(doc(me, 'accounts/me'), {
        ownerId: 'me',
        name: 'Me',
        friendUids: ['u1', 'u2'],
      })
    );

    await assertSucceeds(
      setDoc(doc(alice, 'accounts/u1'), {
        ownerId: 'u1',
        name: 'Alice',
        friendUids: ['me'],
      })
    );

    await assertSucceeds(
      setDoc(doc(me, 'posts/my_post'), {
        postUID: 'my_post',
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: Date.now(),
      })
    );

    // Author can read
    await assertSucceeds(getDoc(doc(me, 'posts/my_post')));

    // Friend can read (u1 is friends with me)
    await assertSucceeds(getDoc(doc(alice, 'posts/my_post')));

    // Non-friend cannot read (u3 is not mutual friends)
    await assertFails(getDoc(doc(charlie, 'posts/my_post')));
  });

  test('Only author can create post with their ownerId', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'posts/valid_post'), {
        postUID: 'valid_post',
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: Date.now(),
      })
    );

    // Cannot create with different ownerId
    await assertFails(
      setDoc(doc(me, 'posts/invalid_post'), {
        postUID: 'invalid_post',
        ownerId: 'u1',
        name: 'Alice',
        outfitURL: 'url',
        timestamp: Date.now(),
      })
    );
  });

  test('Only author can update and delete posts', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'posts/update_post'), {
        postUID: 'update_post',
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'url1',
        timestamp: 1,
      })
    );

    // Author can update
    await assertSucceeds(
      setDoc(doc(me, 'posts/update_post'), {
        postUID: 'update_post',
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'url2',
        timestamp: 2,
      })
    );

    // Non-author cannot update
    await assertFails(
      setDoc(doc(alice, 'posts/update_post'), {
        postUID: 'update_post',
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'hacked',
        timestamp: 3,
      })
    );

    // Non-author cannot delete
    await assertFails(deleteDoc(doc(alice, 'posts/update_post')));

    // Author can delete
    await assertSucceeds(deleteDoc(doc(me, 'posts/update_post')));
  });

  test('Unfiltered post queries are denied', async () => {
    // Unfiltered query fails
    await assertFails(getDocs(collection(me, 'posts')));

    // Constrained query succeeds
    await assertSucceeds(
      getDocs(query(collection(me, 'posts'), where('ownerId', 'in', ['u1', 'u2'])))
    );
  });

  // Items
  test('Only owner can read their own items', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'items/my_item'), {
        ownerId: 'me',
        name: 'My Item',
        imageURL: 'url',
      })
    );

    await assertSucceeds(getDoc(doc(me, 'items/my_item')));

    // Non-owner cannot read
    await assertFails(getDoc(doc(alice, 'items/my_item')));
  });

  test('Only owner can create, update, and delete their items', async () => {
    // Create with correct ownerId
    await assertSucceeds(
      setDoc(doc(me, 'items/item1'), {
        ownerId: 'me',
        name: 'Item',
        imageURL: 'url',
      })
    );

    // Cannot create with wrong ownerId
    await assertFails(
      setDoc(doc(me, 'items/item2'), {
        ownerId: 'u1',
        name: 'Item',
        imageURL: 'url',
      })
    );

    // Owner can update
    await assertSucceeds(
      setDoc(doc(me, 'items/item1'), {
        ownerId: 'me',
        name: 'Updated',
        imageURL: 'url2',
      })
    );

    // Non-owner cannot update or delete
    await assertFails(
      setDoc(doc(alice, 'items/item1'), {
        ownerId: 'me',
        name: 'Hacked',
        imageURL: 'url3',
      })
    );
    await assertFails(deleteDoc(doc(alice, 'items/item1')));

    // Owner can delete
    await assertSucceeds(deleteDoc(doc(me, 'items/item1')));
  });
});