package dev.pugrilla.altmanager.account;

import dev.pugrilla.altmanager.auth.SessionRefreshPolicyTest;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.UUID;
import net.minecraft.util.Session;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionPreparationTest {
   private static final UUID ID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

   private static final class Account extends AbstractAccount {
      private int refreshes;
      private int validations;
      private LoginResponseType refreshed = LoginResponseType.SUCCESS;
      private LoginResponseType validated = LoginResponseType.SUCCESS;

      Account(AccountType type, String token) {
         super(type, "TestUser", ID, token);
         setLastRefresh(System.currentTimeMillis());
      }
      protected LoginResult loginImpl() {
         ++refreshes;
         return refreshed == LoginResponseType.SUCCESS
            ? new LoginResult(new Session("TestUser", ID.toString(), "new-token", "mojang"), refreshed)
            : new LoginResult(refreshed);
      }
      protected LoginResult validateSession() {
         ++validations;
         return validated == LoginResponseType.SUCCESS ? new LoginResult(createSessionObject(), validated) : new LoginResult(validated);
      }
      protected void serialize(DataOutputStream output) { }
      protected void deserialize(DataInputStream input) { }
   }

   private Account validAccount() {
      return new Account(AccountType.BROWSER, SessionRefreshPolicyTest.token(System.currentTimeMillis() / 1000L + 3600L));
   }

   @Test public void expiredTokenRefreshesBeforeActivationWithoutRecordingUse() {
      Account account = new Account(AccountType.BROWSER, SessionRefreshPolicyTest.token(1L));
      assertTrue(account.prepareSession(true, false).isSuccessfulLogin());
      assertEquals(1, account.refreshes);
      assertEquals(0, account.validations);
      assertEquals("new-token", account.getAccessToken());
      assertEquals(0L, account.getLastUsed());
   }

   @Test public void serverRejectedTokenRefreshesEvenWhenExpiryIsInFuture() {
      Account account = validAccount();
      account.validated = LoginResponseType.INVALID_ACCOUNT;
      assertTrue(account.prepareSession(true, false).isSuccessfulLogin());
      assertEquals(1, account.validations);
      assertEquals(1, account.refreshes);
   }

   @Test public void failedRefreshKeepsPreviousTokenAndNeverReportsSuccess() {
      Account account = new Account(AccountType.BROWSER, "old-token");
      account.refreshed = LoginResponseType.INVALID_ACCOUNT;
      assertFalse(account.prepareSession(true, true).isSuccessfulLogin());
      assertEquals("old-token", account.getAccessToken());
      assertEquals(0L, account.getLastUsed());
   }

   @Test public void sessionOnlyAccountsAndDisabledRefreshDoNotReauthenticate() {
      Account sessionOnly = new Account(AccountType.SESSION, "old-token");
      sessionOnly.validated = LoginResponseType.INVALID_ACCOUNT;
      assertFalse(sessionOnly.prepareSession(true, true).isSuccessfulLogin());
      assertEquals(0, sessionOnly.refreshes);
      Account disabled = validAccount();
      disabled.validated = LoginResponseType.INVALID_ACCOUNT;
      assertFalse(disabled.prepareSession(false, true).isSuccessfulLogin());
      assertEquals(0, disabled.refreshes);
   }

   @Test public void rateLimitsAndNetworkFailuresDoNotTriggerRefreshStorms() {
      for (LoginResponseType response : new LoginResponseType[]{LoginResponseType.RATE_LIMITED, LoginResponseType.NETWORK_ERROR}) {
         Account account = validAccount();
         account.validated = response;
         assertEquals(response, account.prepareSession(true, false).getResponseType());
         assertEquals(0, account.refreshes);
      }
   }

   @Test public void accountDiagnosticsDoNotExposeTokensOrRecurseThroughRepository() {
      Account account = new Account(AccountType.BROWSER, "private-token");
      AccountRepository repository = new AccountRepository("Test", null, 1L, new dev.pugrilla.altmanager.storage.RepositoryEncryption(false));
      repository.addAccount(account);
      assertFalse(account.toString().contains("private-token"));
      assertFalse(repository.toString().contains("private-token"));
   }
}
