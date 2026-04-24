/*******************************************************************************
 * Copyright 2018 572682
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.ode.traveler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.springframework.http.HttpStatus;

import us.dot.its.jpo.ode.rsu.RsuProperties;
import us.dot.its.jpo.ode.snmp.SnmpSession;

@ExtendWith(MockitoExtension.class)
public class TimDeleteControllerTest {

   @Mock
   RsuProperties injectableRsuProperties;

   @Mock
   ResponseEvent mockResponseEvent;

   @InjectMocks
   TimDeleteController testTimDeleteController;

   private final String defaultRSUNullUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\"}";
   private final String fourDot1RSUNullUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"snmpProtocol\":\"FOURDOT1\"}";
   private final String ntcip1218RSUNullUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"snmpProtocol\":\"NTCIP1218\"}";

   private final String defaultRSUNotNullUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuUsername\":\"user\",\"rsuPassword\":\"pass\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\"}";
   private final String fourDot1RSUNotNullUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuUsername\":\"user\",\"rsuPassword\":\"pass\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"snmpProtocol\":\"FOURDOT1\"}";
   private final String ntcip1218RSUNotNullUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuUsername\":\"user\",\"rsuPassword\":\"pass\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"snmpProtocol\":\"NTCIP1218\"}";

   private final String defaultRSUBlankUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuUsername\":\"\",\"rsuPassword\":\"\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\"}";
   private final String fourDot1RSUBlankUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuUsername\":\"\",\"rsuPassword\":\"\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"snmpProtocol\":\"FOURDOT1\"}";
   private final String ntcip1218RSUBlankUserPass = "{\"rsuTarget\":\"10.10.10.10\",\"rsuUsername\":\"\",\"rsuPassword\":\"\",\"rsuRetries\":\"3\",\"rsuTimeout\":\"5000\",\"snmpProtocol\":\"NTCIP1218\"}";

   /**
    * Opens a {@link MockedConstruction} for {@link SnmpSession} with the given initializer applied
    * to every constructed instance. Use inside a try-with-resources block.
    */
   private static MockedConstruction<SnmpSession> mockSnmpSession(Consumer<SnmpSession> initializer) {
      return mockConstruction(SnmpSession.class, (mock, ctx) -> initializer.accept(mock));
   }

   /**
    * Opens a {@link MockedConstruction} for {@link SnmpSession} whose constructor throws the given
    * exception. Use inside a try-with-resources block.
    */
   private static MockedConstruction<SnmpSession> mockSnmpSessionThrowing(Throwable toThrow) {
      return mockConstruction(SnmpSession.class, (mock, ctx) -> { throw toThrow; });
   }

   @BeforeEach
   void resetResponseEvent() {
      // MockitoExtension creates a fresh mock per test; no-op guard kept for clarity.
   }

   @Test
   public void deleteShouldReturnBadRequestWhenNull() {
      assertEquals(HttpStatus.BAD_REQUEST,
            testTimDeleteController.deleteTim(null, 42).getStatusCode());
   }

   @Test
   @Disabled("TODO: cannot simulate SnmpSession constructor throwing IOException with pure Mockito "
         + "(MockedConstruction wraps initializer throwables in MockitoException). "
         + "Unblock by injecting a SnmpSession factory into TimDeleteController or adopting PowerMock.")
   public void deleteShouldCatchSessionIOException() {
      try (MockedConstruction<SnmpSession> ignored =
                 mockSnmpSessionThrowing(new IOException("testException123"))) {
         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
               testTimDeleteController.deleteTim(defaultRSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   @Disabled("TODO: cannot simulate SnmpSession constructor throwing IOException with pure Mockito "
         + "(MockedConstruction wraps initializer throwables in MockitoException). "
         + "Unblock by injecting a SnmpSession factory into TimDeleteController or adopting PowerMock.")
   public void deleteShouldCatchSessionIOException_fourDot1RSU() {
      try (MockedConstruction<SnmpSession> ignored =
                 mockSnmpSessionThrowing(new IOException("testException123"))) {
         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
               testTimDeleteController.deleteTim(fourDot1RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   @Disabled("TODO: cannot simulate SnmpSession constructor throwing IOException with pure Mockito "
         + "(MockedConstruction wraps initializer throwables in MockitoException). "
         + "Unblock by injecting a SnmpSession factory into TimDeleteController or adopting PowerMock.")
   public void deleteShouldCatchSessionIOException_ntcip1218RSU() {
      try (MockedConstruction<SnmpSession> ignored =
                 mockSnmpSessionThrowing(new IOException("testException123"))) {
         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
               testTimDeleteController.deleteTim(ntcip1218RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   @Disabled("TODO: cannot simulate SnmpSession constructor throwing NullPointerException with pure Mockito "
         + "(MockedConstruction wraps initializer throwables in MockitoException). "
         + "Unblock by injecting a SnmpSession factory into TimDeleteController or adopting PowerMock.")
   public void deleteShouldCatchSessionNullPointerException() {
      try (MockedConstruction<SnmpSession> ignored =
                 mockSnmpSessionThrowing(new NullPointerException("testException123"))) {
         assertEquals(HttpStatus.BAD_REQUEST,
               testTimDeleteController.deleteTim(defaultRSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   @Disabled("TODO: cannot simulate SnmpSession constructor throwing NullPointerException with pure Mockito "
         + "(MockedConstruction wraps initializer throwables in MockitoException). "
         + "Unblock by injecting a SnmpSession factory into TimDeleteController or adopting PowerMock.")
   public void deleteShouldCatchSessionNullPointerException_fourDot1RSU() {
      try (MockedConstruction<SnmpSession> ignored =
                 mockSnmpSessionThrowing(new NullPointerException("testException123"))) {
         assertEquals(HttpStatus.BAD_REQUEST,
               testTimDeleteController.deleteTim(fourDot1RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   @Disabled("TODO: cannot simulate SnmpSession constructor throwing NullPointerException with pure Mockito "
         + "(MockedConstruction wraps initializer throwables in MockitoException). "
         + "Unblock by injecting a SnmpSession factory into TimDeleteController or adopting PowerMock.")
   public void deleteShouldCatchSessionNullPointerException_ntcip1218RSU() {
      try (MockedConstruction<SnmpSession> ignored =
                 mockSnmpSessionThrowing(new NullPointerException("testException123"))) {
         assertEquals(HttpStatus.BAD_REQUEST,
               testTimDeleteController.deleteTim(ntcip1218RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteShouldCatchSnmpSetException() throws IOException {
      try (MockedConstruction<SnmpSession> ignored = mockSnmpSession(session -> {
         try {
            when(session.set(any(PDU.class), any(), any(), anyBoolean()))
                  .thenThrow(new IOException("testSnmpException123"));
         } catch (IOException e) {
            throw new AssertionError(e);
         }
      })) {
         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
               testTimDeleteController.deleteTim(defaultRSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteShouldCatchSnmpSetException_fourDot1RSU() throws IOException {
      try (MockedConstruction<SnmpSession> ignored = mockSnmpSession(session -> {
         try {
            when(session.set(any(PDU.class), any(), any(), anyBoolean()))
                  .thenThrow(new IOException("testSnmpException123"));
         } catch (IOException e) {
            throw new AssertionError(e);
         }
      })) {
         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
               testTimDeleteController.deleteTim(fourDot1RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteShouldCatchSnmpSetException_ntcip1218RSU() throws IOException {
      try (MockedConstruction<SnmpSession> ignored = mockSnmpSession(session -> {
         try {
            when(session.set(any(PDU.class), any(), any(), anyBoolean()))
                  .thenThrow(new IOException("testSnmpException123"));
         } catch (IOException e) {
            throw new AssertionError(e);
         }
      })) {
         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
               testTimDeleteController.deleteTim(ntcip1218RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestTimeout1() throws IOException {
      try (MockedConstruction<SnmpSession> ignored = mockSnmpSession(session -> {
         try {
            when(session.set(any(PDU.class), any(), any(), anyBoolean())).thenReturn(null);
         } catch (IOException e) {
            throw new AssertionError(e);
         }
      })) {
         assertEquals(HttpStatus.REQUEST_TIMEOUT,
               testTimDeleteController.deleteTim(defaultRSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestTimeout1_fourDot1RSU() throws IOException {
      try (MockedConstruction<SnmpSession> ignored = mockSnmpSession(session -> {
         try {
            when(session.set(any(PDU.class), any(), any(), anyBoolean())).thenReturn(null);
         } catch (IOException e) {
            throw new AssertionError(e);
         }
      })) {
         assertEquals(HttpStatus.REQUEST_TIMEOUT,
               testTimDeleteController.deleteTim(fourDot1RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestTimeout1_ntcip1218RSU() throws IOException {
      try (MockedConstruction<SnmpSession> ignored = mockSnmpSession(session -> {
         try {
            when(session.set(any(PDU.class), any(), any(), anyBoolean())).thenReturn(null);
         } catch (IOException e) {
            throw new AssertionError(e);
         }
      })) {
         assertEquals(HttpStatus.REQUEST_TIMEOUT,
               testTimDeleteController.deleteTim(ntcip1218RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestTimeout2() throws IOException {
      when(mockResponseEvent.getResponse()).thenReturn(null);
      try (MockedConstruction<SnmpSession> ignored = stubSessionReturning(mockResponseEvent)) {
         assertEquals(HttpStatus.REQUEST_TIMEOUT,
               testTimDeleteController.deleteTim(defaultRSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestTimeout2_fourDot1RSU() throws IOException {
      when(mockResponseEvent.getResponse()).thenReturn(null);
      try (MockedConstruction<SnmpSession> ignored = stubSessionReturning(mockResponseEvent)) {
         assertEquals(HttpStatus.REQUEST_TIMEOUT,
               testTimDeleteController.deleteTim(fourDot1RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestTimeout2_ntcip1218RSU() throws IOException {
      when(mockResponseEvent.getResponse()).thenReturn(null);
      try (MockedConstruction<SnmpSession> ignored = stubSessionReturning(mockResponseEvent)) {
         assertEquals(HttpStatus.REQUEST_TIMEOUT,
               testTimDeleteController.deleteTim(ntcip1218RSUNullUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestOK() throws IOException {
      PDU pdu = Mockito.mock(PDU.class);
      when(pdu.getErrorStatus()).thenReturn(0);
      when(mockResponseEvent.getResponse()).thenReturn(pdu);
      try (MockedConstruction<SnmpSession> ignored = stubSessionReturning(mockResponseEvent)) {
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(defaultRSUNullUserPass, 42).getStatusCode());
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(defaultRSUNotNullUserPass, 42).getStatusCode());
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(defaultRSUBlankUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestOK_fourDot1RSU() throws IOException {
      PDU pdu = Mockito.mock(PDU.class);
      when(pdu.getErrorStatus()).thenReturn(0);
      when(mockResponseEvent.getResponse()).thenReturn(pdu);
      try (MockedConstruction<SnmpSession> ignored = stubSessionReturning(mockResponseEvent)) {
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(fourDot1RSUNullUserPass, 42).getStatusCode());
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(fourDot1RSUNotNullUserPass, 42).getStatusCode());
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(fourDot1RSUBlankUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestOK_ntcip1218RSU() throws IOException {
      PDU pdu = Mockito.mock(PDU.class);
      when(pdu.getErrorStatus()).thenReturn(0);
      when(mockResponseEvent.getResponse()).thenReturn(pdu);
      try (MockedConstruction<SnmpSession> ignored = stubSessionReturning(mockResponseEvent)) {
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(ntcip1218RSUNullUserPass, 42).getStatusCode());
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(ntcip1218RSUNotNullUserPass, 42).getStatusCode());
         assertEquals(HttpStatus.OK,
               testTimDeleteController.deleteTim(ntcip1218RSUBlankUserPass, 42).getStatusCode());
      }
   }

   @Test
   public void deleteTestMessageAlreadyExists() throws IOException {
      assertBadRequestForErrorCode(defaultRSUNullUserPass, 12);
   }

   @Test
   public void deleteTestMessageAlreadyExists_fourDot1RSU() throws IOException {
      assertBadRequestForErrorCode(fourDot1RSUNullUserPass, 12);
   }

   @Test
   public void deleteTestMessageAlreadyExists_ntcip1218RSU() throws IOException {
      assertBadRequestForErrorCode(ntcip1218RSUNullUserPass, 12);
   }

   @Test
   public void deleteTestInvalidIndex() throws IOException {
      assertBadRequestForErrorCode(defaultRSUNullUserPass, 10);
   }

   @Test
   public void deleteTestInvalidIndex_fourDot1RSU() throws IOException {
      assertBadRequestForErrorCode(fourDot1RSUNullUserPass, 10);
   }

   @Test
   public void deleteTestInvalidIndex_ntcip1218RSU() throws IOException {
      assertBadRequestForErrorCode(ntcip1218RSUNullUserPass, 10);
   }

   @Test
   public void deleteTestUnknownErrorCode() throws IOException {
      assertBadRequestForErrorCode(defaultRSUNullUserPass, 5);
   }

   @Test
   public void deleteTestUnknownErrorCode_fourDot1RSU() throws IOException {
      assertBadRequestForErrorCode(fourDot1RSUNullUserPass, 5);
   }

   @Test
   public void deleteTestUnknownErrorCode_ntcip1218RSU() throws IOException {
      assertBadRequestForErrorCode(ntcip1218RSUNullUserPass, 5);
   }

   private void assertBadRequestForErrorCode(String rsuJson, int errorStatus) throws IOException {
      PDU pdu = Mockito.mock(PDU.class);
      when(pdu.getErrorStatus()).thenReturn(errorStatus);
      when(pdu.getErrorStatusText()).thenReturn("mocked error");
      when(mockResponseEvent.getResponse()).thenReturn(pdu);
      try (MockedConstruction<SnmpSession> ignored = stubSessionReturning(mockResponseEvent)) {
         assertEquals(HttpStatus.BAD_REQUEST,
               testTimDeleteController.deleteTim(rsuJson, 42).getStatusCode());
      }
   }

   private static MockedConstruction<SnmpSession> stubSessionReturning(ResponseEvent event) {
      return mockSnmpSession(session -> {
         try {
            when(session.set(any(PDU.class), any(), any(), anyBoolean())).thenReturn(event);
         } catch (IOException e) {
            throw new AssertionError(e);
         }
      });
   }
}
