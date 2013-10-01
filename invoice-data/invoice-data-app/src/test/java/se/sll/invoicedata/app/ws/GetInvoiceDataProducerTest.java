/**
 *  Copyright (c) 2013 SLL <http://sll.se/>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * 
 */
package se.sll.invoicedata.app.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.junit.Assert;
import org.junit.Test;

import riv.sll.invoicedata._1.Event;
import riv.sll.invoicedata._1.RegisteredEvent;
import riv.sll.invoicedata._1.ResultCodeEnum;
import riv.sll.invoicedata.createinvoicedata._1.rivtabp21.CreateInvoiceDataResponderInterface;
import riv.sll.invoicedata.createinvoicedataresponder._1.CreateInvoiceDataRequest;
import riv.sll.invoicedata.createinvoicedataresponder._1.CreateInvoiceDataResponse;
import riv.sll.invoicedata.getinvoicedata._1.rivtabp21.GetInvoiceDataResponderInterface;
import riv.sll.invoicedata.getinvoicedataresponder._1.GetInvoiceDataRequest;
import riv.sll.invoicedata.getinvoicedataresponder._1.GetInvoiceDataResponse;
import riv.sll.invoicedata.registerinvoicedata._1.rivtabp21.RegisterInvoiceDataResponderInterface;
import se.sll.invoicedata.app.TestSupport;

/**
 * @author muqkha
 * 
 */
public class GetInvoiceDataProducerTest extends TestSupport {

	private final String LOGICAL_ADDRESS = "loc:TolkPortalen";

	@Test
	public void get_InvoiceData_Unprocessed_Events_Success() {

		Event event = createSampleEventData();
		RegisterInvoiceDataProducerTest.getRegisterInvoiceDataService()
				.registerInvoiceData(LOGICAL_ADDRESS, event);

		GetInvoiceDataRequest request = new GetInvoiceDataRequest();
		request.setSupplierId(event.getSupplierId());
		request.setPaymentResponsible(event.getPaymentResponsible());

		GetInvoiceDataResponse response = getGetInvoiceDataService()
				.getInvoiceData(LOGICAL_ADDRESS, request);

		Assert.assertNotNull(response);
		Assert.assertEquals(ResultCodeEnum.OK, response.getResultCode()
				.getCode());

		Assert.assertNotNull(response.getRegisteredEventList());
	}

	@Test
	public void get_InvoiceData_Incomplete_Request_Data_Fail() {
		Event event = createSampleEventData();
		RegisterInvoiceDataProducerTest.getRegisterInvoiceDataService()
				.registerInvoiceData(LOGICAL_ADDRESS, event);

		GetInvoiceDataRequest request = new GetInvoiceDataRequest();
		request.setSupplierId(event.getSupplierId());

		GetInvoiceDataResponse response = getGetInvoiceDataService()
				.getInvoiceData(LOGICAL_ADDRESS, request);

		Assert.assertNotNull(response);
		Assert.assertEquals(ResultCodeEnum.ERROR, response.getResultCode()
				.getCode());

		request.setSupplierId("");
		request.setPaymentResponsible(event.getPaymentResponsible());
		response = getGetInvoiceDataService().getInvoiceData(LOGICAL_ADDRESS,
				request);
	}

	@Test
	public void get_InvoiceData_Normal_Case_Success() {

		Event event = createSampleEventData();
		RegisterInvoiceDataProducerTest.getRegisterInvoiceDataService()
				.registerInvoiceData(LOGICAL_ADDRESS, event);

		GetInvoiceDataRequest request = new GetInvoiceDataRequest();
		request.setSupplierId(event.getSupplierId());
		request.setPaymentResponsible(event.getPaymentResponsible());

		GetInvoiceDataResponse response = getGetInvoiceDataService()
				.getInvoiceData(LOGICAL_ADDRESS, request);

		List<RegisteredEvent> regEventList = response.getRegisteredEventList();

		List<Long> eventRefList = new ArrayList<Long>();
		for (RegisteredEvent regEvent : regEventList) {
			eventRefList.add(regEvent.getId());
		}

		CreateInvoiceDataRequest invoiceDataRequest = new CreateInvoiceDataRequest();
		invoiceDataRequest.setSupplierId(event.getSupplierId());
		invoiceDataRequest.setPaymentResponsible(event.getPaymentResponsible());
		invoiceDataRequest.setCreatedBy("test");
		invoiceDataRequest.getEventRefIdList().addAll(eventRefList);

		CreateInvoiceDataProducerTest.getCreateInvoiceDataService()
				.createInvoiceData(LOGICAL_ADDRESS, invoiceDataRequest);

		Assert.assertNotNull(response);
		Assert.assertEquals(ResultCodeEnum.OK, response.getResultCode()
				.getCode());

		Assert.assertNotNull(response.getInvoiceDataList());
		Assert.assertTrue(response.getRegisteredEventList().size() > 0);
		Assert.assertTrue(response.getInvoiceDataList().size() > 0);
	}

