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


//Tests for rules made by AI and verified by human
describe('Firestore rules - OOTD friend-only feed (intended rules)', async () => {
  const env = await setup();

  const meCtx = env.authenticatedContext('me');
  const me = meCtx.firestore();

  const aliceCtx = env.authenticatedContext('u1');
  const alice = aliceCtx.firestore();

  const bobCtx = env.authenticatedContext('u3');
  const bob = bobCtx.firestore();

  beforeAll(async () => {
    await setupFirestore(env);
  });

  afterAll(async () => {
    await env.clearFirestore();
    await env.cleanup();
  });

  // items: top-level open; everything else default deny unless matched earlier
  test('Top-level items are freely readable/writable; unrelated collections are denied', async () => {
    await assertSucceeds(setDoc(doc(me, 'items/i1'), { any: 'value' }));
    await assertSucceeds(getDoc(doc(me, 'items/i1')));

    // Default deny for unrelated collection
    await assertFails(setDoc(doc(me, 'misc/m1'), { nope: true }));
  });

  // /users rules
  test('Authenticated users can read any user doc and write (current rules)', async () => {
    await assertSucceeds(getDoc(doc(me, 'users/me')));
    await assertSucceeds(getDoc(doc(me, 'users/u1')));

    await assertSucceeds(
      setDoc(doc(me, 'users/me'), { uid: 'me', name: 'Me', friendList: [] }, { merge: true })
    );
    // Writes to other users are allowed by current rules
    await assertSucceeds(
      setDoc(doc(alice, 'users/me'), { uid: 'me', touchedBy: 'u1' }, { merge: true })
    );
  });

  // posts read rules
  test('Author can read their own post even without mutual friendship', async () => {
    const selfDb = env.authenticatedContext('me').firestore();
    await assertSucceeds(
      setDoc(doc(selfDb, 'posts/self_post'), {
        postUID: 'self_post',
        uid: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: Date.now(),
      })
    );
    await assertSucceeds(getDoc(doc(selfDb, 'posts/self_post')));
  });

  test('Friends can read posts; non-friends and one-way are denied', async () => {
    // Seeded: me is mutual friends with u1 and u2; u3 is one-way; u9 is non-friend
    await assertSucceeds(getDoc(doc(me, 'posts/p1'))); // u1
    await assertSucceeds(getDoc(doc(me, 'posts/p2'))); // u2

    await assertFails(getDoc(doc(me, 'posts/p3'))); // u3
    await assertFails(getDoc(doc(me, 'posts/p9'))); // u9
  });

  test('List query: allowed when constrained to mutual friends; unfiltered is denied', async () => {
    // Constrained to mutual friends
    await assertSucceeds(
      getDocs(query(collection(me, 'posts'), where('uid', 'in', ['u1', 'u2'])))
    );

    // Unfiltered should fail because it would include non-friend docs
    await assertFails(getDocs(collection(me, 'posts')));
  });

  // posts write rules
  test('Create: only allowed when uid matches the authenticated user', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_me'), {
        postUID: 'post_me',
        uid: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: Date.now(),
      })
    );

    await assertFails(
      setDoc(doc(me, 'posts/post_wrong_uid'), {
        postUID: 'post_wrong_uid',
        uid: 'someoneElse',
        name: 'Me',
        outfitURL: 'url',
        timestamp: Date.now(),
      })
    );
  });

  test('Update: only original author can update; uid/postUID may change', async () => {
    // Seed a post by me
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_edit'), {
        postUID: 'post_edit',
        uid: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: 1,
      })
    );

    // Author can update content
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_edit'), {
        postUID: 'post_edit',
        uid: 'me',
        name: 'Me',
        outfitURL: 'url2',
        timestamp: 2,
      })
    );

    // Author can change identifiers under current rules
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_edit'), {
        postUID: 'changed_post_uid',
        uid: 'me', // keep uid to retain authorship for later delete test
        name: 'Me',
        outfitURL: 'url3',
        timestamp: 3,
      })
    );

    // Non-author cannot update
    await assertFails(
      setDoc(doc(alice, 'posts/post_edit'), {
        postUID: 'attempt_by_other',
        uid: 'me',
        name: 'Me',
        outfitURL: 'hacked',
        timestamp: 999,
      })
    );
  });

  test('Delete: only the original author can delete', async () => {
    // Seed a post by me
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_to_delete'), {
        postUID: 'post_to_delete',
        uid: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: 1,
      })
    );

    // Non-author delete denied
    await assertFails(deleteDoc(doc(alice, 'posts/post_to_delete')));

    // Author delete allowed
    await assertSucceeds(deleteDoc(doc(me, 'posts/post_to_delete')));
  });
});
