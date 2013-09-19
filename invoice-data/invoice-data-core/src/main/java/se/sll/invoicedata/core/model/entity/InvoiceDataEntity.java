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

package se.sll.invoicedata.core.model.entity;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Persistent invoice data information.
 *
 * @author Peter
 */
@Entity
@Table(name="invoice_data")
public class InvoiceDataEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    
    @Column(name="supplier_id", length=64, nullable=false, updatable=false)
    private String supplierId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_timestamp", nullable=false, updatable=false)
    private Date createdTimestamp;
    
    @Column(name="created_by", length=64, nullable=false, updatable=false)
    private String createdBy;
 
    @OneToMany(fetch=FetchType.LAZY, mappedBy="invoiceData", orphanRemoval=false, cascade=CascadeType.ALL)    
    private List<BusinessEventEntity> businessEventEntities = new LinkedList<BusinessEventEntity>();

    
    @PrePersist
    void onPrePerist() {
        setCreatedTimestamp(new Date());
    }

    public Long getId() {
        return id;
    }

    /**
     * Returns invoice reference id.
     * 
     * @return the reference id.
     */
    public String getReferenceId() {
        if (getId() == null) {
            throw new IllegalStateException("A valid reference can only be retrieved after saving invoice data to database");
        }
        return String.format("%s.%06d", getSupplierId(), getId());
    }
    
    public String getSupplierId() {
        return supplierId;
    }
    
    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    protected void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public boolean removeBusinessEventEntity(BusinessEventEntity businessEventEntity) {
        if (this.equals(businessEventEntity.getInvoiceData())) {
            businessEventEntity.setInvoiceData(null);
            return businessEventEntities.remove(businessEventEntity);
        }
        return false;
    }
    
    public boolean addBusinessEventEntity(BusinessEventEntity businessEventEntity) {
        if (businessEventEntity.getInvoiceData() == null) {
            businessEventEntity.setInvoiceData(this);
            return businessEventEntities.add(businessEventEntity);
        }
        return false;
    }

    public List<BusinessEventEntity> getBusinessEventEntities() {
        return Collections.unmodifiableList(businessEventEntities);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    @Override
    public boolean equals(Object r) {
        if (this == r) {
            return true;
        }
        if (getId() != null && r instanceof InvoiceDataEntity) {
            return getId().equals(((InvoiceDataEntity)r).getId());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        final Long id = getId();
        return (id == null) ? super.hashCode() : id.hashCode();
    }
}