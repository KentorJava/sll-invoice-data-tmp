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

package se.sll.invoicedata.app.ws;

import java.util.Map;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;

import riv.sll.invoicedata._1.ResultCode;
import riv.sll.invoicedata._1.ResultCodeEnum;
import se.sll.invoicedata.core.access.Operation;
import se.sll.invoicedata.core.jmx.StatusBean;
import se.sll.invoicedata.core.security.User;
import se.sll.invoicedata.core.service.InvoiceDataErrorCodeEnum;
import se.sll.invoicedata.core.service.InvoiceDataService;
import se.sll.invoicedata.core.service.InvoiceDataServiceException;
import se.sll.invoicedata.core.service.OperationAccessConfigService;

/**
 * Abstracts generic logging and error handling for Web Service Producers.
 * 
 * @author Peter
 */
public abstract class AbstractProducer {

    private static final Logger log = LoggerFactory.getLogger("WS-API");
    
    @Autowired
    private StatusBean statusBean;
    
    @Autowired
    private InvoiceDataService invoiceDataService;
    
    @Autowired
    private OperationAccessConfigService operationAccessConfigService;
    
    @Resource
    private WebServiceContext webServiceContext;
    
    @Value("${security.acl}") 
    private String securityAccessList;
    
    @Value("${operation.acl}") 
    private String operationAccessList;
    
    /**
     * Creates a soap fault.
     * 
     * @param throwable the cause.
     * @return the soap fault object.
     */
    protected SoapFault createSoapFault(Throwable throwable) {
        final String msg = createLogMessage(throwable.toString());
        log.error(msg, throwable);
        
        final SoapFault soapFault = createSoapFault(msg);
        
        return soapFault;
    }
    
    /**
     * Creates a soap fault.
     * 
     * @param msg the message.
     * @return the soap fault object.
     */
    protected SoapFault createSoapFault(final String msg) {        
        final SoapFault soapFault = new SoapFault(msg, SoapFault.FAULT_CODE_SERVER);
        return soapFault;
    }
    
    
    /**
     * Returns the invoice data service.
     * 
     * @return the invoice data service.
     */
    protected InvoiceDataService getInvoiceDataService() {
        return invoiceDataService;
    }
    
    /**
     * Returns status bean.
     * 
     * @return the status bean.
     */
    protected StatusBean getStatusBean() {
        return statusBean;
    }

    
    /**
     * Returns the actual message context.
     * 
     * 
     * @return the message context.
     */
    protected MessageContext getMessageContext() {
        return webServiceContext.getMessageContext();
    }
    
    /**
     *
     * Logs message context information.
     * 
     * @param messageContext the context.
     */
    private void log(final Map<?, ?> headers) {
        log.info(createLogMessage(getHSAId()));
        log.debug("HTTP Headers {}", headers);
    }
    
    
    /**
     * Creates a log message.
     * 
     * @param msg the message.
     * @return the log message.
     */
    protected String createLogMessage(Object msg) {
        return String.format("%s - %s - \"%s\"", statusBean.getName(), statusBean.getGUID(), (msg == null) ? "NA" : msg);
    }
    
    /**
     * Runs a runnable in an instrumented manner.
     * 
     * @param runnable the runnable to run.
     * @return the result code.
     */
    protected ResultCode fulfill(final Runnable runnable) {
        final MessageContext messageContext = getMessageContext();
        final String path = (String)messageContext.get(MessageContext.PATH_INFO);
        statusBean.start(path);
        log(messageContext);
        final ResultCode rc = new ResultCode();
        try {
            runnable.run();
            rc.setCode(ResultCodeEnum.OK);
        } catch (InvoiceDataServiceException ex) {
            rc.setCode((ex.getCode() == InvoiceDataErrorCodeEnum.NOTFOUND_ERROR) ? ResultCodeEnum.NOTFOUND_ERROR : ResultCodeEnum.REQUEST_ERROR);
            rc.setMessage(ex.getMessage() + " (" + statusBean.getGUID() + ")");
            log.error(createLogMessage(ex.getMessage()));
        } catch (Throwable throwable) {
            throw createSoapFault(throwable);
        } finally {
            statusBean.stop();
        }
        
        return rc;
    }

	public void throwExceptionIfSystemHasNoAccessToOperation(final Operation operationEnum) {
		if (!isOpenToAllSystems()) {			
			String hsaID = getHSAId();
			if (!(operationAccessConfigService.hasSystemAccessToOperation(operationEnum, hsaID)
					&& securityAccessList.contains(hsaID))) {
				log.warn(hsaID + " has no access to operation " + operationEnum);
				throw InvoiceDataErrorCodeEnum.SERVICE_AUTHORIZATION_ERROR.createException(hsaID);
			}
		}
	}
	
	public void throwExceptionIfSupplierHasNoAccessToOperation(final Operation operationEnum, final String supplierId) {
		if (!isOpenToAllSuppliers() && !operationAccessConfigService.hasSupplierAccessToOperation(operationEnum, getHSAId(), supplierId)) {
			log.warn(supplierId + " has no access to operation " + operationEnum.name());
			throw InvoiceDataErrorCodeEnum.SUPPLIER_AUTHORIZATION_ERROR.createException(supplierId);
		}
	}
	
	private boolean isOpenToAllSystems() {
		return (securityAccessList != null && securityAccessList.equals("*"));
	}
	
	private boolean isOpenToAllSuppliers() {
		return (operationAccessList != null && operationAccessList.equals("*"));
	}
	
	private String getHSAId() {
		String hsaId = null;		
		try {
			User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			hsaId = user.getUsername();
		} catch (Exception e) {
			log.error("Could not fetch username from SecurityContextHolder " + e.getMessage());
		}
		
        log.info("HsaId from certificate: " + hsaId);
        return hsaId;
	}
    
}