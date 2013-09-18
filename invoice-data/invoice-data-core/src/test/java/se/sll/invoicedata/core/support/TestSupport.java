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

package se.sll.invoicedata.core.support;

import java.util.Date;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import se.sll.invoicedata.core.model.entity.BusinessEventEntity;
import se.sll.invoicedata.core.model.entity.ItemEntity;
import se.sll.invoicedata.core.model.repository.BusinessEventRepository;

/**
 * Abstracts JUnit and Spring configuration stuff, and is intended to extend
 * all test classes.
 * 
 * @author Peter
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/invoice-data-service.xml")
@ActiveProfiles(profiles={"db-embedded","test"}, inheritProfiles=true)
public abstract class TestSupport {

    @Autowired
    private BusinessEventRepository businessEventRepository;

    protected BusinessEventRepository getBusinessEventRepository() {
        return businessEventRepository;
    }
    
    protected ItemEntity createSampleItemEntity() {
    	ItemEntity i = new ItemEntity();
    	i.setDescription("Item is kind of a product");
		i.setItemId("IT101");
		i.setQty(2.0f);
		
		return i;
	
    }
    
    protected BusinessEventEntity createSampleBusinessEventEntity() {
    	BusinessEventEntity e = new BusinessEventEntity();
        e.setId("event-123");
        e.setSignedBy("Peter Larsson");
        e.setSupplierName("Dummy");
        e.setSignedTimestamp(new Date());
        e.setServiceCode("XYZ");
        e.setSupplierId("12342");
        e.setStartTimestamp(new Date());
        e.setEndTimestamp(new Date());
        
        return e;
    }

}