	//@Test
	public void get_InvoiceData_Some_Processed_Some_Unprocessed_Success() {

		Event event = createSampleEventData();		
		RegisterInvoiceDataResponderInterface registerIDRInterface = 
				RegisterInvoiceDataProducerTest.getRegisterInvoiceDataService();
		
		//Registering two events; same supplier id
		registerIDRInterface.registerInvoiceData(LOGICAL_ADDRESS, event);
		event.setEventId("EID4321");
		registerIDRInterface.registerInvoiceData(LOGICAL_ADDRESS, event);

		GetInvoiceDataRequest getInvoiceReq = new GetInvoiceDataRequest();
		getInvoiceReq.setSupplierId(event.getSupplierId());
		getInvoiceReq.setPaymentResponsible(event.getPaymentResponsible());

		GetInvoiceDataResponse getInvoiceResp = getGetInvoiceDataService()
				.getInvoiceData(LOGICAL_ADDRESS, getInvoiceReq);
		
		Assert.assertNotNull(getInvoiceResp);
		//Two registered events back in response
		Assert.assertEquals(2, getInvoiceResp.getRegisteredEventList().size());
		
		List<RegisteredEvent> regEventList = getInvoiceResp.getRegisteredEventList();
		List<Long> eventRefList = new ArrayList<Long>();
		for (RegisteredEvent regEvent : regEventList) {
			eventRefList.add(regEvent.getId());
		}
		//Removing one item; generating only one invoice
		eventRefList.remove(0);  
		
		CreateInvoiceDataRequest invoiceDataRequest = new CreateInvoiceDataRequest();
		invoiceDataRequest.setSupplierId(event.getSupplierId());
		invoiceDataRequest.setPaymentResponsible(event.getPaymentResponsible());
		invoiceDataRequest.setCreatedBy("test");
		invoiceDataRequest.getEventRefIdList().addAll(eventRefList);

		CreateInvoiceDataResponderInterface createIDResp = CreateInvoiceDataProducerTest.getCreateInvoiceDataService();
		CreateInvoiceDataResponse createResp = createIDResp.createInvoiceData(LOGICAL_ADDRESS, invoiceDataRequest);

		Assert.assertNotNull(createResp);
		Assert.assertEquals(ResultCodeEnum.OK, getInvoiceResp.getResultCode()
				.getCode());	
		Assert.assertNotNull(createResp.getReferenceId());
		
		getInvoiceResp = getGetInvoiceDataService()
				.getInvoiceData(LOGICAL_ADDRESS, getInvoiceReq);
		Assert.assertNotNull(getInvoiceResp);
		//One invoiced data back in response
		Assert.assertEquals(1, getInvoiceResp.getInvoiceDataList().size());
		//Assert.assertEquals(1, getInvoiceResp.getInvoiceDataList().size());
	}

	private GetInvoiceDataResponderInterface getGetInvoiceDataService() {
		GetInvoiceDataResponderInterface iGetInvoiceDataResponder = null;

		final String URL = "http://localhost:8080/invoice-data-app/ws/getInvoiceData";
		// Endpoint.publish(URL, new RegisterInvoiceDataProducer());

		try {
			URL wsdlURL = new URL(URL + "?wsdl");

			String namespaceURI = "http://ws.app.invoicedata.sll.se/";
			String serviceName = "GetInvoiceDataProducerService";

			QName serviceQN = new QName(namespaceURI, serviceName);

			Service service = Service.create(wsdlURL, serviceQN);

			iGetInvoiceDataResponder = service
					.getPort(GetInvoiceDataResponderInterface.class);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return iGetInvoiceDataResponder;
	}

}
