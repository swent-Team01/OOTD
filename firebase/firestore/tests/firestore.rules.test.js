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

      await assertSucceeds(getDoc(doc(me, 'posts/my_post')));
      await assertSucceeds(getDoc(doc(alice, 'posts/my_post')));
      await assertFails(getDoc(doc(charlie, 'posts/my_post')));
    });

  test('Unfiltered post queries are denied', async () => {
    // Unfiltered query fails
    await assertFails(getDocs(collection(me, 'posts')));

    // Constrained query succeeds
    await assertSucceeds(
      getDocs(query(collection(me, 'posts'), where('ownerId', 'in', ['u1', 'u2'])))
    );
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
