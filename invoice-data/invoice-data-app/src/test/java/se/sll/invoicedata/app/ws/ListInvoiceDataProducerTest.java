/**
 * Copyright (c) 2013 SLL. <http://sll.se>
 *
 * This file is part of Invoice-Data.
 *
 *     Invoice-Data is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Invoice-Data is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Invoice-Data.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */

/**
 * 
 */
package se.sll.invoicedata.app.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import riv.sll.invoicedata._1.Event;
import riv.sll.invoicedata._1.RegisteredEvent;
import riv.sll.invoicedata._1.ResultCodeEnum;
import riv.sll.invoicedata.createinvoicedataresponder._1.CreateInvoiceDataRequest;
import riv.sll.invoicedata.createinvoicedataresponder._1.CreateInvoiceDataResponse;
import riv.sll.invoicedata.getinvoicedataresponder._1.GetInvoiceDataRequest;
import riv.sll.invoicedata.getinvoicedataresponder._1.GetInvoiceDataResponse;
import riv.sll.invoicedata.listinvoicedata._1.rivtabp21.ListInvoiceDataResponderInterface;
import riv.sll.invoicedata.listinvoicedataresponder._1.ListInvoiceDataRequest;
import riv.sll.invoicedata.listinvoicedataresponder._1.ListInvoiceDataResponse;
import riv.sll.invoicedata.registerinvoicedataresponder._1.RegisterInvoiceDataResponse;
import se.sll.invoicedata.app.TestSupport;
import se.sll.invoicedata.core.service.impl.CoreUtil;

/**
 * @author muqkha
 *
 */
public class ListInvoiceDataProducerTest extends TestSupport {

	private static ListInvoiceDataResponderInterface listIDRInterface;
	private static Event event;

	@BeforeClass
	public static void setUp() {
		listIDRInterface = getListInvoiceDataService();
		testPrerequisiteStep_ListInvoiceData();
	}

	@AfterClass
	public static void tearDown() {
		listIDRInterface = null;
	}
	
	/**
	 * Steps:
	 * 1. Register event -> Result should be OK
	 * 2. GetInvoice -> Returns registered event list
	 * 		The list should not be empty.
	 * 3. Iterate the list and store RegisteredEventId
	 * 4. Use RegisteredEventId list to request
	 * 		CreateInvoice -> Returns a reference id for
	 * 		the created InvoiceData
	 */
	private static String testPrerequisiteStep_ListInvoiceData() {
		
		//1. Register event 
		event = createRandomEventData();
		RegisterInvoiceDataResponse regIDResp = RegisterInvoiceDataProducerTest.
				getRegisterInvoiceDataService().registerInvoiceData(LOGICAL_ADDRESS, event);
		
		// 1. Register event -> Result should be OK 
		Assert.assertNotNull(regIDResp);
		Assert.assertEquals(ResultCodeEnum.OK, regIDResp.getResultCode().getCode());
		
		//2. GetInvoice		 
		GetInvoiceDataRequest getIDReq = new GetInvoiceDataRequest();
		getIDReq.setSupplierId(event.getSupplierId());
		//getIDReq.setPaymentResponsible(event.getPaymentResponsible());
		
		GetInvoiceDataResponse invoiceResp = GetInvoiceDataProducerTest.
				getGetInvoiceDataService().getInvoiceData(LOGICAL_ADDRESS, getIDReq);
		
		//2. GetInvoice -> Returns registered event list
		//		The list should not be empty.
		Assert.assertNotNull(invoiceResp);
		Assert.assertNotNull(invoiceResp.getRegisteredEventList());
		
		List<RegisteredEvent> regEventList = invoiceResp.getRegisteredEventList();
		//3. Iterate the list and store RegisteredEventId
		List<String> ackIdList = new ArrayList<String>();
		for (RegisteredEvent regEvent : regEventList) {
			ackIdList.add(regEvent.getAcknowledgementId());
		}
		
		// 4. Use RegisteredEventId list to request CeateInvoice
		CreateInvoiceDataRequest invoiceDataRequest = new CreateInvoiceDataRequest();
		invoiceDataRequest.setSupplierId(event.getSupplierId());
		invoiceDataRequest.setPaymentResponsible(event.getPaymentResponsible());
		invoiceDataRequest.setCreatedBy("test");
		invoiceDataRequest.getAcknowledgementIdList().addAll(ackIdList);

		CreateInvoiceDataResponse createIDResp = CreateInvoiceDataProducerTest.
				getCreateInvoiceDataService().createInvoiceData(LOGICAL_ADDRESS, invoiceDataRequest);
		
		// 4. Use RegisteredEventId list to request
		// 		CreateInvoice -> Returns a reference id for the created InvoiceData
		Assert.assertNotNull(createIDResp.getReferenceId());
		
		return createIDResp.getReferenceId();
	}
	
