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
describe('Firestore rules - OOTD friend-only feed (intended rules)', () => {
  let env;
  let meCtx, me;
  let aliceCtx, alice;
  let bobCtx, bob;

  beforeAll(async () => {
    env = await setup();

    meCtx = env.authenticatedContext('me');
    me = meCtx.firestore();

    aliceCtx = env.authenticatedContext('u1');
    alice = aliceCtx.firestore();

    bobCtx = env.authenticatedContext('u3');
    bob = bobCtx.firestore();

    await setupFirestore(env);
  });

  afterAll(async () => {
    await env.clearFirestore();
    await env.cleanup();
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
        ownerId: 'me',
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
      getDocs(query(collection(me, 'posts'), where('ownerId', 'in', ['u1', 'u2'])))
    );

    // Unfiltered should fail because it would include non-friend docs
    await assertFails(getDocs(collection(me, 'posts')));
  });

  // posts write rules
  test('Create: only allowed when uid matches the authenticated user', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_me'), {
        postUID: 'post_me',
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: Date.now(),
      })
    );

    await assertFails(
      setDoc(doc(me, 'posts/post_wrong_uid'), {
        postUID: 'post_wrong_uid',
        ownerId: 'someoneElse',
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
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'url',
        timestamp: 1,
      })
    );

    // Author can update content
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_edit'), {
        postUID: 'post_edit',
        ownerId: 'me',
        name: 'Me',
        outfitURL: 'url2',
        timestamp: 2,
      })
    );

    // Author can change identifiers under current rules
    await assertSucceeds(
      setDoc(doc(me, 'posts/post_edit'), {
        postUID: 'changed_post_uid',
        ownerId: 'me', // keep uid to retain authorship for later delete test
        name: 'Me',
        outfitURL: 'url3',
        timestamp: 3,
      })
    );

    // Non-author cannot update
    await assertFails(
      setDoc(doc(alice, 'posts/post_edit'), {
        postUID: 'attempt_by_other',
        ownerId: 'me',
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
        ownerId: 'me',
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

  // /consents rules
  test('User can create their own consent record', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'consents/consent_me'), {
        consentUuid: 'consent_me',
        userId: 'me',
        timestamp: Date.now(),
        version: '1.0',
      })
    );
  });

  test('User cannot create consent record for another user', async () => {
    await assertFails(
      setDoc(doc(me, 'consents/consent_alice'), {
        consentUuid: 'consent_alice',
        userId: 'u1',
        timestamp: Date.now(),
        version: '1.0',
      })
    );
  });

  test('User can read their own consent record', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'consents/consent_me_read'), {
        consentUuid: 'consent_me_read',
        userId: 'me',
        timestamp: Date.now(),
        version: '1.0',
      })
    );
    await assertSucceeds(getDoc(doc(me, 'consents/consent_me_read')));
  });

  test('User cannot read another user consent record', async () => {
    await assertSucceeds(
      setDoc(doc(alice, 'consents/consent_alice_private'), {
        consentUuid: 'consent_alice_private',
        userId: 'u1',
        timestamp: Date.now(),
        version: '1.0',
      })
    );
    await assertFails(getDoc(doc(me, 'consents/consent_alice_private')));
  });

  test('User can update their own consent record', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'consents/consent_me_update'), {
        consentUuid: 'consent_me_update',
        userId: 'me',
        timestamp: 1000,
        version: '1.0',
      })
    );
    await assertSucceeds(
      setDoc(doc(me, 'consents/consent_me_update'), {
        consentUuid: 'consent_me_update',
        userId: 'me',
        timestamp: 2000,
        version: '1.1',
      })
    );
  });

  test('User cannot update another user consent record', async () => {
    await assertSucceeds(
      setDoc(doc(alice, 'consents/consent_alice_update'), {
        consentUuid: 'consent_alice_update',
        userId: 'u1',
        timestamp: 1000,
        version: '1.0',
      })
    );
    await assertFails(
      setDoc(doc(me, 'consents/consent_alice_update'), {
        consentUuid: 'consent_alice_update',
        userId: 'u1',
        timestamp: 2000,
        version: '1.1',
      })
    );
  });

  test('User can delete their own consent record', async () => {
    await assertSucceeds(
      setDoc(doc(me, 'consents/consent_me_delete'), {
        consentUuid: 'consent_me_delete',
        userId: 'me',
        timestamp: Date.now(),
        version: '1.0',
      })
    );
    await assertSucceeds(deleteDoc(doc(me, 'consents/consent_me_delete')));
  });

  test('User cannot delete another user consent record', async () => {
    await assertSucceeds(
      setDoc(doc(bob, 'consents/consent_bob_delete'), {
        consentUuid: 'consent_bob_delete',
        userId: 'u3',
        timestamp: Date.now(),
        version: '1.0',
      })
    );
    await assertFails(deleteDoc(doc(me, 'consents/consent_bob_delete')));
  });
});
