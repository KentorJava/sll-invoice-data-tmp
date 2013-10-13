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

package se.sll.invoicedata.core.service;

import java.util.List;

import se.sll.invoicedata.core.service.dto.PriceList;
import se.sll.invoicedata.core.service.dto.ServiceResponse;

public interface PriceListService {

    List<PriceList> getPriceLists();

    List<ServiceResponse>  savePriceLists(List<PriceList> priceLists);

    ServiceResponse deletePriceList(Long id);

    ServiceResponse savePriceList(PriceList priceList);

    PriceList getPriceList(Long id);
    
}