	@Test
	public void testListInvoiceData_With_All_InParams_Result_Success() {
		ListInvoiceDataRequest request = new ListInvoiceDataRequest();
		request.setSupplierId(event.getSupplierId());
		request.setFromDate(getCurrentDate());
		request.setToDate(getCurrentDate());
		request.setPaymentResponsible(event.getPaymentResponsible());
		
		ListInvoiceDataResponse invoiceDataResponse = listIDRInterface.listInvoiceData(LOGICAL_ADDRESS, request);
		
		Assert.assertNotNull(invoiceDataResponse);
		
		Assert.assertEquals(ResultCodeEnum.OK, invoiceDataResponse.getResultCode().getCode());
		
		Assert.assertNotNull(invoiceDataResponse.getInvoiceDataList());
		
	}
	
	@Test
	public void testListAllInvoiceData_With_Different_Alternatives() {

		// Request with only supplier id
		ListInvoiceDataRequest invoiceListRequest = new ListInvoiceDataRequest();
		invoiceListRequest.setSupplierId(event.getSupplierId());

		ListInvoiceDataResponse invoiceDataResponse = listIDRInterface.listInvoiceData(LOGICAL_ADDRESS, invoiceListRequest);

		assertNotNull(invoiceDataResponse);
		assertEquals(event.getPaymentResponsible(), invoiceDataResponse.getInvoiceDataList().get(0).getPaymentResponsible());

		// Request with only payment responsible
		invoiceListRequest = new ListInvoiceDataRequest();
		invoiceListRequest.setPaymentResponsible(event.getPaymentResponsible());

		invoiceDataResponse = listIDRInterface.listInvoiceData(LOGICAL_ADDRESS, invoiceListRequest);

		assertNotNull(invoiceDataResponse);
		assertEquals(event.getPaymentResponsible(), invoiceDataResponse.getInvoiceDataList().get(0).getPaymentResponsible());

		// Request with both payment responsible and supplier id
		invoiceListRequest = new ListInvoiceDataRequest();
		invoiceListRequest.setSupplierId(event.getSupplierId());
		invoiceListRequest.setPaymentResponsible(event.getPaymentResponsible());

		invoiceDataResponse = listIDRInterface.listInvoiceData(LOGICAL_ADDRESS, invoiceListRequest);

		assertNotNull(invoiceDataResponse);
		assertEquals(event.getSupplierId(), invoiceDataResponse.getInvoiceDataList().get(0).getSupplierId());
		assertEquals(event.getPaymentResponsible(), invoiceDataResponse.getInvoiceDataList().get(0).getPaymentResponsible());

		// Request with only from date; fromDate (setting year to 1 year back
		// from current year)
		invoiceListRequest = new ListInvoiceDataRequest();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
		invoiceListRequest.setFromDate(CoreUtil.toXMLGregorianCalendar(cal.getTime()));
		invoiceListRequest.setSupplierId(event.getSupplierId());

		invoiceDataResponse = listIDRInterface.listInvoiceData(LOGICAL_ADDRESS, invoiceListRequest);
		
		assertNotNull(invoiceDataResponse);
		assertEquals(event.getSupplierId(), invoiceDataResponse.getInvoiceDataList().get(0).getSupplierId());

		// Request with only to date; toDate
		invoiceListRequest = new ListInvoiceDataRequest();
		invoiceListRequest.setPaymentResponsible(event.getPaymentResponsible());
		cal.set(Calendar.YEAR, 9999);
		invoiceListRequest.setToDate(CoreUtil.toXMLGregorianCalendar(cal.getTime()));

		invoiceDataResponse = listIDRInterface.listInvoiceData(LOGICAL_ADDRESS, invoiceListRequest);
		
		assertNotNull(invoiceDataResponse);
		assertEquals(event.getPaymentResponsible(), invoiceDataResponse.getInvoiceDataList().get(0).getPaymentResponsible());
	}

	static ListInvoiceDataResponderInterface getListInvoiceDataService() {
		if (listIDRInterface == null) {
		    listIDRInterface = createWebServiceConsumer(ListInvoiceDataResponderInterface.class);
		}
		return listIDRInterface;
	}
}
